param([switch]$Demo, [switch]$SeedDemo, [int]$DatabasePort = 3307)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if ($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME/bin/java.exe")) {
    $javaCommand = "$env:JAVA_HOME/bin/java.exe"
} else {
    $javaCommand = (Get-Command java -ErrorAction Stop).Source
}
$jarPath = Join-Path $projectRoot 'dist/fleet-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    & "$projectRoot/mvnw.cmd" package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed. See the output above.' }
    $jarPath = Join-Path $projectRoot 'target/fleet-1.0.0.jar'
}
$appArgs = @('-jar', $jarPath)
if ($Demo) {
    $appArgs += '--spring.profiles.active=demo'
} else {
    if (-not $env:DB_URL) { $env:DB_URL = "jdbc:mysql://localhost:${DatabasePort}/fleet?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Europe/Moscow" }
    if (-not $env:DB_USER) { $env:DB_USER = 'fleet_user' }
    if (-not $env:DB_PASSWORD) {
        $securePassword = Read-Host 'MySQL password' -AsSecureString
        $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
    }
    if ($SeedDemo) { $appArgs += '--app.seed-demo=true' }
}
Write-Host 'Open http://localhost:8082 after the Started FleetApplication message. Stop: Ctrl+C.'
try { & $javaCommand @appArgs; $appExitCode = $LASTEXITCODE }
finally { if (-not $Demo) { Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue } }
exit $appExitCode
