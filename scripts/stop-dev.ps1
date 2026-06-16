# Frees the backend (8080) and frontend (5173) dev ports by killing whatever
# process is listening on them, regardless of how it was started (detached
# `npm run dev &`, an orphaned gradlew daemon, a crashed previous session).
param(
    [int[]]$Ports = @(8080, 5173)
)

foreach ($port in $Ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) {
        Write-Host "Port $port - nothing listening"
        continue
    }

    foreach ($procId in ($conns.OwningProcess | Select-Object -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { "unknown" }
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host "Port $port - killed PID $procId ($name)"
    }
}
