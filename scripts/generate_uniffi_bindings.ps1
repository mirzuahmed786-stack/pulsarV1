param(
    [string]$udlPath = "Wallet-Rust/wallet.udl",
    [string]$outDir = "Wallet-Rust/kotlin-bindings"
)

Write-Host "Generating UniFFI Kotlin bindings"
if (-not (Get-Command uniffi-bindgen -ErrorAction SilentlyContinue)) {
    Write-Error "uniffi-bindgen not found in PATH. Install via 'pip install uniffi_bindgen' or see UniFFI docs."
    exit 2
}

if (-not (Test-Path $udlPath)) {
    Write-Error "UDL file not found at $udlPath"
    exit 2
}

mkdir -Force $outDir | Out-Null
uniffi-bindgen generate --lang kotlin --out-dir $outDir $udlPath
if ($LASTEXITCODE -ne 0) { Write-Error "uniffi-bindgen failed"; exit $LASTEXITCODE }
Write-Host "Bindings generated to $outDir"
