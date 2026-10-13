"""Focused real-file lifecycle and launcher guard tests; no desktop launch."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
import gzip
import zlib
from unittest import mock

TOOLS = Path(__file__).resolve().parents[2] / "tools"
sys.path.insert(0, str(TOOLS))
from human_observer import HumanState, JournalTail, LiveObserver, UNKNOWN
spec = importlib.util.spec_from_file_location("human_launcher", TOOLS / "rw-agent-human.py")
launcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(launcher)


def row(event, **data):
    return json.dumps({"event": event, "data": data}, ensure_ascii=False).encode() + b"\n"


class LatestFeedTests(unittest.TestCase):
    def test_latest_feed_bypasses_archive_backlog_and_replaces_generals(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); reports = root / "rw-agent-reports"; reports.mkdir()
            (reports / "battle-old.jsonl.gz.partial").write_bytes(gzip.compress(row("g5_runtime_state", credits=1) * 40000))
            live = reports / "battle-latest.json"
            live.write_text(json.dumps({"schemaVersion": 1, "journal": "battle-new.jsonl.gz", "rows": [
                {"event": "g5_runtime_state", "data": {"gameTimeMs": 600000, "sourceFrame": 12345, "credits": 999,
                    "generals": [{"generalId": 9, "phase": "ACTIVE"}]}}]}), encoding="utf-8")
            observer = LiveObserver(root); observer.poll()
            self.assertEqual(observer.state.values["credits"], 999)
            self.assertEqual(observer.tails, {})
            self.assertEqual(list(observer.state.generals), ["9"])
            # Partial replacement is tolerated without replaying twenty minutes of archive.
            live.write_text("{", encoding="utf-8"); observer.poll()
            self.assertEqual(observer.state.values["credits"], 999)
            live.write_text(json.dumps({"schemaVersion": 1, "journal": "battle-new.jsonl.gz", "rows": [
                {"event": "g5_runtime_state", "data": {"gameTimeMs": 610000, "sourceFrame": 12500, "generals": []}},
                {"event": "summary", "data": {"outcome": "PARTIAL", "matchOutcome": "ONGOING"}}]}), encoding="utf-8")
            observer.poll(); self.assertEqual(observer.state.generals, {})
            self.assertEqual(observer.state.summary["matchOutcome"], "ONGOING")
            observer.finish("user exited game")


class TailTests(unittest.TestCase):
    def test_live_observer_discovers_gzip_journal_and_commit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            reports = root / "rw-agent-reports"
            reports.mkdir()
            path = reports / "battle-real.jsonl.gz.partial"
            path.write_bytes(gzip.compress(row("g5_runtime_state", credits=55, freeCount=2) + row("summary", outcome="PARTIAL", matchOutcome="ONGOING")))
            observer = LiveObserver(root)
            observer.poll()
            self.assertEqual(observer.state.summary["outcome"], "PARTIAL")
            self.assertEqual(observer.state.values["credits"], 55)
            self.assertEqual(observer.state.events, 2)
            path.rename(reports / "battle-real.jsonl.gz")
            observer.poll(final=True)
            self.assertEqual(observer.state.events, 2)

    def test_gzip_live_flush_commit_and_bounded_backlog(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "battle-one.jsonl.gz"
            partial = Path(str(path) + ".partial")
            compressor = zlib.compressobj(wbits=16 + zlib.MAX_WBITS)
            payload = b"".join(row("heartbeat", credits=i, message="前进" * 20) for i in range(90))
            partial.write_bytes(compressor.compress(payload) + compressor.flush(zlib.Z_SYNC_FLUSH))
            tail = JournalTail(path, max_bytes=251)
            rows = []
            for _ in range(1000):
                rows.extend(tail.poll())
                if not tail.backlog:
                    break
            self.assertEqual([r["data"]["credits"] for r in rows], list(range(90)))
            self.assertEqual(tail.poll(), [])
            with partial.open("ab") as stream:
                stream.write(compressor.compress(row("summary", outcome="PARTIAL")) + compressor.flush())
            partial.rename(path)
            self.assertEqual([r["event"] for r in tail.poll(final=True)], ["summary"])
            self.assertEqual(tail.invalid, 0)
            self.assertEqual(tail.resets, 0)

    def test_gzip_interrupted_stream_keeps_complete_rows_and_replacement_recovers(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "battle-one.jsonl.gz.partial"
            compressor = zlib.compressobj(wbits=16 + zlib.MAX_WBITS)
            path.write_bytes(compressor.compress(row("heartbeat", credits=3)) + compressor.flush(zlib.Z_SYNC_FLUSH))
            tail = JournalTail(path)
            self.assertEqual(tail.poll(final=True)[0]["data"]["credits"], 3)
            self.assertEqual(tail.poll(final=True), [])
            path.write_bytes(gzip.compress(row("heartbeat", credits=4)))
            self.assertEqual(tail.poll()[0]["data"]["credits"], 4)
            self.assertEqual(tail.resets, 1)

    def test_compact_reference_expansion_never_invents_omitted_fields(self):
        from expand_battle_log import expand
        value = {"data": {"intent": {"owner": "general:1", "actors": [1, 2]}},
                 "trace": {"context": {"$rwReportRef": "#/data/intent"}},
                 "report": {"omittedFields": {"ownUnits": {"status": "OMITTED_CONTINUOUS_PAYLOAD"}}}}
        self.assertEqual(expand(value)["trace"]["context"], value["data"]["intent"])
        self.assertNotIn("ownUnits", expand(value)["data"])
        with self.assertRaises(ValueError):
            expand({"self": {"$rwReportRef": "#/self"}})

    def test_live_split_utf8_commit_and_final_no_duplicate(self):
        with tempfile.TemporaryDirectory() as directory:
            committed = Path(directory) / "battle-one.jsonl"
            partial = Path(str(committed) + ".partial")
            payload = row("g5_commander_transition", reason="退压后恢复")
            partial.write_bytes(payload[:-4])
            tail = JournalTail(committed)
            self.assertEqual(tail.poll(), [])
            with partial.open("ab") as stream:
                stream.write(payload[-4:])
            self.assertEqual(tail.poll()[0]["data"]["reason"], "退压后恢复")
            committed.write_bytes(partial.read_bytes() + row("summary", outcome="PARTIAL", matchOutcome="ONGOING"))
            partial.unlink()
            rows = tail.poll()
            self.assertEqual([r["event"] for r in rows], ["summary"])
            self.assertEqual(tail.poll(final=True), [])
            self.assertEqual(tail.resets, 0)

    def test_incremental_backlog_and_truncation_recovery(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "battle-one.jsonl"
            path.write_bytes(b"".join(row("heartbeat", credits=i) for i in range(100)))
            tail = JournalTail(path, max_bytes=79)
            rows = []
            while True:
                rows.extend(tail.poll())
                if not tail.backlog:
                    break
            self.assertEqual([r["data"]["credits"] for r in rows], list(range(100)))
            self.assertEqual(tail.poll(), [])
            path.write_bytes(row("heartbeat", credits=900))
            self.assertEqual(tail.poll()[0]["data"]["credits"], 900)
            self.assertEqual(tail.resets, 1)

    def test_partial_last_line_invalid_and_oversize_are_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "battle-one.jsonl.partial"
            path.write_bytes(row("heartbeat", credits=10) + b"broken\n" + b"x" * 140 + b"\n" + b'{"event":"summary","data":{"outcome":"PARTIAL"}}')
            tail = JournalTail(path, max_line=100)
            self.assertEqual([r["event"] for r in tail.poll()], ["heartbeat"])
            self.assertEqual(tail.poll(final=True)[0]["event"], "summary")
            self.assertEqual(tail.invalid, 1)
            self.assertEqual(tail.oversized, 1)

    def test_same_size_replacement_resets(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "battle-one.jsonl"
            path.write_bytes(row("heartbeat", credits=1))
            tail = JournalTail(path)
            tail.poll()
            path.write_bytes(row("heartbeat", credits=2))
            self.assertEqual(tail.poll()[0]["data"]["credits"], 2)
            self.assertEqual(tail.resets, 1)


class StateTests(unittest.TestCase):
    def test_g51_tactical_admission_is_visible_without_claiming_damage(self):
        state = HumanState()
        state.consume({"event": "g5_runtime_state", "data": {"generals": [
            {"generalId": 1, "phase": "ACTIVE", "members": [2], "combat": {
                "tacticalChoice": "STANDOFF", "tacticalReason": "VISIBLE_STATIC_DEFENDERS", "capabilityNeed": "ANTI_AIR"}}]}})
        text = state.render()
        self.assertIn("tactic=STANDOFF", text)
        self.assertIn("admission=VISIBLE_STATIC_DEFENDERS", text)
        self.assertIn("need=ANTI_AIR", text)
        self.assertNotIn("kills=", text)

    def test_multi_general_never_merge_and_unknown_stays_unknown(self):
        state = HumanState()
        state.consume({"event": "g5_runtime_state", "data": {"generals": [
            {"generalId": "a", "phase": "ACTIVE", "members": [1, 2], "healthyAttachedStrength": 2},
            {"generalId": "b", "phase": "FORMING", "members": [3]}], "freeCount": 4}})
        state.consume({"event": "g5_general_transition", "data": {"generalId": "a", "after": {"phase": "COOLDOWN"}, "reason": "RETREAT_PRESSURE"}})
        self.assertEqual(state.generals["a"]["phase"], "COOLDOWN")
        self.assertEqual(state.generals["b"]["phase"], "FORMING")
        self.assertIn("General b | phase=FORMING | members=1 | healthy=UNKNOWN", state.render())
        self.assertEqual(state.values["credits"], UNKNOWN)
        state.consume({"event": "g5_general_transition", "data": {"generalId": "a", "after": {"phase": "ACTIVE"}, "reason": "PRESSURE_RECOVERED"}})
        self.assertIn("PRESSURE_RECOVERED", state.render())
        self.assertEqual(state.values["FREE"], 4)

    def test_g4_fallback_unit_transitions_do_not_corrupt_general(self):
        state = HumanState()
        state.consume({"event": "g4_force_state", "data": {"sourceGameTimeMs": 1000, "generals": [{"generalId": 1, "phase": "ACTIVE", "members": [2]}],
             "units": [{"allocation": "FREE"}, {"allocation": "ASSIGNED"}]}})
        state.consume({"event": "g4_force_transition", "data": {"after": {"generalId": 1, "unitId": 3, "membership": "JOINING"}}})
        self.assertNotIn("unitId", state.generals["1"])
        self.assertEqual(state.values["FREE"], 1)
        state.consume({"event": "heartbeat", "data": {"gameSeconds": 9, "credits": 70, "factories": 2}})
        self.assertEqual(state.values["gameTime"], 9000)
        self.assertEqual(state.values["credits"], 70)

    def test_runtime_clocks_provenance_and_no_fake_kills(self):
        state = HumanState()
        state.consume({"event": "g5_runtime_state", "data": {"factories": [{"id": 1, "type": "landFactory", "queue": 2}], "threatTasks": [],
            "runtime": {"commandsPerGameMinute": 30, "effectiveDecisionIntervalGameMs": 1000, "dispatchLatencyWallMs": 5}}})
        self.assertEqual(state.values["factories"], 1)
        self.assertEqual(state.values["queues"], [2])
        report = state.report()
        self.assertIn("commandsPerGameMinute", report)
        self.assertIn("dispatchLatencyWallMs", report)
        self.assertIn("PARTIAL", report)
        self.assertNotIn("kills=", report)
        state.consume({"event": "summary", "data": {"outcome": "PARTIAL", "matchOutcome": "ONGOING", "effectiveDecisionIntervalGameMs": 900}})
        self.assertEqual(state.values["decisionLatencyMs"]["effectiveDecisionIntervalGameMs"], 1000)
        self.assertEqual(state.values["decisionLatencyMs"]["recentDecisionIntervalGameMs"], 900)
        self.assertEqual(state.values["decisionLatencyMs"]["dispatchLatencyWallMs"], 5)
        self.assertIn("whole-run mean", state.sources["decisionLatencyMs"])
        self.assertIn("recent/attention statistic", state.sources["decisionLatencyMs"])
        state.consume({"event": "report_provenance", "data": {"agentJarSha256": "bridge-identity", "gameLibJarSha256": "engine-identity"}})
        self.assertIn("bridge-identity", state.report())

    def test_legacy_summary_decision_interval_without_runtime_retains_legacy_basis(self):
        state = HumanState()
        state.consume({"event": "summary", "data": {"outcome": "PARTIAL", "matchOutcome": "ONGOING", "effectiveDecisionIntervalGameMs": 512}})
        self.assertEqual(state.values["decisionLatencyMs"]["effectiveDecisionIntervalGameMs"], 512)
        self.assertNotIn("recentDecisionIntervalGameMs", state.values["decisionLatencyMs"])
        self.assertIn("legacy recent/attention statistic", state.sources["decisionLatencyMs"])

    def test_native_all_unit_queue_rows_do_not_inflate_factory_panel(self):
        rows = [{"id": 1, "type": "landFactory", "queue": 2}, {"id": 2, "type": "commandCenter", "queue": 0},
                {"id": 3, "type": "tank", "queue": -1}, {"id": 4, "type": "c_tank", "queue": -1},
                {"id": 5, "type": "extractor", "queue": -1}, {"id": 6, "type": "airFactory", "queue": None}]
        state = HumanState()
        state.consume({"event": "g5_runtime_state", "data": {"factories": rows}})
        self.assertEqual(state.values["factories"], 3)
        self.assertEqual(state.values["queues"], [2, 0, UNKNOWN])
        self.assertEqual(len(rows), 6, 'raw input packet remains intact')
        self.assertEqual(rows[2]["queue"], -1)
        self.assertIn("type=*Factory|commandCenter", state.sources["factories"])

    def test_combat_transition_and_nested_metadata_show_retreat_and_recovery(self):
        state = HumanState()
        state.consume({"event": "g5_runtime_state", "data": {"generals": [{"generalId": "a", "phase": "ACTIVE", "members": [1],
             "combat": {"crisis": "NORMAL", "lastMeaningfulEvent": "CURRENT_VISIBLE_TASK_CHANGED"}}]}})
        self.assertIn("crisis=NORMAL", state.render())
        self.assertIn("latest=CURRENT_VISIBLE_TASK_CHANGED", state.render())
        state.consume({"event": "g5_general_transition", "data": {"generalId": "a", "reason": "GENERAL_COMBAT_OBSERVED",
              "after": {"generalId": "a", "crisis": "RETREATING", "retreating": True, "lastMeaningfulEvent": "CRISIS_RETREATING"}}})
        self.assertIn("crisis=RETREATING", state.render())
        state.consume({"event": "g5_general_transition", "data": {"generalId": "a", "reason": "GENERAL_COMBAT_OBSERVED",
              "after": {"generalId": "a", "crisis": "NORMAL", "retreating": False, "lastMeaningfulEvent": "CRISIS_NORMAL"}}})
        self.assertIn("crisis=NORMAL", state.render())
        state.consume({"event": "g5_runtime_state", "data": {"capability": [{"diagnostic": "x" * 10000}]}})
        self.assertLess(len(state.render()), 3000)

    def test_terminal_and_partial_summary_are_distinct(self):
        state = HumanState()
        state.consume({"event": "match_terminal", "data": {"outcome": "VICTORY", "source": "native_result_screen"}})
        self.assertIn("Outcome=PARTIAL | Native match=VICTORY", state.report())
        state.consume({"event": "summary", "data": {"outcome": "PASS", "matchOutcome": "VICTORY", "phase": "battle"}})
        self.assertIn("Outcome=PASS | Native match=VICTORY", state.report())
        state.consume({"event": "summary", "data": {"outcome": "PARTIAL", "matchOutcome": "ONGOING", "reason": "budget"}})
        self.assertIn("Outcome=PARTIAL | Native match=ONGOING", state.report())

    def test_live_observer_real_partial_final_short_report(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            reports = work / "rw-agent-reports"
            reports.mkdir()
            path = reports / "battle-one.jsonl.partial"
            path.write_bytes(row("heartbeat", credits=50))
            observer = LiveObserver(work, html_output=True)
            observer.poll()
            self.assertEqual(observer.state.events, 1)
            path.write_bytes(path.read_bytes() + row("summary", phase="battle", outcome="PARTIAL", matchOutcome="ONGOING"))
            with mock.patch("builtins.print"):
                report = observer.finish("test")
            self.assertEqual(observer.state.events, 2)
            self.assertIn("PARTIAL", report.read_text(encoding="utf-8"))
            self.assertTrue((work / "rw-agent-live.html").is_file())

    def test_read_only_source_directory_writes_artifacts_elsewhere(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "source"
            report_dir = source / "rw-agent-reports"
            report_dir.mkdir(parents=True)
            journal = report_dir / "battle-one.jsonl"
            raw = row("summary", outcome="PARTIAL", matchOutcome="ONGOING")
            journal.write_bytes(raw)
            target = Path(directory) / "observer output"
            observer = LiveObserver(source, html_output=True, output_directory=target)
            with mock.patch("builtins.print"):
                report = observer.finish("read-only validation")
            self.assertEqual(report.parent, target)
            self.assertEqual(journal.read_bytes(), raw)
            self.assertFalse((source / "rw-agent-human-report.txt").exists())
            self.assertFalse((source / "rw-agent-live.html").exists())


class LauncherTests(unittest.TestCase):
    def healthy(self):
        return ({"status": "ok", "strategyContractVersion": 1, "version": "0.07-alpha1", "allowCommands": True,
                 "provenance": {"agentJarSha256": "candidate", "gameLibJarSha256": launcher.GAME_SHA}},
                {"status": "running", "sessionId": "s", "networked": False, "replay": False, "player": {},
                 "match": {"outcome": "ONGOING"}, "ownUnits": [{"type": "commandCenter", "dead": False, "hp": 100}]})

    def test_guards_fail_closed_network_replay_terminal_unknown_identity(self):
        health, state = self.healthy()
        self.assertTrue(launcher.match_guard(health, state, "candidate")[0])
        for name, value in (("networked", True), ("replay", True), ("networked", None), ("replay", None)):
            changed = dict(state, **{name: value})
            self.assertFalse(launcher.match_guard(health, changed, "candidate")[0])
        self.assertFalse(launcher.match_guard(health, dict(state, match={"outcome": "VICTORY"}), "candidate")[0])
        self.assertTrue(launcher.match_guard(health, state, "other")[0])
        self.assertFalse(launcher.match_guard(health, state, "other", require_same_bridge=True)[0])
        self.assertFalse(launcher.match_guard(dict(health, strategyContractVersion=None), state, "candidate")[0])
        self.assertFalse(launcher.match_guard(dict(health, provenance={"gameLibJarSha256": launcher.GAME_SHA}), state, "candidate")[0])
        self.assertFalse(launcher.match_guard(dict(health, provenance={"agentJarSha256": "candidate", "gameLibJarSha256": "other"}), state, "candidate")[0])
        self.assertFalse(launcher.match_guard(dict(health, allowCommands=None), state, "candidate")[0])

    def test_command_paths_with_spaces_are_single_arguments(self):
        jar = Path("candidate space") / "a.jar"
        command = launcher.client_command("C:/Java Path/java.exe", jar, 47654, 1800, 9000, ["rwagent.g5=false"])
        self.assertEqual(command[0], "C:/Java Path/java.exe")
        self.assertEqual(command[command.index("-cp") + 1], str(jar.resolve()))
        self.assertIn("-Drwagent.g5=false", command)
        self.assertEqual(command[-2:], ["io.rwagent.client.BattleClient", "1800"])
        for prop in ("rwagent.port=9", "rwagent.allowCommands=true", "other=x", "rwagent.x"):
            with self.assertRaises(ValueError):
                launcher.client_command("java", jar, 1, 1, 1, [prop])
        parsed = launcher.parser().parse_args(["--game-dir", "external space", "--jar", str(jar)])
        self.assertFalse(parsed.start_game)
        self.assertEqual(parsed.wait, 0)

    def test_staging_preserves_source_and_omits_user_data(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            game = root / "external game"
            game.mkdir()
            for name in ("libs", "assets", "res", "font"):
                (game / name).mkdir()
                (game / name / "input").write_bytes(b"engine")
            (game / "game-lib.jar").write_bytes(b"engine")
            (game / "preferences.ini").write_bytes(b"protected")
            (game / "saves").mkdir()
            jar = root / "candidate jar.jar"
            jar.write_bytes(b"candidate")
            isolated = root / "isolated game"
            launcher.stage_game(game, jar, isolated)
            self.assertEqual((isolated / "rw-agent-bootstrap.jar").read_bytes(), b"candidate")
            self.assertFalse((game / "rw-agent-bootstrap.jar").exists())
            self.assertFalse((isolated / "preferences.ini").exists())
            self.assertFalse((isolated / "saves").exists())
            self.assertEqual((isolated / "font" / "input").read_bytes(), b"engine")
            self.assertEqual((game / "preferences.ini").read_bytes(), b"protected")
            cmd = launcher.game_command("java", isolated, 47654)
            self.assertIn("-javaagent:" + str((isolated / "rw-agent-bootstrap.jar").resolve()), cmd)

    def test_attach_launcher_runs_only_client_in_isolated_cwd_and_finishes_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            external = root / "external game space"
            external.mkdir()
            (external / "game-lib.jar").write_bytes(b"original")
            jar = root / "candidate space.jar"
            jar.write_bytes(b"candidate")
            health, state = self.healthy()
            frame = [0]
            commands = []
            def bridge(port, path):
                if path == "/health":
                    return health
                frame[0] += 1
                return dict(state, frame=frame[0])
            class FakeClient:
                def __init__(self, command, cwd, **kwargs):
                    commands.append((command, Path(cwd)))
                    self.pid = 1001
                    self.returncode = 2
                    self.polls = 0
                    reports = Path(cwd) / "rw-agent-reports"
                    reports.mkdir()
                    (reports / "battle-one.jsonl").write_bytes(row("g5_runtime_state", credits=70, freeCount=2) +
                         row("summary", outcome="PARTIAL", matchOutcome="ONGOING", phase="battle", reason="budget"))
                def poll(self):
                    self.polls += 1
                    return None if self.polls == 1 else self.returncode
            def fingerprint(path):
                return launcher.GAME_SHA if Path(path).name == "game-lib.jar" else "new-client"
            with mock.patch.object(launcher, "get", side_effect=bridge), mock.patch.object(launcher, "sha", side_effect=fingerprint), \
                 mock.patch.object(launcher.subprocess, "Popen", side_effect=FakeClient), mock.patch.object(launcher.time, "sleep"), \
                 mock.patch("tempfile.gettempdir", return_value=directory), \
                 mock.patch("builtins.print"):
                code = launcher.main(["--game-dir", str(external), "--jar", str(jar), "--java", sys.executable,
                                      "--output-root", str(root / "runs space"), "--no-wait"])
            self.assertEqual(code, 2)
            self.assertEqual(len(commands), 1)
            command, cwd = commands[0]
            self.assertIn("io.rwagent.client.BattleClient", command)
            self.assertNotEqual(cwd, external)
            config = json.loads((cwd / "human-launch.json").read_text(encoding="utf-8"))
            self.assertEqual(config["binaryCompatibility"], "COMPATIBLE_DISTINCT_BINARIES")
            report = (cwd / "rw-agent-human-report.txt").read_text(encoding="utf-8")
            self.assertIn("Outcome=PARTIAL | Native match=ONGOING", report)
            self.assertIn("new-client", report)
            self.assertIn("candidate", report)
            self.assertEqual((external / "game-lib.jar").read_bytes(), b"original")
            self.assertFalse((external / "rw-agent-reports").exists())

    def test_port_lease_excludes_competing_launchers(self):
        with tempfile.TemporaryDirectory() as directory, mock.patch("tempfile.gettempdir", return_value=directory):
            with launcher.PortLease(58191):
                with self.assertRaises(RuntimeError):
                    with launcher.PortLease(58191):
                        pass


if __name__ == "__main__":
    unittest.main()
