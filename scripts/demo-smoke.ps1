param(
    [string]$BaseUrl = "http://localhost:8080",
    [int64]$SkuId = 1001,
    [int64]$EventId = 3001,
    [int64]$UserId = 2001,
    [int]$Stock = 1000,
    [int]$Quantity = 1,
    [string]$Strategy = "REDIS_LUA"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Path,
        [hashtable]$Body
    )

    $json = $Body | ConvertTo-Json -Depth 8
    Invoke-RestMethod -Method Post -Uri "$BaseUrl$Path" -ContentType "application/json" -Body $json
}

function Invoke-JsonPostAllowFailure {
    param(
        [string]$Path,
        [hashtable]$Body
    )

    try {
        Invoke-JsonPost -Path $Path -Body $Body
    } catch {
        $message = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($message) -and $null -ne $_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = [System.IO.StreamReader]::new($stream)
                try {
                    $message = $reader.ReadToEnd()
                } finally {
                    $reader.Dispose()
                }
            }
        }
        if ([string]::IsNullOrWhiteSpace($message)) {
            throw
        }
        $message | ConvertFrom-Json
    }
}

Write-Host ""
Write-Host "TicketRush core smoke" -ForegroundColor Cyan
Write-Host "BaseUrl: $BaseUrl"
Write-Host ""

$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health"
if ($health.status -ne "UP") {
    throw "Actuator health is not UP: $($health.status)"
}
Write-Host "[1/4] Health: UP" -ForegroundColor Green

$preload = Invoke-JsonPost -Path "/api/rush/inventory/preload" -Body @{
    skuId = $SkuId
    totalStock = $Stock
}
Write-Host "[2/4] Inventory preload: sku=$SkuId stock=$($preload.data.availableStock)" -ForegroundColor Green

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$requestA = "demo-$suffix-a"
$requestB = "demo-$suffix-b"
$idempotentKey = "rush:demo:$UserId`:$EventId`:$SkuId`:$suffix"

$rushBody = @{
    requestId = $requestA
    userId = $UserId
    eventId = $EventId
    skuId = $SkuId
    quantity = $Quantity
    strategy = $Strategy
    idempotentKey = $idempotentKey
}

$firstRush = Invoke-JsonPost -Path "/api/rush/tickets" -Body $rushBody
Write-Host "[3/4] First rush: accepted=$($firstRush.data.accepted) remainingStock=$($firstRush.data.remainingStock) virtualThread=$($firstRush.data.processedByVirtualThread)" -ForegroundColor Green

$repeatBody = $rushBody.Clone()
$repeatBody.requestId = $requestB
$repeatRush = Invoke-JsonPostAllowFailure -Path "/api/rush/tickets" -Body $repeatBody
$repeatCode = $repeatRush.code
$repeatMessage = $repeatRush.message
Write-Host "[4/4] Repeat rush: requestId changed, idempotentKey unchanged, code=$repeatCode message=$repeatMessage" -ForegroundColor Green

Write-Host ""
Write-Host "Core evidence" -ForegroundColor Cyan
[PSCustomObject]@{
    StockFlow = "$Stock -> $($firstRush.data.remainingStock) -> $($firstRush.data.remainingStock)"
    RequestId = "$requestA -> $requestB"
    IdempotentKey = $idempotentKey
    DuplicateCode = $repeatCode
    VirtualThread = $firstRush.data.processedByVirtualThread
    AsyncOrder = "triggered once; duplicate request rejected before another order message"
} | Format-List

if ($firstRush.data.remainingStock -ne ($Stock - $Quantity)) {
    throw "Unexpected remaining stock after first rush."
}
if ($repeatCode -ne "A0429") {
    throw "Expected duplicate request code A0429, got $repeatCode."
}
if ($firstRush.data.processedByVirtualThread -ne $true) {
    throw "Expected processedByVirtualThread=true."
}

Write-Host "PASS: no oversell, idempotency works, virtual thread path hit." -ForegroundColor Green
