param(
    [Parameter(Mandatory = $true)]
    [string]$Port,

    [int]$BaudRate = 9600
)

$ErrorActionPreference = "Stop"
$serial = [System.IO.Ports.SerialPort]::new(
    $Port,
    $BaudRate,
    [System.IO.Ports.Parity]::None,
    8,
    [System.IO.Ports.StopBits]::One
)
$serial.NewLine = "`n"
$serial.ReadTimeout = 8000
$serial.WriteTimeout = 5000

try {
    $serial.Open()
    Write-Host "Opened $Port. Sending RFCOMM test message..."
    $message = "PHONEKEYS_RFCOMM_TEST_001"
    $serial.WriteLine($message)
    $reply = $serial.ReadLine().Trim()
    Write-Host "Received: $reply"

    $expected = "PHONEKEYS_ACK:$message"
    if ($reply -eq $expected) {
        Write-Host "SUCCESS: two-way RFCOMM/SPP test passed." -ForegroundColor Green
    } else {
        Write-Warning "A response arrived, but it did not match the expected acknowledgement."
        Write-Host "Expected: $expected"
    }
}
catch {
    Write-Error "RFCOMM COM-port test failed: $($_.Exception.Message)"
    exit 1
}
finally {
    if ($serial.IsOpen) { $serial.Close() }
}
