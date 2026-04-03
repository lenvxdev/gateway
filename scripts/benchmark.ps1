Param(
    [string]$JarPath = "build/libs/Gateway-1.0.1-1.21.11.jar",
    [int]$WarmupSeconds = 15
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $JarPath)) {
    Write-Error "Jar not found: $JarPath"
}

$start = Get-Date
$proc = Start-Process -FilePath "java" -ArgumentList @("-jar", $JarPath, "--nogui") -PassThru

Start-Sleep -Seconds $WarmupSeconds

if (!$proc.HasExited) {
    $peakWorkingSetMb = [math]::Round($proc.PeakWorkingSet64 / 1MB, 2)
    $uptimeSeconds = [math]::Round(((Get-Date) - $start).TotalSeconds, 2)
    Write-Output "startup_seconds=$uptimeSeconds"
    Write-Output "peak_working_set_mb=$peakWorkingSetMb"
    Stop-Process -Id $proc.Id -Force
} else {
    Write-Output "Process exited before warmup window."
    Write-Output "exit_code=$($proc.ExitCode)"
}

