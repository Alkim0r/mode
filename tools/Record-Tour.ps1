$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$lockPath = Join-Path $root 'coord\BUILD.lock'
if ((Get-Content -LiteralPath $lockPath -Raw).Trim() -ne 'free') { throw 'Build or test is running. Wait and retry.' }
$jdkRoot = Join-Path $root '.jdk\jdk-21.0.12.1+1'
$prev = $env:JAVA_HOME
try {
    Set-Content -LiteralPath $lockPath 'busy claude record-tour'
    $env:JAVA_HOME = $jdkRoot
    Remove-Item -Recurse -Force (Join-Path $root 'run-tour\screenshots') -ErrorAction SilentlyContinue
    Push-Location $root
    try {
        & (Join-Path $root 'gradlew.bat') -p $root --offline --no-daemon runTourCapture 2>&1 | Tee-Object -FilePath (Join-Path $root 'tour.log')
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $prev
    Set-Content -LiteralPath $lockPath 'free'
}
