$cases = @(
  @{ name='BSC mainnet BNB->USDT'; chainId='56'; sell='0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE'; buy='0x55d398326f99059ff775485246999027b3197955'; amount='10000000000000000' },
  @{ name='BSC mainnet BNB->USDC'; chainId='56'; sell='0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE'; buy='0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d'; amount='10000000000000000' },
  @{ name='Polygon mainnet MATIC->USDC'; chainId='137'; sell='0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE'; buy='0x3c499c542cef5e3811e1192ce70d8cc03d5c3359'; amount='10000000000000000' },
  @{ name='Avalanche mainnet AVAX->USDC'; chainId='43114'; sell='0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE'; buy='0xB97EF9Ef8734C71904D8002F8b6Bc66Dd9c48a6E'; amount='10000000000000000' }
)
$taker='0x1dbc9995a0bb40ff14d4482b0ec0af6bb83a09e1'
foreach($c in $cases){
  $url = "http://localhost:3001/api/evm/quote?chainId=$($c.chainId)&sellToken=$($c.sell)&buyToken=$($c.buy)&sellAmount=$($c.amount)&taker=$taker&slippageBps=50"
  try {
    $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -Method GET
    $json = $resp.Content | ConvertFrom-Json
    $src = if($json.source){$json.source}else{$json._source}
    Write-Output ("PASS|{0}|{1}|{2}" -f $c.name,$resp.StatusCode,$src)
  } catch {
    $status = $_.Exception.Response.StatusCode.value__
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $body = $reader.ReadToEnd()
    Write-Output ("FAIL|{0}|{1}|{2}" -f $c.name,$status,$body)
  }
}
