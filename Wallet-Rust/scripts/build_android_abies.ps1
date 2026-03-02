param(
    [string[]] $abis = @('arm64-v8a','armeabi-v7a','x86_64')
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$outDir = Join-Path $root "..\target\android-libs"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

if (-not (Get-Command cargo-ndk -ErrorAction SilentlyContinue)) {
    Write-Error "cargo-ndk not found. Install via 'cargo install cargo-ndk'"
    exit 2
}

foreach ($abi in $abis) {
    Write-Host "Building ABI: $abi"
    & cargo ndk -t $abi --release build -p wallet_core
    $src = Join-Path -Path (Join-Path $root "..\target\$abi\release") -ChildPath "libwallet_core.so"
    if (Test-Path $src) {
        $destDir = Join-Path $outDir $abi
        New-Item -ItemType Directory -Force -Path $destDir | Out-Null
        Copy-Item -Path $src -Destination (Join-Path $destDir "libwallet_core.so") -Force
        Write-Host "Copied $src -> $destDir"
    } else {
        Write-Warning "$src not found"
    }
}

Write-Host "Done. ABI libs in $outDir"
