# rw-agent Windows regression runner.
# Mirrors developer/test.sh; Windows also runs two Reachability harness variants.
# Differences from test.sh, both forced by this environment and documented in the handoff notes:
#   1. The build passes source files directly to javac instead of writing an @argfile, because
#      cmd.exe writes the argfile in the OEM code page while javac reads it in another, which
#      corrupts paths that contain non-ASCII characters (e.g. "deepseek 工作台").
#   2. Python is invoked as `python` (there is no `python3` on Windows).
# Usage: test-win.ps1 <path-to-game-lib.jar> <path-to-libs-dir>
param(
    [Parameter(Mandatory = $true)][string]$GameJar,
    [Parameter(Mandatory = $true)][string]$LibsDir
)
$ErrorActionPreference = 'Continue'
# The Python client regressions decode the child JVM's stdout as UTF-8. On a Windows console whose
# code page is not UTF-8 the JVM emits the OEM code page instead, which makes those tests fail on a
# decode error rather than on behaviour, so child JVMs are pinned to UTF-8 here.
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$root = $PSScriptRoot
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$python = 'python'
$classesDir = Join-Path $root 'build\classes'
$testsDir = Join-Path $root 'build\tests'
$distJar = Join-Path $root 'dist\rw-agent-bootstrap.jar'
$logDir = Join-Path $root 'build\logs'
foreach ($d in @($classesDir, $testsDir, $logDir)) {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}
New-Item -ItemType Directory -Force -Path (Join-Path $root 'dist') | Out-Null

$results = New-Object System.Collections.ArrayList
function Step([string]$name, [scriptblock]$body) {
    Write-Host "=== $name ===" -ForegroundColor Cyan
    $out = Join-Path $logDir (($name -replace '[^A-Za-z0-9_.-]', '_') + '.log')
    $code = 0
    try { & $body *>&1 | Tee-Object -FilePath $out } catch { Write-Host $_; $code = 1 }
    if ($LASTEXITCODE -ne $null) { $code = $LASTEXITCODE }
    $ok = ($code -eq 0)
    [void]$results.Add([pscustomobject]@{ Step = $name; Exit = $code; Ok = $ok })
    Write-Host ("--> {0} exit={1}" -f $(if ($ok) { 'PASS' } else { 'FAIL' }), $code) -ForegroundColor $(if ($ok) { 'Green' } else { 'Red' })
}

# ---- build ----
Step 'build:compile' {
    $files = Get-ChildItem -Recurse (Join-Path $root 'src') -Filter *.java | ForEach-Object { $_.FullName }
    & $java -m jdk.compiler/com.sun.tools.javac.Main --release 8 -encoding UTF-8 -cp $GameJar -d $classesDir $files
}
Step 'build:jar' {
    & $java -m jdk.jartool/sun.tools.jar.Main --create --file $distJar --manifest (Join-Path $root 'MANIFEST.MF') -C $classesDir . -C (Join-Path $root 'resources') .
}
Step 'build:test-classes' {
    $cp = "$GameJar;$LibsDir\*;$distJar"
    $harnesses = Get-ChildItem (Join-Path $root 'tests') -Filter *.java | ForEach-Object { $_.FullName }
    & $java -m jdk.compiler/com.sun.tools.javac.Main -cp $cp -d $testsDir $harnesses
}

