# Requires Windows PowerShell 5.1+ and existing, legally installed full frozen kits.
# Only copies original working kits to local-kits/; does not alter source kits.
param(
    [ValidateSet('R9','R12','R15','R16','All')][string]$Version = 'All',
    [switch]$ForceOverwrite
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$versions = if($Version -eq 'All'){@('R9','R12','R15','R16')}else{@($Version)}
foreach($name in $versions){
    $metaPath = Join-Path $repo "versions/$name/VERSION.json"
    $meta = Get-Content -LiteralPath $metaPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $src = $meta.local_full_kit
    $dst = Join-Path $repo "local-kits/$name"
    if(-not (Test-Path -LiteralPath $src)){throw "Missing original $name local kit at $src"}
    if((Test-Path -LiteralPath $dst) -and -not $ForceOverwrite){throw "$dst exists. Refusing to overwrite without -ForceOverwrite"}
    New-Item -ItemType Directory -Force -Path $dst | Out-Null
    Write-Host "Copying $name original kit to $dst (may consume hundreds of MB)..."
    & robocopy.exe $src $dst /E /R:0 /W:0 /XJ /XD runs /XF *.log *.partial *.pyc | Out-Null
    if($LASTEXITCODE -ge 8){throw "robocopy failure: $LASTEXITCODE"}
    $jar=Join-Path $dst 'game/rw-agent-bootstrap.jar'
    $actual=(Get-FileHash -Algorithm SHA256 -LiteralPath $jar).Hash.ToLowerInvariant()
    if($actual -ne $meta.agent_jar_sha256.ToLowerInvariant()){throw "$name JAR mismatch, expected $($meta.agent_jar_sha256) actual $actual"}
    Write-Host "[$name] copied and JAR hash verified; launcher: $dst/1-Start-Game.bat"
}
Write-Host 'Completed. Each environment has its own game, runtime, agent and port.'
