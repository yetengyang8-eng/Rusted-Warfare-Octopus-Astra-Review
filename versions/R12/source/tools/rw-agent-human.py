"""Wait for a legal local match, run BattleClient, observe it in this console.

Default attaches only. --start-game creates a fresh isolated game directory and
starts it explicitly; never installs a JAR into the supplied game directory.
"""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import urllib.request
from human_observer import LiveObserver

GAME_SHA = "8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9"


def sha(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def get(port, path):
    with urllib.request.urlopen("http://127.0.0.1:%d%s" % (port, path), timeout=2) as response:
        return json.load(response)


def match_guard(health, state, jar_sha, require_same_bridge=False):
    """Fail closed for unknown guard fields. Returns readiness + human reason."""
    if health.get("version") != "0.07-alpha1":
        return False, "Waiting for compatible bridge 0.07-alpha1"
    if health.get("status") != "ok" or health.get("strategyContractVersion") != 1:
        return False, "Unknown bridge protocol/strategy contract"
    if health.get("allowCommands") is not True:
        return False, "Bridge local commands are disabled/UNKNOWN"
    provenance = health.get("provenance", health)
    if not isinstance(provenance, dict) or not provenance.get("agentJarSha256"):
        return False, "Bridge binary identity UNKNOWN"
    if require_same_bridge and provenance.get("agentJarSha256") != jar_sha:
        return False, "Bridge/client JAR differ under --require-same-bridge"
    if provenance.get("gameLibJarSha256") != GAME_SHA:
        return False, "Engine fingerprint differs/UNKNOWN"
    if state.get("status") != "running" or not state.get("sessionId"):
        return False, "Waiting for an active match/session"
    if state.get("networked") is not False or state.get("replay") is not False:
        return False, "Local non-network non-replay guard is not satisfied"
    if not isinstance(state.get("player"), dict):
        return False, "Waiting for own player"
    if state.get("match", {}).get("outcome") != "ONGOING":
        return False, "Waiting for native ONGOING match"
    units = state.get("ownUnits", [])
    if not any(isinstance(u, dict) and u.get("type") == "commandCenter" and
               u.get("dead") is False and isinstance(u.get("hp"), (int, float)) and u["hp"] > 0 for u in units):
        return False, "Waiting for own live commandCenter required by BattleClient"
    return True, "Legal local ONGOING match"


def java_path(game, requested=None):
    if requested:
        return str(Path(requested).resolve()) if Path(requested).is_file() else requested
    for path in (game / "jvm64/bin/java.exe", game / "jvm/bin/java.exe"):
        if path.is_file():
            return str(path.resolve())
    result = shutil.which("java")
    if not result:
        raise FileNotFoundError("Java missing; supply --java")
    return result


def client_command(java, jar, port, seconds, wall_seconds, properties=()):
    command = [java, "-Dfile.encoding=UTF-8", "-Drwagent.port=" + str(port),
               "-Drwagent.g5=true", "-Drwagent.runtimeAdaptive=true", "-Drwagent.pollMs=500",
               "-Drwagent.battleSafetyGameSeconds=" + str(max(7200, seconds)),
               "-Drwagent.battleSafetyWallSeconds=" + str(wall_seconds)]
    for item in properties:
        key, sep, value = item.partition("=")
        if not sep or not key.startswith("rwagent.") or key in ("rwagent.port", "rwagent.allowCommands"):
            raise ValueError("--property requires rwagent.name=value; port/allowCommands are guarded")
        command.append("-D" + key + "=" + value)
    return command + ["-cp", str(Path(jar).resolve()), "io.rwagent.client.BattleClient", str(seconds)]


def stage_game(game, jar, target):
    """Copy compatible engine inputs and map-only custom content, never user config/save/replay/unit-mod data."""
    target.mkdir(parents=True, exist_ok=False)
    copied = []
    for name in ("game-lib.jar", "libs", "assets", "res", "font"):
        source = game / name
        if not source.exists():
            raise FileNotFoundError(source)
        if source.is_dir():
            shutil.copytree(source, target / name)
        else:
            shutil.copy2(source, target / name)
        copied.append(name)
    maps_source = game / "mods" / "maps"
    if maps_source.exists():
        shutil.copytree(maps_source, target / "mods" / "maps")
        copied.append("mods/maps")
    for source in game.glob("*.dll"):
        shutil.copy2(source, target / source.name)
        copied.append(source.name)
    shutil.copy2(jar, target / "rw-agent-bootstrap.jar")
    return copied


def game_command(java, game, port):
    return [java, "--add-modules", "jdk.httpserver", "-Xmx1000M", "-Dfile.encoding=UTF-8",
            "-Drwagent.port=" + str(port), "-Drwagent.allowCommands=true",
            "-javaagent:" + str((game / "rw-agent-bootstrap.jar").resolve()), "-Djava.library.path=.",
            "-cp", os.pathsep.join(("rw-agent-bootstrap.jar", "game-lib.jar", "libs/*")), "com.corrodinggames.rts.java.Main",
            "-width", "1280", "-height", "720", "-log", "rw-agent-game.log"]


class PortLease:
    """Cross-output-directory local launcher exclusion; released on process exit."""
    def __init__(self, port):
        import tempfile
        self.path = Path(tempfile.gettempdir()) / ("rw-agent-human-%d.lock" % port)
        self.file = None

    def __enter__(self):
        self.file = self.path.open("a+b")
        try:
            self.file.seek(0)
            if self.file.read(1) == b"":
                self.file.write(b"0")
                self.file.flush()
            self.file.seek(0)
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(self.file.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(self.file, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError as error:
            self.file.close()
            raise RuntimeError("Another human launcher owns port; do not run competing controllers") from error
        return self

    def __exit__(self, *args):
        if self.file:
            self.file.close()


def parser():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--game-dir", type=Path, required=True, help="external compatible game; read-only")
    p.add_argument("--jar", type=Path, required=True, help="candidate bootstrap JAR; read-only")
    p.add_argument("--seconds", type=int, default=1800, help="BattleClient GAME seconds")
    p.add_argument("--wall-seconds", type=int, default=14400, help="controller wall safety limit")
    p.add_argument("--port", type=int, default=47653)
    p.add_argument("--wait", type=float, nargs="?", const=0, default=0, help="wait wall seconds; 0 waits until legal match")
    p.add_argument("--no-wait", action="store_true", help="fail promptly if no legal match")
    p.add_argument("--start-game", action="store_true", help="explicitly start a new ISOLATED desktop game")
    p.add_argument("--require-same-bridge", action="store_true", help="optional exact bridge/client SHA gate; compatible distinct binaries are accepted by default")
    p.add_argument("--java", help="Java executable; default external jvm64, then jvm, then PATH")
    p.add_argument("--output-root", type=Path, default=Path(__file__).resolve().parents[1] / "_human_runs")
    p.add_argument("--html", action="store_true", help="write small auto-refresh offline HTML")
    p.add_argument("--property", action="append", default=[], metavar="rwagent.name=value")
    return p


def main(argv=None):
    args = parser().parse_args(argv)
    if not 1 <= args.port <= 65535 or args.seconds < 1 or args.wall_seconds < 1 or args.wait < 0:
        parser().error("port/time budgets must be positive (wait may be zero)")
    game, jar = args.game_dir.resolve(), args.jar.resolve()
    if not jar.is_file() or not (game / "game-lib.jar").is_file():
        parser().error("candidate JAR or external game-lib.jar missing")
    if sha(game / "game-lib.jar") != GAME_SHA:
        parser().error("Unsupported external game-lib.jar fingerprint")
    java = java_path(game, args.java)
    jar_sha = sha(jar)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + str(os.getpid())
    work = args.output_root.resolve() / stamp
    work.mkdir(parents=True, exist_ok=False)
    observer = LiveObserver(work, args.html)
    config = {"schemaVersion": 1, "createdUtc": datetime.now(timezone.utc).isoformat(),
              "gameDirectoryReadOnly": str(game), "candidateJar": str(jar), "candidateSha256": jar_sha,
              "gameLibSha256": GAME_SHA, "workDirectory": str(work), "cli": vars(args).copy(),
              "mode": "isolated-explicit-game-start" if args.start_game else "attach-existing-game",
              "clientClass": "io.rwagent.client.BattleClient"}
    config["cli"] = {k: str(v) if isinstance(v, Path) else v for k, v in config["cli"].items()}
    def save_config():
        (work / "human-launch.json").write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    save_config()
    client = None
    reason = "Launcher stopped before BattleClient"
    code = 1
    try:
        with PortLease(args.port):
            if args.start_game:
                # Refuse a bridge already using this port before launching another game.
                try:
                    get(args.port, "/health")
                except OSError:
                    pass
                else:
                    raise RuntimeError("Port already has a bridge; use attach or a separate port")
                isolated = work / "game"
                config["copiedInputs"] = stage_game(game, jar, isolated)
                config["gameCommand"] = game_command(java, isolated, args.port)
                with (work / "game-start.log").open("wb") as output:
                    launched = subprocess.Popen(config["gameCommand"], cwd=isolated, stdout=output, stderr=subprocess.STDOUT)
                config["gamePid"] = launched.pid
                save_config()
                print("Isolated game started. Create a local match in its window. Game remains open after observer ends.", flush=True)
            print("Waiting for legal local ONGOING match. Output: " + str(work), flush=True)
            deadline = time.monotonic() + 2 if args.no_wait else time.monotonic() + args.wait if args.wait else float("inf")
            previous = None
            last_reason = None
            while True:
                ready = False
                try:
                    health, state = get(args.port, "/health"), get(args.port, "/state")
                    ready, why = match_guard(health, state, jar_sha, args.require_same_bridge)
                    if ready:
                        frame = state.get("frame")
                        if previous and previous[0] == state["sessionId"] and isinstance(frame, (int, float)) and frame > previous[1]:
                            config["sessionId"] = state["sessionId"]
                            config["initialBridge"] = health
                            provenance = health.get("provenance", health)
                            config["bridgeJarSha256"] = provenance["agentJarSha256"]
                            config["binaryCompatibility"] = "SAME_BINARY" if provenance["agentJarSha256"] == jar_sha else "COMPATIBLE_DISTINCT_BINARIES"
                            config["initialFrame"] = frame
                            break
                        previous = (state["sessionId"], frame) if isinstance(frame, (int, float)) else None
                        why = "Waiting for advancing simulation frames (unpause the local match)"
                    else:
                        previous = None
                except (OSError, ValueError) as error:
                    why = "Bridge unavailable: " + str(error)
                    previous = None
                if why != last_reason:
                    print(why, flush=True)
                    last_reason = why
                if (args.no_wait and not ready) or time.monotonic() >= deadline:
                    raise RuntimeError("Wait ended: " + why)
                time.sleep(1)
            command = client_command(java, jar, args.port, args.seconds, args.wall_seconds, args.property)
            config["clientCommand"] = command
            save_config()
            print("Legal match confirmed (" + config["binaryCompatibility"] + "); BattleClient + live observer running. Ctrl+C stops only this controller.", flush=True)
            with (work / "battle-client.log").open("wb") as output:
                client = subprocess.Popen(command, cwd=work, stdout=output, stderr=subprocess.STDOUT)
                config["clientPid"] = client.pid
                save_config()
                while client.poll() is None:
                    observer.poll()
                    observer.display()
                    time.sleep(2)
                code = client.returncode
            reason = "BattleClient exit " + str(code)
    except KeyboardInterrupt:
        reason = "User interrupted controller/observer; game kept open"
        code = 130
    except (OSError, ValueError, RuntimeError) as error:
        reason = str(error)
        print(reason, file=sys.stderr)
    finally:
        if client is not None and client.poll() is None:
            client.terminate()
            try:
                client.wait(timeout=5)
            except subprocess.TimeoutExpired:
                client.kill()
                client.wait(timeout=5)
        config["launcherExitCode"] = code
        config["stopReason"] = reason
        save_config()
        report = observer.finish(reason)
        print("Short report: " + str(report) + "\nRaw journals/log/config retained: " + str(work), flush=True)
    return code


if __name__ == "__main__":
    raise SystemExit(main())
