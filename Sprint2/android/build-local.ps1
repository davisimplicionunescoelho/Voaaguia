param([string]$ApiBaseUrl = 'http://10.0.2.2:8080/api/')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$taskTemp = Join-Path $env:PUBLIC 'voaaguia-tmp'
New-Item -ItemType Directory -Force -Path $taskTemp | Out-Null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djava.io.tmpdir=$taskTemp -Djdk.net.unixdomain.tmpdir=$taskTemp"
& "$PSScriptRoot/gradlew.bat" assembleDebug "-PAPI_BASE_URL=$ApiBaseUrl"
exit $LASTEXITCODE
