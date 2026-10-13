#!/usr/bin/env python3
"""Portable version of the already-used R16 Spain natural-run harness.

This is an adaptation of the archived spain_natural.py, not a newly validated
engine. It REQUIRES separately provided, lawfully available game engine inputs.
All output is written to --output-root, never to frozen version snapshots.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import socket
import subprocess
import sys
import time
import urllib.request

REPO = Path(__file__).resolve().parents[1]
MAP_SHA256 = "342db6d8a8b8320b6a271b9e3c8a4c29c96a203e206c4be690c44bbef5882746"
ENGINE_SHA256 = "8a550a37e2d8a5430866090d4e7d5892f9010b47f52a5a09350fc66c620deec9"


def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def find_repo_map():
    folder = REPO / "evidence" / "human-1v9-original"
    matches = [p for p in folder.glob("*.tmx") if "西班牙" in p.name and "MP97" in p.name]
    if len(matches) != 1:
        raise FileNotFoundError("Expected exactly one [10p] MP97 Spain .tmx in " + str(folder))
    return matches[0]


def inputs(args):
    resources = args.resources_dir.resolve()
    manifest = REPO / "versions" / args.version / "VERSION.json"
    meta = json.loads(manifest.read_text(encoding="utf-8"))
    agent = REPO / "versions" / args.version / "kit-owned-files" / "game" / "rw-agent-bootstrap.jar"
    map_file = args.map_file.resolve() if args.map_file else find_repo_map()
    needed = [resources / "game-lib.jar", agent, map_file]
    needed += [resources / d for d in ("assets", "libs", "res")]
    missing = [str(x) for x in needed if not x.exists()]
    if missing:
        raise FileNotFoundError("Missing game/runtime inputs; GitHub does not include licensed engine files: " + "; ".join(missing))
    assert (resources / "assets" / "tilesets").is_dir(), "Missing assets/tilesets for MP97 map"
    got = sha256(agent)
    if got != meta["agent_jar_sha256"].lower():
        raise RuntimeError("Frozen " + args.version + " JAR digest mismatch")
    engine = sha256(resources / "game-lib.jar")
    if engine != ENGINE_SHA256:
        raise RuntimeError("Game engine fingerprint mismatch: " + engine)
    map_hash = sha256(map_file)
    if map_hash != MAP_SHA256:
        raise RuntimeError("Spain map fingerprint mismatch: " + map_hash)
    java = args.java
    if Path(java).exists(): java = str(Path(java).resolve())
    return resources, agent, map_file, java, meta


def http_json(port, path):
    with urllib.request.urlopen(f"http://127.0.0.1:{port}{path}", timeout=10) as response:
        return json.load(response)


def run(args):
    if args.game_seconds < 120 or args.game_seconds > 14400:
        raise ValueError("Expected 120 <= --game-seconds <= 14400")
    if args.speed != 5 or args.difficulty != 0 or args.player != 0:
        raise ValueError("Frozen R12 only supports native player 0; comparison requires speed=5, difficulty=0, player=0")
    resources, agent, map_file, java, meta = inputs(args)
    target_root = args.output_root.resolve()
    if args.check_only:
        print(json.dumps({"status": "INPUTS_OK", "version": args.version,
             "jar_sha256": sha256(agent), "map_sha256": sha256(map_file),
             "engine_sha256": ENGINE_SHA256,
             "repository": str(REPO), "resources": str(resources)}, indent=2))
        return
    label = args.label
    if not label or any(x in label for x in ("/", "\\", "..")):
        raise ValueError("Use a new simple directory label, e.g. r12-normal-p5-run01")
    folder = target_root / label
    work = folder / "work"
    work.mkdir(parents=True, exist_ok=False)
    for name in ("assets", "libs", "res"):
        shutil.copytree(resources / name, work / name)
    shutil.copy2(resources / "game-lib.jar", work / "game-lib.jar")
    shutil.copy2(agent, work / "rw-agent-bootstrap.jar")
    map_name = "maps/skirmish/[10p] Spain MP97.tmx"
    (work / "assets" / "maps" / "skirmish").mkdir(parents=True, exist_ok=True)
    shutil.copy2(map_file, work / "assets" / map_name)
    # The original, already successful runner added this alias for relative TMX tileset refs.
    shutil.copytree(work / "assets" / "tilesets", work / "assets" / "assets" / "tilesets")
    wall_limit = max(180, min(3500, args.game_seconds // 3 + 180))
    metadata = dict(version=args.version, sourceCommit=meta["source_commit"],
                    jarSha256=sha256(agent), gameLibSha256=ENGINE_SHA256,
                    mapSha256=MAP_SHA256, speed=5, difficulty=0, playerId=0,
                    durationGameSeconds=args.game_seconds,
                    wallSafetySeconds=wall_limit, nativePositionMutation=False,
                    fixture=False, desktopTouched=False)
    (folder / "input.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    logs = [(folder / name).open("wb") for name in
            ("engine.log", "engine.err.log", "client.log", "client.err.log")]
    engine = subprocess.Popen([
        java, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
        "-cp", os.pathsep.join(("rw-agent-bootstrap.jar", "game-lib.jar", "libs/*")),
        "io.rwagent.headless.HeadlessRunner",
        "--map", map_name, "--port", str(port), "--speed", "5",
        "--max-wall-seconds", str(wall_limit + 20), "--frames", "0",
        "--difficulty", "0"],
        cwd=work, stdout=logs[0], stderr=logs[1])
    client = None
    begun = time.monotonic()
    result = {}
    try:
        for _ in range(300):
            if engine.poll() is not None:
                raise RuntimeError("HeadlessRunner stopped before /state reported running")
            try:
                state = http_json(port, "/state")
                if state.get("status") == "running":
                    break
            except Exception:
                pass
            time.sleep(0.1)
        else:
            raise TimeoutError("Spain native bridge not ready")
        (folder / "initial-state.json").write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")
        if state.get("player", {}).get("teamId") != 0:
            raise RuntimeError("Wrong native player id")
        if not any(u.get("type") == "commandCenter" and not u.get("dead") for u in state.get("ownUnits", [])):
            raise RuntimeError("No live native own commandCenter")
        if args.version == "R16":
            boot = json.loads((work / "native-bootstrap.json").read_text(encoding="utf-8"))
            if boot.get("playerId") != 0:
                raise RuntimeError("R16 bootstrap player mismatch")
        client = subprocess.Popen([
            java, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8",
            "-Dsun.stderr.encoding=UTF-8", "-Drwagent.g6=true",
            "-Drwagent.reportMode=compact", "-Drwagent.reportGzip=true",
            "-Drwagent.battleSafetyGameSeconds=14400",
            f"-Drwagent.battleSafetyWallSeconds={wall_limit}",
            f"-Drwagent.port={port}", "-cp", "rw-agent-bootstrap.jar",
            "io.rwagent.client.BattleClient", str(args.game_seconds)],
            cwd=work, stdout=logs[2], stderr=logs[3])
        with (folder / "monitor.jsonl").open("w", encoding="utf-8") as mon:
            while client.poll() is None:
                if time.monotonic() - begun > wall_limit + 10:
                    raise TimeoutError("Natural match exceeded the wall-time budget")
                try:
                    state = http_json(port, "/state")
                    kinds = {}
                    for u in state.get("ownUnits", []):
                        if not u.get("dead") and u.get("hp", 0) > 0:
                            key = u.get("type")
                            kinds[key] = kinds.get(key, 0) + 1
                    observation = dict(
                        frame=state.get("frame"), gameTimeMs=state.get("gameTimeMs"),
                        player=state.get("player"), types=kinds,
                        groundUnits=[{key: u.get(key) for key in
                            ("id", "type", "x", "y", "hp", "maxHp", "orderType", "orderX",
                             "orderY", "orderTargetId")}
                            for u in state.get("ownUnits", []) if u.get("type") in
                            ("heavyTank", "heavyArtillery", "builder", "scout", "amphibiousJet")])
                    mon.write(json.dumps(observation, ensure_ascii=False) + "\n")
                    mon.flush()
                except Exception as exc:
                    print("MONITOR_UNKNOWN", type(exc).__name__, flush=True)
                time.sleep(1)
        result = dict(clientExit=client.returncode, engineExit=engine.poll(),
                      jarSha256=sha256(agent), noDesktopTouched=True)
        (folder / "final-state.json").write_text(
            json.dumps(http_json(port, "/state"), ensure_ascii=False, indent=2), encoding="utf-8")
    except Exception as exc:
        result.update(errorType=type(exc).__name__, error=str(exc))
        raise
    finally:
        (folder / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        if client and client.poll() is None:
            client.terminate()
            client.wait(timeout=10)
        (work / "stop.request").write_text("isolated Spain run stop")
        try:
            engine.wait(timeout=10)
        except subprocess.TimeoutExpired:
            engine.terminate()
            engine.wait(timeout=10)
        for stream in logs:
            stream.close()
    print("FINISHED", folder, result)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--version", required=True, choices=("R12", "R16"))
    p.add_argument("--resources-dir", required=True, type=Path,
                   help="lawfully available local unpacked game resources (NOT bundled in this repo)")
    p.add_argument("--java", default="java")
    p.add_argument("--map-file", type=Path, help="defaults to the repo original Spain MP97 map")
    p.add_argument("--game-seconds", type=int, default=3600)
    p.add_argument("--speed", type=int, default=5)
    p.add_argument("--difficulty", type=int, default=0)
    p.add_argument("--player", type=int, default=0)
    p.add_argument("--label", default="r12-or-r16-test01")
    p.add_argument("--output-root", type=Path, default=REPO / "cloud_review" / "runs")
    p.add_argument("--check-only", action="store_true", help="validate fingerprints without launching")
    run(p.parse_args())


if __name__ == "__main__":
    main()
