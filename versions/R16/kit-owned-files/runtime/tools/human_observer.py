"""Small, incremental live BattleClient observer. Python stdlib only.

The journal is evidence. Missing fields stay UNKNOWN; orders never become kills.
"""
from __future__ import annotations
import argparse
import html
import json
import math
import os
from pathlib import Path
import sys
import time
import urllib.request
import zlib

UNKNOWN = "UNKNOWN"


def compact(value):
    if value is None:
        return UNKNOWN
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return str(value)


def panel_value(value):
    text = compact(value).replace("\r", " ").replace("\n", " ")
    return text if len(text) <= 260 else text[:257] + "..."


def general_fields(general):
    out = dict(general)
    combat = general.get("combat")
    if isinstance(combat, dict):
        for key, value in combat.items():
            out.setdefault(key, value)
    return out


class JournalTail:
    """Byte cursor survives .partial -> committed rename, never rereads a whole log.

    A bounded poll lets the UI breathe under backlog. No complete records are dropped.
    A short fingerprint at the cursor detects replacement even at the same size.
    """
    def __init__(self, path, max_bytes=1024 * 1024, max_line=4 * 1024 * 1024):
        self.path = Path(path)
        self.offset = 0
        self.pending = b""
        self.anchor = b""
        self.max_bytes = max_bytes
        self.max_line = max_line
        self.invalid = self.resets = self.oversized = 0
        self.backlog = 0
        self.discarding = False
        self.compressed = ".jsonl.gz" in str(self.path)
        self.decoder = zlib.decompressobj(16 + zlib.MAX_WBITS) if self.compressed else None
        self.compressed_pending = b""

    def poll(self, final=False):
        committed = self.path if not str(self.path).endswith(".partial") else Path(str(self.path)[:-8])
        partial = Path(str(committed) + ".partial")
        path = committed if committed.exists() else partial
        if not path.exists():
            return []
        try:
            with path.open("rb") as stream:
                size = path.stat().st_size
                replaced = size < self.offset
                if self.offset and not replaced:
                    stream.seek(max(0, self.offset - len(self.anchor)))
                    replaced = stream.read(len(self.anchor)) != self.anchor
                if replaced:
                    self.offset = 0
                    self.pending = self.anchor = b""
                    self.discarding = False
                    self.compressed_pending = b""
                    if self.compressed:
                        self.decoder = zlib.decompressobj(16 + zlib.MAX_WBITS)
                    self.resets += 1
                stream.seek(self.offset)
                chunk = b"" if self.compressed_pending else stream.read(self.max_bytes)
                self.offset += len(chunk)
                stream.seek(max(0, self.offset - 64))
                self.anchor = stream.read(min(64, self.offset))
                self.backlog = max(0, size - self.offset)
        except OSError:
            return []  # Writer commit/rotation can race an open on Windows.
        if self.compressed:
            packed = self.compressed_pending + chunk
            self.compressed_pending = b""
            try:
                if self.decoder.eof and packed:
                    self.decoder = zlib.decompressobj(16 + zlib.MAX_WBITS)
                chunk = self.decoder.decompress(packed, self.max_bytes)
                self.compressed_pending = self.decoder.unconsumed_tail
                if self.decoder.eof and self.decoder.unused_data:
                    # Concatenated gzip members remain bounded to the next poll.
                    self.compressed_pending = self.decoder.unused_data
                self.backlog += len(self.compressed_pending)
            except zlib.error:
                self.invalid += 1
                self.pending = b""
                return []
        pieces = (self.pending + chunk).split(b"\n")
        self.pending = pieces.pop()
        rows = []
        for line in pieces:
            if self.discarding:
                self.discarding = False
                continue
            if len(line) > self.max_line:
                self.oversized += 1
                continue
            self._parse(line, rows)
        if len(self.pending) > self.max_line:
            self.pending = b""
            self.discarding = True
            self.oversized += 1
        if final and not self.backlog and self.pending:
            self._parse(self.pending, rows)
            self.pending = b""
        return rows

    def _parse(self, line, rows):
        if not line.strip():
            return
        try:
            row = json.loads(line.decode("utf-8-sig"))
            if isinstance(row, dict):
                rows.append(row)
            else:
                self.invalid += 1
        except (ValueError, UnicodeError):
            self.invalid += 1


