param([switch]$Battle)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previewRoot = Join-Path (Split-Path -Parent $projectRoot) 'regnum-codex-build'
$lockPath = Join-Path $previewRoot 'coord\BUILD.lock'
if (!(Test-Path -LiteralPath (Join-Path $previewRoot 'run-visual\saves\codex-showcase-20261002\level.dat'))) {
    throw 'The Regnum preview world has not been prepared yet.'
}
if ((Get-Content -LiteralPath $lockPath -Raw).Trim() -ne 'free') {
    throw 'The preview copy is currently running a test. Wait for it to finish, then try again.'
}
$jdkRoot = Join-Path $projectRoot '.jdk\jdk-21.0.12.1+1'
if (!(Test-Path -LiteralPath (Join-Path $jdkRoot 'bin\java.exe'))) { throw 'Project Java runtime is missing.' }
$previousJava = $env:JAVA_HOME
try {
    Set-Content -LiteralPath $lockPath $(if ($Battle) { 'busy codex user-battle' } else { 'busy codex user-preview' })
    $env:JAVA_HOME = $jdkRoot
    Push-Location $previewRoot
    try {
        $runTask = if ($Battle) { 'runBattleShowcase' } else { 'runShowcase' }
        & (Join-Path $previewRoot 'gradlew.bat') -p $previewRoot --offline --no-daemon $runTask
        if ($LASTEXITCODE -ne 0) { throw "Preview exited with code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    Set-Content -LiteralPath $lockPath 'free'
}
