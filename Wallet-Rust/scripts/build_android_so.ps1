param(
    [string]$CrateDir = "..",
    [string[]]$Targets = @("arm64-v8a", "armeabi-v7a", "x86_64"),
    [string]$OutJniLibs = "../front-kotlin/android/android/app/src/main/jniLibs"
)

Write-Host "Building native Android .so for targets: $($Targets -join ', ')"

if (-not (Get-Command cargo-ndk -ErrorAction SilentlyContinue)) {
    Write-Host "cargo-ndk not found. Install with: cargo install cargo-ndk" -ForegroundColor Yellow
    exit 2
}

Push-Location $CrateDir
try {
    $targetArgs = $Targets | ForEach-Object { "-t $_" } | Out-String
    # Build release libs for specified ABIs and output to a temporary folder
    cargo ndk -t $($Targets -join ',') -o $OutJniLibs build --release
    if ($LASTEXITCODE -ne 0) { Write-Host "cargo ndk build failed" -ForegroundColor Red; exit $LASTEXITCODE }
    Write-Host "Native libraries placed under $OutJniLibs"
} finally {
    Pop-Location
}
