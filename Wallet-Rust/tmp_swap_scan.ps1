$cases = @(
  @{ name='BSC BNB->USDT'; chainId='56'; buy='0x55d398326f99059ff775485246999027b3197955' },
  @{ name='BSC BNB->USDC'; chainId='56'; buy='0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d' },
  @{ name='BSC BNB->BUSD'; chainId='56'; buy='0xe9e7cea3dedca5984780bafc599bd69add087d56' },
  @{ name='BSC BNB->WETH'; chainId='56'; buy='0x2170ed0880ac9a755fd29b2688956bd959f933f8' },
  @{ name='Polygon MATIC->USDC.e'; chainId='137'; buy='0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174' },
  @{ name='Polygon MATIC->USDT'; chainId='137'; buy='0xc2132d05d31c914a87c6611c10748aeb04b58e8f' },
  @{ name='Polygon MATIC->WETH'; chainId='137'; buy='0x7ceB23fD6bC0adD59E62ac25578270cFf1b9f619' },
  @{ name='Avalanche AVAX->USDT'; chainId='43114'; buy='0x9702230a8ea53601f5cd2dc00fdbc13d4df4a8c7' },
  @{ name='Avalanche AVAX->USDC'; chainId='43114'; buy='0xB97EF9Ef8734C71904D8002F8b6Bc66Dd9c48a6E' },
  @{ name='Avalanche AVAX->USDC.e'; chainId='43114'; buy='0xA7D7079b0FEAD91F3e65f86E8915Cb59c1a4C664' },
  @{ name='Avalanche AVAX->WETH.e'; chainId='43114'; buy='0x49D5c2BdFfac6CE2BFdB6640F4F80f226bc10bAB' }
)
$taker='0x1dbc9995a0bb40ff14d4482b0ec0af6bb83a09e1'
$sell='0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE'
$amount='10000000000000000'
foreach($c in $cases){
  $url = "http://localhost:3001/api/evm/quote?chainId=$($c.chainId)&sellToken=$sell&buyToken=$($c.buy)&sellAmount=$amount&taker=$taker&slippageBps=50"
  try {
    $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -Method GET
    $json = $resp.Content | ConvertFrom-Json
    $src = if($json.source){$json.source}else{$json._source}
    Write-Output ("PASS|{0}|{1}|{2}" -f $c.name,$resp.StatusCode,$src)
  } catch {
    $status = $_.Exception.Response.StatusCode.value__
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $body = $reader.ReadToEnd()
    $code=''
    if($body){ try { $obj=$body | ConvertFrom-Json; $code=$obj.error.code } catch {} }
    Write-Output ("FAIL|{0}|{1}|{2}" -f $c.name,$status,$code)
  }
}
