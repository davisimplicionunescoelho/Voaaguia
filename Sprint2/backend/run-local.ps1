$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (Test-Path -LiteralPath '.env') {
    Get-Content -LiteralPath '.env' | ForEach-Object {
        if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
    }
}
if (-not $env:JWT_SECRET) { throw 'Copie .env.example para .env e configure JWT_SECRET antes de executar.' }
$taskTemp = Join-Path $env:PUBLIC 'voaaguia-tmp'
New-Item -ItemType Directory -Force -Path $taskTemp | Out-Null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djava.io.tmpdir=$taskTemp -Djdk.net.unixdomain.tmpdir=$taskTemp"
& "$PSScriptRoot/mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
