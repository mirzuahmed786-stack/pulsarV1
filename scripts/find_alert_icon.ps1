$matches = @()
$paths = @('C:\Stellix\kotline-project\bridge-integrated\bridge-app', "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1")
foreach ($p in $paths) {
  if (Test-Path $p) {
    Get-ChildItem -Path $p -Recurse -File -Include *.aar,*.jar,*.zip -ErrorAction SilentlyContinue | ForEach-Object {
      try {
        $file = $_.FullName
        $zf = [System.IO.Compression.ZipFile]::OpenRead($file)
        foreach($entry in $zf.Entries) {
          if ($entry.FullName -like '*alert-icon.png*') {
            $matches += $file
            break
          }
        }
        $zf.Dispose()
      } catch { }
    }
  }
}
if ($matches.Count -gt 0) {
  $matches | Sort-Object -Unique | ForEach-Object { Write-Output $_ }
} else {
  Write-Output 'NO_MATCHES'
}
