$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    throw "Khong tim thay adb.exe: $adb"
}
$connected = @()
foreach ($line in (& $adb devices)) {
    if ($line -match '^([^\s]+)\s+device$') {
        $connected += $Matches[1]
    }
}
if ($connected.Count -eq 0) {
    throw "Chua co emulator hoac thiet bi dang ket noi ADB."
}
foreach ($serial in $connected) {
    & $adb -s $serial reverse tcp:8000 tcp:8000
    if ($LASTEXITCODE -ne 0) { throw "ADB reverse that bai: $serial" }
    Write-Host "ADB reverse OK: $serial -> may tinh:8000"
}
