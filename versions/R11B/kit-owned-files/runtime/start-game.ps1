$ErrorActionPreference='Stop'
$kit=Split-Path $PSScriptRoot -Parent
$port=47697
try { $health=Invoke-RestMethod -Uri "http://127.0.0.1:$port/health" -TimeoutSec 2 } catch { $health=$null }
if($health){throw 'Test port already has a bridge. Keep it open; do not start a second game.'}
$java=Join-Path $kit 'jvm64/bin/java.exe'
$game=Join-Path $kit 'game'
Set-Location -LiteralPath $game
& $java --add-modules jdk.httpserver -Xmx1000M '-Dfile.encoding=UTF-8' "-Drwagent.port=$port" '-Drwagent.allowCommands=true' "-javaagent:$game/rw-agent-bootstrap.jar" '-Djava.library.path=.' -cp 'rw-agent-bootstrap.jar;game-lib.jar;libs/*' com.corrodinggames.rts.java.Main -width 1280 -height 720 -log rw-agent-game.log
exit $LASTEXITCODE
