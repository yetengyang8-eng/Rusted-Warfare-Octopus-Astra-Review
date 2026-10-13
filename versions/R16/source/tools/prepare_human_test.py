"""Prepare an isolated, frozen human test kit. Does not launch any process/game."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import shutil

def sha(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()

def prepare(game, jar, target, python, seconds=4800, port=47661, g6=False):
    game, jar, target = map(lambda x: Path(x).resolve(), (game, jar, target))
    if target.exists():
        raise FileExistsError('Use a new kit directory; existing kits are never overwritten')
    if not 1 <= port <= 65535 or seconds < 1:
        raise ValueError('Invalid port/time budget')
    spec = importlib.util.spec_from_file_location('human_launcher', Path(__file__).with_name('rw-agent-human.py'))
    launcher = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(launcher)
    if sha(game / 'game-lib.jar') != launcher.GAME_SHA:
        raise ValueError('Unsupported engine fingerprint')
    if not (game / 'jvm64/bin/java.exe').is_file() or not jar.is_file() or not Path(python).is_file():
        raise FileNotFoundError('Candidate JAR, Java runtime or Python missing')
    target.mkdir(parents=True)
    copied = launcher.stage_game(game, jar, target / 'game')
    shutil.copytree(game / 'jvm64', target / 'jvm64')
    scripts = target / 'runtime/tools'
    scripts.mkdir(parents=True)
    for name in ('rw-agent-human.py', 'human_observer.py', 'expand_battle_log.py'):
        shutil.copy2(Path(__file__).with_name(name), scripts / name)
    # The first entry is the only one that starts a window, explicitly chosen by the human.
    game_script = '''$ErrorActionPreference='Stop'
$kit=Split-Path $PSScriptRoot -Parent
$port=PORT_VALUE
try { $health=Invoke-RestMethod -Uri "http://127.0.0.1:$port/health" -TimeoutSec 2 } catch { $health=$null }
if($health){throw 'Test port already has a bridge. Keep it open; do not start a second game.'}
$java=Join-Path $kit 'jvm64/bin/java.exe'
$game=Join-Path $kit 'game'
Set-Location -LiteralPath $game
& $java --add-modules jdk.httpserver -Xmx1000M '-Dfile.encoding=UTF-8' "-Drwagent.port=$port" '-Drwagent.allowCommands=true' "-javaagent:$game/rw-agent-bootstrap.jar" '-Djava.library.path=.' -cp 'rw-agent-bootstrap.jar;game-lib.jar;libs/*' com.corrodinggames.rts.java.Main -width 1280 -height 720 -log rw-agent-game.log
exit $LASTEXITCODE
'''.replace('PORT_VALUE', str(port))
    (target / 'runtime/start-game.ps1').write_text(game_script, encoding='utf-8-sig')
    start = '@echo off\r\nsetlocal\r\npowershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0runtime\\start-game.ps1"\r\nif errorlevel 1 pause\r\n'
    (target / '1-Start-Game.bat').write_bytes(start.encode('ascii'))
    for duration in sorted({1200, 2400, seconds}):
        endgame_option = ' --property rwagent.g6=true' if g6 else ''
        run = fr'''@echo off
setlocal
"{Path(python).resolve()}" "%~dp0runtime\tools\rw-agent-human.py" --game-dir "%~dp0game" --jar "%~dp0game\rw-agent-bootstrap.jar" --java "%~dp0jvm64\bin\java.exe" --port {port} --seconds {duration} --output-root "%~dp0runs" --require-same-bridge --html --property rwagent.reportMode=compact --property rwagent.reportGzip=true{endgame_option}
if errorlevel 1 pause
'''
        (target / f'2-Run-Agent-{duration}.bat').write_bytes(run.replace('\n','\r\n').encode('utf-8'))
    files = {}
    for path in sorted(target.rglob('*')):
        if path.is_file():
            files[path.relative_to(target).as_posix()] = {'bytes': path.stat().st_size, 'sha256': sha(path)}
    manifest = dict(schemaVersion=1, preparationOnly=True, gameLaunched=False, referenceGameReadOnly=str(game),
                    candidateSource=str(jar), candidateJar=str(target / 'game/rw-agent-bootstrap.jar'),
                    candidateSha256=sha(jar), gameLibSha256=launcher.GAME_SHA, copiedGameInputs=copied,
                    kit=str(target), port=port, defaultGameSeconds=seconds, reportMode='compact', reportGzip=True, g6Enabled=bool(g6),
                    nativeClockDisplay={'defaultEnabled': True, 'source': '/state gameTimeMs/frame', 'html': 'runs/<run-id>/rw-agent-live.html', 'wallTimeEstimate': False},
                    omittedInputs=['preferences.ini','parameter.ini','saves','replays','unit mods'], files=files)
    (target / 'KIT_MANIFEST.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n',encoding='utf-8')
    return manifest

if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--game-dir', required=True)
    p.add_argument('--jar', required=True)
    p.add_argument('--target', required=True)
    p.add_argument('--python', required=True)
    p.add_argument('--seconds', type=int, default=4800)
    p.add_argument('--port', type=int, default=47661)
    p.add_argument('--g6', action='store_true', help='Enable the candidate air/endgame controller in Agent entries')
    a=p.parse_args()
    m=prepare(a.game_dir,a.jar,a.target,a.python,a.seconds,a.port,a.g6)
    print(json.dumps({k:v for k,v in m.items() if k!='files'}, ensure_ascii=False,indent=2))