class HumanState:
    FIELDS = ("gameTime", "credits", "income", "factories", "queues", "FREE", "GeneralCount",
              "Recon", "LR", "Capability", "ThreatTask", "throughput", "decisionLatencyMs")
    ALIASES = {"gameTime": ("gameTime", "gameTimeMs", "sourceGameTimeMs"),
               "income": ("income", "incomeEstimate", "modelledIncomePerGameSecond"),
               "queues": ("queues", "factoryQueueNonEmpty"),
               "FREE": ("free", "freeCount", "FREE"),
               "GeneralCount": ("generalCount", "GeneralCount"),
               "Recon": ("recon", "reconTaskKind"), "LR": ("localResponse", "localCrisis", "LR"),
               "Capability": ("capability", "capabilityTasks"), "ThreatTask": ("threatTask", "threatTasks", "ThreatTask"),
               "throughput": ("throughput", "commandsPerGameSecond"),
               "decisionLatencyMs": ("decisionLatencyMs", "effectiveDecisionIntervalGameMs")}

    def __init__(self):
        self.values = {field: UNKNOWN for field in self.FIELDS}
        self.generals = {}
        self.summary = None
        self.terminal = None
        self.events = 0
        self.latest = "Waiting for BattleClient journal"
        self.sources = {}
        self.transitions = []
        self.provenance = {}
        self.runtime_decision_interval = False

    def consume(self, row):
        event = row.get("event", row.get("type", ""))
        data = row.get("data", row.get("payload", {}))
        if not isinstance(data, dict):
            return
        self.events += 1
        for field in self.FIELDS:
            for key in self.ALIASES.get(field, (field,)):
                if key in data:
                    if field == "decisionLatencyMs" and key == "effectiveDecisionIntervalGameMs":
                        previous = self.values[field]
                        clocks = dict(previous) if isinstance(previous, dict) else {}
                        summary = event in ("summary", "battle_summary")
                        metric = "recentDecisionIntervalGameMs" if summary and self.runtime_decision_interval else key
                        clocks[metric] = data[key] if data[key] is not None else UNKNOWN
                        self.values[field] = clocks
                        if summary and self.runtime_decision_interval:
                            self.sources[field] = "g5_runtime_state.runtime.effectiveDecisionIntervalGameMs=whole-run mean; " + event + ".effectiveDecisionIntervalGameMs=recent/attention statistic shown as recentDecisionIntervalGameMs; wall clocks retain runtime source"
                        else:
                            self.sources[field] = event + "." + key + ("=legacy recent/attention statistic" if summary else "") + "; other wall clock metrics retain runtime source"
                    else:
                        self.values[field] = data[key] if data[key] is not None else UNKNOWN
                        self.sources[field] = event + "." + key
                    break
        if "gameTimeMs" in row:
            self.values["gameTime"] = row["gameTimeMs"]
            self.sources["gameTime"] = "journal.gameTimeMs"
        if event == "heartbeat" and "gameSeconds" in data and "gameTimeMs" not in row:
            self.values["gameTime"] = data["gameSeconds"] * 1000
            self.sources["gameTime"] = "heartbeat.gameSeconds*1000 (battle elapsed)"
        if event == "g5_runtime_state":
            if isinstance(data.get("factories"), list):
                # The native packet includes every own unit carrying productionQueue.
                # A tank's -1 queue is not a factory. Keep raw rows intact.
                factories = [f for f in data["factories"] if isinstance(f, dict) and
                             isinstance(f.get("type"), str) and
                             (f["type"].endswith("Factory") or f["type"] == "commandCenter")]
                self.values["factories"] = len(factories)
                self.sources["factories"] = event + ".factories[type=*Factory|commandCenter].length"
                queues = [f.get("queue") if f.get("queue") is not None else UNKNOWN for f in factories]
                self.values["queues"] = queues
                self.sources["queues"] = event + ".factories[type=*Factory|commandCenter][].queue"
            runtime = data.get("runtime", {})
            if isinstance(runtime, dict):
                throughput = {k: runtime[k] for k in ("commandsPerGameMinute", "maxCommandsPerObservation", "cacheHits", "parallelFetches") if k in runtime}
                latency = {k: runtime[k] for k in ("effectiveDecisionIntervalGameMs", "observationLatencyWallMs", "dispatchLatencyWallMs") if k in runtime}
                if throughput:
                    self.values["throughput"] = throughput
                    self.sources["throughput"] = event + ".runtime (labelled units)"
                if latency:
                    self.values["decisionLatencyMs"] = latency
                    self.runtime_decision_interval = "effectiveDecisionIntervalGameMs" in latency
                    self.sources["decisionLatencyMs"] = event + ".runtime (effective interval=whole-run mean; game/wall clocks distinct)"
        if event == "report_provenance":
            self.provenance = {k: data[k] for k in ("agentJarSha256", "gameLibJarSha256", "workingDirectory", "javaVersion") if k in data}
        if event in ("g4_force_state", "g5_force_state", "g5_runtime_state"):
            if isinstance(data.get("generals"), list):
                previous = self.generals
                self.generals = {}
                for general in data["generals"]:
                    if isinstance(general, dict) and general.get("generalId") is not None:
                        gid = str(general["generalId"])
                        self.generals[gid] = dict(previous.get(gid, {}), **general_fields(general))
                self.values["GeneralCount"] = len(self.generals)
                self.sources["GeneralCount"] = event + ".generals.length"
            if isinstance(data.get("units"), list) and "free" not in data:
                self.values["FREE"] = sum(u.get("allocation") == "FREE" for u in data["units"] if isinstance(u, dict))
                self.sources["FREE"] = event + ".units.allocation=FREE"
        if event in ("g5_general_transition", "g4_force_transition"):
            after = data.get("after")
            # Unit transitions may also have generalId; never overwrite a General with a unit view.
            is_general = isinstance(after, dict) and (event == "g5_general_transition" or "members" in after or "phase" in after)
            general = general_fields(after) if is_general else data
            gid = general.get("generalId", data.get("generalId"))
            if gid is not None and (event == "g5_general_transition" or "phase" in general or "members" in general):
                key = str(gid)
                self.generals[key] = dict(self.generals.get(key, {}), **general)
                self.generals[key]["latestMeaningful"] = data.get("reason", data.get("kind", event))
            self._meaningful(event, data)
        elif event == "g5_commander_transition":
            self._meaningful(event, data)
        elif event == "g3_execution":
            self.latest = event + ": " + compact(data.get("reason", data.get("status")))
        elif event in ("summary", "battle_summary"):
            if data.get("phase", "battle") == "battle":
                self.summary = data
                self.latest = "summary: " + compact(data.get("outcome")) + " / " + compact(data.get("matchOutcome"))
        elif event == "match_terminal":
            self.terminal = data
            self.latest = "native terminal: " + compact(data.get("outcome"))
        if event.startswith(("recon_", "local_crisis_", "capability_", "threat_task_")):
            field = "Recon" if event.startswith("recon_") else "LR" if event.startswith("local_crisis_") else "Capability" if event.startswith("capability_") else "ThreatTask"
            self.values[field] = event + ": " + compact(data.get("reason", data.get("status")))
            self.sources[field] = event

    def _meaningful(self, event, data):
        self.latest = event + ": " + compact(data.get("reason", data.get("kind", data.get("phase"))))
        self.transitions.append(self.latest)
        del self.transitions[:-8]

    def render(self):
        lines = [" | ".join(field + "=" + panel_value(self.values[field]) for field in self.FIELDS[:7]),
                 " | ".join(field + "=" + panel_value(self.values[field]) for field in self.FIELDS[7:])]
        for gid, general in sorted(self.generals.items()):
            def get(*keys):
                for key in keys:
                    if key in general:
                        return general[key]
                return UNKNOWN
            members = get("members")
            if isinstance(members, list):
                members = len(members)
            goal = get("goal")
            if goal == UNKNOWN and general.get("goalKnown") is True:
                goal = {"x": general.get("goalX"), "y": general.get("goalY")}
            lines.append("General " + gid + " | " + " | ".join(k + "=" + panel_value(v) for k, v in
                         (("phase", get("phase")), ("members", members), ("healthy", get("healthy", "healthyAttachedStrength", "currentHealthy")),
                          ("goal", goal), ("crisis", get("crisis")), ("threat", get("threat", "currentThreat")),
                          ("tactic", get("tacticalChoice")), ("admission", get("tacticalReason")), ("need", get("capabilityNeed")),
                          ("reinforcement", get("reinforcement")), ("cmd", get("currentCommand", "currentcmd")),
                          ("latest", get("latestMeaningful", "lastMeaningfulEvent")))))
        lines.append("Latest: " + panel_value(self.latest))
        return "\n".join(lines)

    def report(self, reason="Observer stopped", diagnostics=None):
        outcome = self.summary.get("outcome", UNKNOWN) if self.summary else "PARTIAL"
        match = self.summary.get("matchOutcome", UNKNOWN) if self.summary else (self.terminal or {}).get("outcome", "ONGOING/UNKNOWN")
        lines = ["RW Agent human validation report", "Outcome=" + compact(outcome) + " | Native match=" + compact(match),
                 "Reason=" + compact(self.summary.get("reason", reason) if self.summary else reason), self.render(),
                 "Evidence: values are journal observations/derived counts; UNKNOWN is missing evidence.",
                 "No kill, win-rate or causal improvement is inferred from commands, damage or parser success.",
                 "Events consumed=" + str(self.events), "Sources=" + compact(self.sources)]
        if diagnostics:
            lines.append("Tail diagnostics=" + compact(diagnostics))
        if self.provenance:
            lines.append("Journal bridge provenance=" + compact(self.provenance))
        if self.summary:
            allowed = ("commands", "observations", "ownLosses", "newCombatUnits", "attackOrders", "attackOrdersConfirmed",
                       "battleGameTimeMs", "effectiveDecisionIntervalGameMs", "upgradesCompleted", "completedMines")
            lines.append("Native/controller counters=" + compact({k: self.summary[k] for k in allowed if k in self.summary}))
        return "\n".join(lines) + "\n"


