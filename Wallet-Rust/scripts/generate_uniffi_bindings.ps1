param(
    [string]$UdlPath = "../wallet.udl",
    [string]$OutDir = "../kotlin-bindings/src/main/kotlin"
)

Write-Host "Generating UniFFI Kotlin bindings"

if (-not (Get-Command uniffi-bindgen -ErrorAction SilentlyContinue)) {
    Write-Host "uniffi-bindgen not found. Install with: cargo install uniffi_bindgen_cli" -ForegroundColor Yellow
    exit 2
}

$udlFull = Resolve-Path $UdlPath
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }

uniffi-bindgen generate $udlFull --language kotlin --out-dir $OutDir
if ($LASTEXITCODE -ne 0) { Write-Host "uniffi-bindgen failed" -ForegroundColor Red; exit $LASTEXITCODE }

Write-Host "Bindings generated to $OutDir"
