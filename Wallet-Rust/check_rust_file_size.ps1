param(
  [string]$RepoRoot = "."
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path $RepoRoot).Path
$maxLines = 600

$rustRoots = @(
  "backend/server/src",
  "backend/core/src",
  "backend/evm-tooling/src",
  "backend/solana-tooling/src",
  "core-rust/src",
  "wasm/src"
)

$violations = @()

foreach ($relativePath in $rustRoots) {
  $scanPath = Join-Path $root $relativePath
  if (-not (Test-Path $scanPath)) {
    continue
  }

  Get-ChildItem -Path $scanPath -Recurse -File -Filter *.rs | ForEach-Object {
    $lineCount = (Get-Content $_.FullName | Measure-Object -Line).Lines
    if ($lineCount -gt $maxLines) {
      $relativeFile = $_.FullName.Substring($root.Length + 1)
      $violations += [PSCustomObject]@{
        Path = $relativeFile
        Lines = $lineCount
      }
    }
  }
}

if ($violations.Count -gt 0) {
  Write-Host "Rust module size guard violations found:"
  $violations |
    Sort-Object -Property Lines -Descending |
    ForEach-Object { Write-Host "- $($_.Path): $($_.Lines) LOC (max $maxLines)" }
  exit 1
}

Write-Host "Rust module size guard passed."