class LiveObserver:
    def __init__(self, directory, html_output=False, output_directory=None):
        self.directory = Path(directory)
        self.output_directory = Path(output_directory) if output_directory is not None else self.directory
        self.output_directory.mkdir(parents=True, exist_ok=True)
        self.state = HumanState()
        self.tails = {}
        self.html_output = html_output
        self.last_display = ""
        self.last_print_at = 0
        self.last_meaningful = ""
        self.interactive = sys.stdout.isatty()
        self.bridge_clock = None
        self.clock_status = "UNKNOWN"
        self.last_clock_poll = -1
        self.latest_snapshot = None
        self.snapshot_signature = None
        if self.interactive and os.name == "nt":
            try:
                import ctypes
                handle = ctypes.windll.kernel32.GetStdHandle(-11)
                mode = ctypes.c_ulong()
                self.interactive = bool(ctypes.windll.kernel32.GetConsoleMode(handle, ctypes.byref(mode)) and
                                        ctypes.windll.kernel32.SetConsoleMode(handle, mode.value | 4))
            except (AttributeError, OSError):
                self.interactive = False

    def poll(self, final=False):
        self.poll_bridge_clock()
        if self.poll_latest_snapshot():
            return self.state.render()
        report_dir = self.directory / "rw-agent-reports"
        for path in sorted(report_dir.glob("battle-*.jsonl*")):
            if path.is_file() and path.name.endswith((".jsonl", ".jsonl.partial", ".jsonl.gz", ".jsonl.gz.partial")):
                logical = str(path)[:-8] if path.name.endswith(".partial") else str(path)
                self.tails.setdefault(logical, JournalTail(logical))
        for tail in self.tails.values():
            for row in tail.poll(final):
                self.state.consume(row)
        return self.state.render()

    def poll_latest_snapshot(self):
        """Current bounded display feed bypasses gzip backlog; never a source for decisions."""
        path = self.directory / "rw-agent-reports" / "battle-latest.json"
        try:
            stat = path.stat()
            signature = (stat.st_mtime_ns, stat.st_size)
            if signature != self.snapshot_signature:
                packet = json.loads(path.read_text(encoding="utf-8"))
                if packet.get("schemaVersion") != 1 or not isinstance(packet.get("rows"), list):
                    return False
                # A new controller journal replaces all stale per-General display state.
                state = HumanState()
                for row in packet["rows"]:
                    if isinstance(row, dict):
                        state.consume(row)
                self.state = state
                self.latest_snapshot = packet
                self.snapshot_signature = signature
            return self.latest_snapshot is not None
        except (OSError, ValueError, TypeError):
            return self.latest_snapshot is not None

    def poll_bridge_clock(self):
        """Display-only current native /state. Wall time limits reads, never estimates game time."""
        now = time.monotonic()
        if self.last_clock_poll >= 0 and now - self.last_clock_poll < 1:
            return
        self.last_clock_poll = now
        config = self.directory / "human-launch.json"
        if not config.is_file():
            return
        try:
            launch = json.loads(config.read_text(encoding="utf-8"))
            port = launch.get("cli", {}).get("port")
            if not isinstance(port, int) or isinstance(port, bool) or not 1 <= port <= 65535:
                return
            with urllib.request.urlopen("http://127.0.0.1:%d/state" % port, timeout=0.3) as response:
                sample = json.load(response)
            game_time, frame = sample.get("gameTimeMs"), sample.get("frame")
            if (sample.get("sessionId") != launch.get("sessionId") or sample.get("status") != "running"
                    or not isinstance(game_time, (int, float)) or isinstance(game_time, bool)
                    or not isinstance(frame, (int, float)) or isinstance(frame, bool)
                    or not math.isfinite(game_time) or not math.isfinite(frame)
                    or game_time < 0 or frame < 0):
                self.clock_status = "UNKNOWN (bridge session/status changed)"
                return
            self.bridge_clock = {"gameTimeMs": game_time, "frame": frame, "sessionId": sample["sessionId"], "source": "/state"}
            self.clock_status = "CURRENT_BRIDGE"
        except (OSError, ValueError, TypeError, KeyError):
            self.clock_status = "UNKNOWN (bridge unavailable)"

    def clock_header(self):
        if self.clock_status != "CURRENT_BRIDGE" or self.bridge_clock is None:
            return "Game UNKNOWN | Frame UNKNOWN | " + self.clock_status
        millis = int(self.bridge_clock["gameTimeMs"])
        minutes, seconds = divmod(millis // 1000, 60)
        return "Game %02d:%02d.%d | Frame %d" % (minutes, seconds, millis % 1000 // 100, self.bridge_clock["frame"])

    def display(self, force=False):
        current = self.state.render()
        clock = self.clock_header()
        if self.latest_snapshot is not None:
            runtime = next((r.get("data", {}) for r in self.latest_snapshot["rows"] if r.get("event") == "g5_runtime_state"), {})
            clock += " | Agent sample Game %s ms / Frame %s" % (compact(runtime.get("gameTimeMs")), compact(runtime.get("sourceFrame")))
        if self.interactive and os.name == "nt":
            try:
                import ctypes
                ctypes.windll.kernel32.SetConsoleTitleW("RW Agent | " + clock)
            except (AttributeError, OSError):
                pass
        # Game time alone should not produce an entire block every poll.
        signature = current.split(" | ", 1)[-1]
        changed = signature != self.last_display
        meaningful = self.state.latest != self.last_meaningful and self.state.latest.startswith(("g5_general_transition", "g5_commander_transition", "g4_force_transition", "summary", "native terminal"))
        now = time.monotonic()
        if force or (changed and (self.interactive or meaningful or now - self.last_print_at >= 10)):
            prefix = "\x1b[H\x1b[2J" if self.interactive else ""
            print(prefix + clock + "\n" + current, flush=True)
            self.last_display = signature
            self.last_print_at = now
            self.last_meaningful = self.state.latest
        if self.html_output:
            page = "<!doctype html><meta charset='utf-8'><meta http-equiv='refresh' content='2'><title>RW Agent</title><style>body{background:#111;color:#eee;font:16px monospace;padding:24px;white-space:pre-wrap}#native-clock{position:sticky;top:0;background:#111;padding:12px 0;font-size:22px;font-weight:bold}</style><div id='native-clock'>" + html.escape(clock) + "</div>" + html.escape(current)
            (self.output_directory / "rw-agent-live.html").write_text(page, encoding="utf-8")

    def finish(self, reason="Observer stopped"):
        # Drain bounded polls, then try any unterminated last record exactly once.
        while True:
            self.poll()
            if self.latest_snapshot is not None:
                break
            if not any(t.backlog for t in self.tails.values()):
                break
        self.poll(final=True)
        self.display(force=True)
        diagnostics = {Path(k).name: {"invalidLines": t.invalid, "truncationResets": t.resets,
                       "oversizedLines": t.oversized, "bytesRead": t.offset} for k, t in self.tails.items()}
        report = self.output_directory / "rw-agent-human-report.txt"
        content = self.state.report(reason, diagnostics)
        if self.latest_snapshot is not None:
            content += "Display evidence=latest snapshot only; Events consumed is not whole-run event count. Full evidence remains in " + str(self.latest_snapshot.get("journal", UNKNOWN)) + "\n"
        config_path = self.directory / "human-launch.json"
        if config_path.is_file():
            try:
                config = json.loads(config_path.read_text(encoding="utf-8"))
                content += "Binary provenance=" + compact({k: config.get(k, UNKNOWN) for k in
                           ("candidateSha256", "bridgeJarSha256", "gameLibSha256", "binaryCompatibility", "sessionId")}) + "\n"
            except (OSError, ValueError):
                content += "Binary provenance=UNKNOWN (launch configuration unreadable)\n"
        report.write_text(content, encoding="utf-8")
        return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path, help="isolated launcher output directory")
    parser.add_argument("--seconds", type=float, default=0, help="wall limit; 0 follows until summary or Ctrl+C")
    parser.add_argument("--html", action="store_true")
    parser.add_argument("--output-dir", type=Path, help="write observer artifacts separately from the read-only journal directory")
    args = parser.parse_args(argv)
    args.directory.mkdir(parents=True, exist_ok=True)
    observer = LiveObserver(args.directory, args.html, args.output_dir)
    end = time.monotonic() + args.seconds if args.seconds else float("inf")
    reason = "Observer wall budget"
    try:
        while time.monotonic() < end:
            observer.poll()
            observer.display()
            if observer.state.summary:
                reason = "Controller summary observed"
                break
            time.sleep(2)
    except KeyboardInterrupt:
        reason = "Observer interrupted; game untouched"
    print("Report: " + str(observer.finish(reason)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
