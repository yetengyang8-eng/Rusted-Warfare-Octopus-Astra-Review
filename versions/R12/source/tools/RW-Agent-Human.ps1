# Pass the Python CLI unchanged; quoted paths containing spaces remain arguments.
$ErrorActionPreference = 'Stop'
& python (Join-Path $PSScriptRoot 'rw-agent-human.py') @args
exit $LASTEXITCODE
