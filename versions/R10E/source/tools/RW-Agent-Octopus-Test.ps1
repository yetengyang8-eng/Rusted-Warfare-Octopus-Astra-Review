param([switch]$StartGame, [int]$Seconds=4800)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$manifest=Get-Content -LiteralPath (Join-Path $repo 'deliveries/octopus-g51-2026-10-03/candidate-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$python='C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if(-not (Test-Path -LiteralPath $python)){$python='python'}
$runArgs=@((Join-Path $PSScriptRoot 'rw-agent-human.py'),'--game-dir',$manifest.referenceGameDirectory,'--jar',$manifest.candidateJar,'--seconds',"$Seconds",'--html')
if($StartGame){$runArgs+='--start-game'}
& $python @runArgs
exit $LASTEXITCODE
