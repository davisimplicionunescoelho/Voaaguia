param(
    [int]$Port = 18080,
    [string]$DatabaseName = 'voaaguia_demo_recording',
    [string]$DemoPassword = 'VoaAguiaDemo2026!'
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not $env:GEMINI_API_KEY) {
    $secureKey = Read-Host 'Cole a GEMINI_API_KEY (o texto ficara oculto)' -AsSecureString
    $keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    try {
        $env:GEMINI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
    }
}

if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
    throw 'GEMINI_API_KEY nao foi informada.'
}

$randomBytes = New-Object byte[] 48
$randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $randomGenerator.GetBytes($randomBytes)
} finally {
    $randomGenerator.Dispose()
}

$env:PORT = "$Port"
$env:MONGODB_URI = "mongodb://localhost:27017/$DatabaseName"
$env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)
$env:SEED_ENABLED = 'true'
$env:SEED_PASSWORD = $DemoPassword
$env:SEED_SAMPLE_DATA = 'true'
$env:GEMINI_MODEL = 'gemini-3.6-flash'

$taskTemp = Join-Path $env:PUBLIC 'voaaguia-tmp'
New-Item -ItemType Directory -Force -Path $taskTemp | Out-Null
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djava.io.tmpdir=$taskTemp -Djdk.net.unixdomain.tmpdir=$taskTemp"

Write-Host "Backend de demonstracao: http://localhost:$Port" -ForegroundColor Cyan
Write-Host "Swagger: http://localhost:$Port/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "Banco MongoDB: $DatabaseName" -ForegroundColor Cyan
Write-Host "Senha dos tres perfis: $DemoPassword" -ForegroundColor Yellow
Write-Host 'A chave da IA existe apenas neste processo e nao foi salva em arquivo.' -ForegroundColor Green

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
