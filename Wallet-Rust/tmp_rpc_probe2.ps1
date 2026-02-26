$targets = @(
  @{name='sepolia.drpc'; url='https://sepolia.drpc.org'},
  @{name='sepolia.1rpc'; url='https://1rpc.io/sepolia'}
)
$body = '{"jsonrpc":"2.0","id":1,"method":"eth_blockNumber","params":[]}'
foreach($t in $targets){
  $u = 'http://localhost:3001/api/proxy/rpc?url=' + [System.Uri]::EscapeDataString($t.url)
  try {
    $resp = Invoke-WebRequest -UseBasicParsing -Method POST -Uri $u -ContentType 'application/json' -Body $body
    Write-Output ("RESULT|{0}|{1}|{2}" -f $t.name,$resp.StatusCode,$resp.Content)
  } catch {
    $status = $_.Exception.Response.StatusCode.value__
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $txt = $reader.ReadToEnd()
    Write-Output ("ERROR|{0}|{1}|{2}|{3}" -f $t.name,$status,$u,$txt)
  }
}