$cp = "$GameJar;$LibsDir\*;$distJar;$testsDir"
function JavaStep([string]$name, [string[]]$javaArgs) {
    Step $name { & $java --add-modules jdk.httpserver -cp $cp @javaArgs }
}
function PyStep([string]$name, [string]$script, [string[]]$pyArgs) {
    # Historical policy suites prove the explicit legacy path. G3 has its own enabled HTTP contracts.
    $previousJavaOptions = $env:JAVA_TOOL_OPTIONS
    try {
        if ($script -notin @('test_g3_execution.py','test_g4_runtime.py','test_g41_runtime.py','test_g5_runtime.py')) { $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions -Drwagent.g3Execution=false" }
        Step $name { & $python (Join-Path $root "tests\$script") @pyArgs }
    } finally { $env:JAVA_TOOL_OPTIONS = $previousJavaOptions }
}

JavaStep 'java:SmokeHarness' @('SmokeHarness')
JavaStep 'java:G54ExecutionFlowHarness' @('io.rwagent.client.G54ExecutionFlowHarness')
JavaStep 'java:G55OperationsHarness' @('io.rwagent.client.G55OperationsHarness')
JavaStep 'java:R7BuilderDemandHarness' @('io.rwagent.client.R7BuilderDemandHarness')
JavaStep 'java:R8BuilderWorkHarness' @('io.rwagent.client.R8BuilderWorkHarness')
JavaStep 'java:R12BuilderWorkHarness' @('io.rwagent.client.R12BuilderWorkHarness')
JavaStep 'java:R12BuilderNativeHarness' @('R12BuilderNativeHarness')
JavaStep 'java:R13BuilderOpportunityHarness' @('io.rwagent.client.R13BuilderOpportunityHarness')
JavaStep 'java:R13MineQuoteNativeHarness' @('R13MineQuoteNativeHarness')
JavaStep 'java:R13ScoutNavigationHarness' @('io.rwagent.client.R13ScoutNavigationHarness')
JavaStep 'java:R13MarginalProductionHarness' @('io.rwagent.client.R13MarginalProductionHarness')
JavaStep 'java:R13ThreatDetachmentHarness' @('io.rwagent.client.R13ThreatDetachmentHarness')
JavaStep 'java:R13AirLifecycleHarness' @('io.rwagent.client.R13AirLifecycleHarness')
JavaStep 'java:R13AirNativeHarness' @('R13AirNativeHarness')
JavaStep 'java:R14BuilderFlowHarness' @('io.rwagent.client.R14BuilderFlowHarness')
JavaStep 'java:R14LiveReportHarness' @('io.rwagent.client.R14LiveReportHarness')
JavaStep 'java:R14HeavyArtilleryHarness' @('io.rwagent.client.R14HeavyArtilleryHarness')
JavaStep 'java:R14HeavyArtilleryNativeHarness' @('R14HeavyArtilleryNativeHarness', (Join-Path $logDir 'r14-heavy-native'))
JavaStep 'java:R14AirFlowHarness' @('io.rwagent.client.R14AirFlowHarness')
JavaStep 'java:R14AirNativeHarness' @('R14AirNativeHarness')
JavaStep 'java:R15AirClearanceHarness' @('io.rwagent.client.R15AirClearanceHarness')
JavaStep 'java:R16GroundBaseHarness' @('io.rwagent.client.R16GroundBaseHarness')
JavaStep 'java:R16BuilderRegionHarness' @('io.rwagent.client.R16BuilderRegionHarness')
JavaStep 'java:R15HeadlessPlayerHarness' @('io.rwagent.headless.R15HeadlessPlayerHarness')
JavaStep 'java:R15AirNativeHarness' @('R15AirNativeHarness')
JavaStep 'java:R8ExecutionAdmissionHarness' @('io.rwagent.client.R8ExecutionAdmissionHarness')
JavaStep 'java:R8BaseAdmissionHarness' @('io.rwagent.client.R8BaseAdmissionHarness')
JavaStep 'java:R8FrontierContinuationHarness' @('io.rwagent.client.R8FrontierContinuationHarness')
JavaStep 'java:R8AirSearchHarness' @('io.rwagent.client.R8AirSearchHarness')
JavaStep 'java:R9ProductionTimeoutHarness' @('io.rwagent.client.R9ProductionTimeoutHarness')
JavaStep 'java:R9SiegeLifecycleHarness' @('io.rwagent.client.R9SiegeLifecycleHarness')
JavaStep 'java:R9BuilderCoverageHarness' @('io.rwagent.client.R9BuilderCoverageHarness')
JavaStep 'java:R9AirInvestmentHarness' @('io.rwagent.client.R9AirInvestmentHarness')
JavaStep 'java:R10TemporaryResponseHarness' @('io.rwagent.client.R10TemporaryResponseHarness')
JavaStep 'java:R10AirFormationsHarness' @('io.rwagent.client.R10AirFormationsHarness')
JavaStep 'java:R10ScoutContinuityHarness' @('io.rwagent.client.R10ScoutContinuityHarness')
JavaStep 'java:R10ScoutBridgeHarness' @('R10ScoutBridgeHarness')
JavaStep 'java:R10FrontierFundingHarness' @('io.rwagent.client.R10FrontierFundingHarness')
JavaStep 'java:R10MilitaryInvestmentHarness' @('io.rwagent.client.R10MilitaryInvestmentHarness')
JavaStep 'java:R11FrontlineContinuityHarness' @('io.rwagent.client.R11FrontlineContinuityHarness')
JavaStep 'java:R11GeneralMotionHarness' @('io.rwagent.client.R11GeneralMotionHarness')
JavaStep 'java:R11AirContinuityHarness' @('io.rwagent.client.R11AirContinuityHarness')
JavaStep 'java:R11ScoutMotionHarness' @('io.rwagent.client.R11ScoutMotionHarness')
JavaStep 'java:R11ScoutBridgeSafetyHarness' @('io.rwagent.bootstrap.R11ScoutBridgeSafetyHarness')
JavaStep 'java:G7ScoutMissionHarness' @('io.rwagent.client.G7ScoutMissionHarness')
JavaStep 'java:G6EndgameHarness' @('io.rwagent.client.G6EndgameHarness')
JavaStep 'java:G6CapitalPolicyHarness' @('io.rwagent.client.G6CapitalPolicyHarness')
JavaStep 'java:AirReconTerrainHarness' @('AirReconTerrainHarness')
JavaStep 'java:BridgeHarness' @('BridgeHarness')
JavaStep 'java:BridgeHarness-disabled' @('BridgeHarness', 'disabled')
JavaStep 'java:EconomyHarness' @('EconomyHarness')
JavaStep 'java:OpeningHarness' @('OpeningHarness')
JavaStep 'java:OpeningHarness-replacement' @('OpeningHarness', 'replacement')
JavaStep 'java:ProductionPlanHarness' @('ProductionPlanHarness')
PyStep 'py:test_development' 'test_development.py' @($distJar)
PyStep 'py:test_opening' 'test_opening.py' @($distJar)
PyStep 'py:test_economy' 'test_economy.py' @($distJar)
PyStep 'py:test_client' 'test_client.py' @($distJar)
PyStep 'py:test_match_bootstrap' 'test_match_bootstrap.py' @($distJar)
JavaStep 'java:DiagnosticsHarness' @('DiagnosticsHarness')
JavaStep 'java:PreflightHarness' @('PreflightHarness')
PyStep 'py:test_reports' 'test_reports.py' @()
JavaStep 'java:ScoutHarness' @('ScoutHarness')
JavaStep 'java:TerrainMemoryHarness' @('TerrainMemoryHarness')
JavaStep 'java:GuardHarness' @('GuardHarness')
JavaStep 'java:CapabilityHarness' @('CapabilityHarness')
JavaStep 'java:TargetCompatibilityHarness' @('TargetCompatibilityHarness')
JavaStep 'java:BuilderHarness' @('BuilderHarness')
JavaStep 'java:ReachabilityHarness' @('ReachabilityHarness')
# 输出24 §P0: the same harness re-run with the diagnostics arm flag, which is the only way the banned
# engine-call groups can execute. This step proves the sandbox path still works after the live incident.
Step 'java:ReachabilityHarness-sandbox' { & $java --add-modules jdk.httpserver '-Drwagent.reachabilityDiagnostics=true' -cp $cp ReachabilityHarness }
PyStep 'py:test_frontier' 'test_frontier.py' @($distJar)
PyStep 'py:test_frontier_reports' 'test_frontier_reports.py' @()
JavaStep 'java:CombatHarness' @('CombatHarness')
PyStep 'py:test_battle_reports' 'test_battle_reports.py' @()
PyStep 'py:test_battle_client' 'test_battle_client.py' @($distJar)
PyStep 'py:test_production_capacity' 'test_production_capacity.py' @($distJar)
PyStep 'py:test_strategy_funding' 'test_strategy_funding.py' @($distJar)
PyStep 'py:test_specialist_lifecycle' 'test_specialist_lifecycle.py' @($distJar)
PyStep 'py:test_local_army' 'test_local_army.py' @($distJar)
PyStep 'py:test_surplus_spending' 'test_surplus_spending.py' @($distJar)
PyStep 'py:test_engineer_provider' 'test_engineer_provider.py' @($distJar)
PyStep 'py:test_target_compatibility' 'test_target_compatibility.py' @($distJar)
PyStep 'py:test_recon_client' 'test_recon_client.py' @($distJar)
PyStep 'py:test_recon_frontier_client' 'test_recon_frontier_client.py' @($distJar)
PyStep 'py:test_g1_trace' 'test_g1_trace.py' @($distJar)
PyStep 'py:test_g2_world_state' 'test_g2_world_state.py' @($distJar)
PyStep 'py:test_g3_execution' 'test_g3_execution.py' @($distJar)
PyStep 'py:test_g4_runtime' 'test_g4_runtime.py' @($distJar)
PyStep 'py:test_g41_runtime' 'test_g41_runtime.py' @($distJar)
PyStep 'py:test_g5_runtime' 'test_g5_runtime.py' @($distJar)
PyStep 'py:test_human_observer' 'test_human_observer.py' @()
PyStep 'py:test_native_clock_display' 'test_native_clock_display.py' @()
PyStep 'py:test_headless_parallel' 'test_headless_parallel.py' @()
PyStep 'py:test_ab_aggregate' 'test_ab_aggregate.py' @()
PyStep 'py:test_ab_campaign' 'test_ab_campaign.py' @()
Step 'java:ReportCommitHarness' { & $java -Xmx96m -cp $cp io.rwagent.client.ReportCommitHarness }
Step 'java:CompactBattleLogHarness' { & $java -cp $cp io.rwagent.client.CompactBattleLogHarness }
Step 'java:ExecutionContractHarness' { & $java -cp $cp io.rwagent.client.ExecutionContractHarness }
Step 'java:ExecutionSchedulerHarness' { & $java -cp $cp io.rwagent.client.ExecutionSchedulerHarness }
Step 'java:CapabilityLifecycleHarness' { & $java -cp $cp io.rwagent.client.CapabilityLifecycleHarness }
Step 'java:NativeCreditWitnessHarness' { & $java -cp $cp io.rwagent.client.NativeCreditWitnessHarness }
Step 'java:StrategyQuoteProvenanceHarness' { & $java -cp $cp io.rwagent.client.StrategyQuoteProvenanceHarness }
Step 'java:GeneralRegistryHarness' { & $java -cp $cp io.rwagent.client.GeneralRegistryHarness }
Step 'java:ForceControllerHarness' { & $java -cp $cp io.rwagent.client.ForceControllerHarness }
Step 'java:GeneralFormationHarness' { & $java -cp $cp io.rwagent.client.GeneralFormationHarness }
Step 'java:ForceFormationHarness' { & $java -cp $cp io.rwagent.client.ForceFormationHarness }
Step 'java:G5CommanderHarness' { & $java -cp $cp io.rwagent.client.G5CommanderHarness }
Step 'java:CommanderBudgetHarness' { & $java -cp $cp io.rwagent.client.CommanderBudgetHarness }
Step 'java:G52FightZonesHarness' { & $java -cp $cp io.rwagent.client.G52FightZonesHarness }
Step 'java:G53FreeResponseHarness' { & $java -cp $cp io.rwagent.client.G53FreeResponseHarness }
Step 'java:G53OutwardMissionHarness' { & $java -cp $cp io.rwagent.client.G53OutwardMissionHarness }
Step 'java:G53ContinuingOperationsHarness' { & $java --add-modules jdk.httpserver -cp $cp io.rwagent.client.G53ContinuingOperationsHarness }
Step 'java:R4ConstructionHarness' { & $java -cp $cp io.rwagent.client.R4ConstructionHarness }
Step 'java:GlobalStrategyPolicyHarness' { & $java -cp $cp io.rwagent.client.GlobalStrategyPolicyHarness }
Step 'java:RecoveryPoolHarness' { & $java -cp $cp io.rwagent.client.RecoveryPoolHarness }
Step 'java:LocalCoverageHarness' { & $java -cp $cp io.rwagent.client.LocalCoverageHarness }
Step 'java:GeneralRosterRefreshHarness' { & $java -cp $cp io.rwagent.client.GeneralRosterRefreshHarness }
Step 'java:G5CombatHarness' { & $java -cp $cp io.rwagent.client.G5CombatHarness }
Step 'java:G51OperationsHarness' { & $java -cp $cp io.rwagent.client.G51OperationsHarness }
Step 'java:G51TacticalHarness' { & $java -cp $cp io.rwagent.client.G51TacticalHarness }
Step 'java:G51LogisticsHarness' { & $java -cp $cp io.rwagent.client.G51LogisticsHarness }
Step 'java:G51ProspectIntegrationHarness' { & $java -cp $cp io.rwagent.client.G51ProspectIntegrationHarness }
Step 'java:RuntimeAccelerationHarness' { & $java --add-modules jdk.httpserver -cp $cp io.rwagent.client.RuntimeAccelerationHarness }
Step 'java:G1TraceHarness' { & $java -cp $cp io.rwagent.client.G1TraceHarness }
Step 'java:WorldStateHarness' { & $java -cp $cp io.rwagent.client.WorldStateHarness }
Step 'java:StrategyContractHarness' { & $java -cp $cp io.rwagent.client.StrategyContractHarness }
Step 'java:ProductionCapacityHarness' { & $java -cp $cp io.rwagent.client.ProductionCapacityHarness }
Step 'java:LocalArmyContractHarness' { & $java -cp $cp io.rwagent.client.LocalArmyContractHarness }
Step 'java:LocalCrisisContractHarness' { & $java -cp $cp io.rwagent.client.LocalCrisisContractHarness }
Step 'java:SurplusSpendingHarness' { & $java -cp $cp io.rwagent.client.SurplusSpendingHarness }
Step 'java:MineInvestmentHarness' { & $java -cp $cp io.rwagent.client.MineInvestmentHarness }
Step 'java:EngineerProviderHarness' { & $java -cp $cp io.rwagent.client.EngineerProviderHarness }
JavaStep 'java:NativeMorphHarness' @('NativeMorphHarness')
JavaStep 'java:StrategyNativeHarness' @('StrategyNativeHarness')
PyStep 'py:test_feedback_progress_audit' 'test_feedback_progress_audit.py' @()
PyStep 'py:test_global_strategy_audit' 'test_global_strategy_audit.py' @()

Write-Host ''
Write-Host '===== SUMMARY =====' -ForegroundColor Yellow
$results | Format-Table -AutoSize | Out-String | Write-Host
$failed = @($results | Where-Object { -not $_.Ok })
$javaRuns = @($results | Where-Object { $_.Step -like 'java:*' }).Count
$pyRuns = @($results | Where-Object { $_.Step -like 'py:*' }).Count
Write-Host ("Java harness runs: {0}   Python suites: {1}   failed steps: {2}" -f $javaRuns, $pyRuns, $failed.Count)
if ($failed.Count -gt 0) { exit 1 } else { exit 0 }
