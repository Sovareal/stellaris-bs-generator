#!/usr/bin/env bash
# Frees the backend (8080) and frontend (5173) dev ports by killing whatever
# process is listening on them, regardless of how it was started (detached
# `npm run dev &`, an orphaned gradlew daemon, a crashed previous session).
#
# Uses lsof when available (Linux/macOS, or a Windows shell with lsof installed).
# Falls back to Windows' native netstat/taskkill when lsof is missing -- this is
# the common case on Git Bash/MSYS on Windows, which does not ship lsof. A prior
# version of this script silently reported "nothing listening" on such a missing
# lsof, even when a process actually held the port (see phase-80 LEARNING.md).
if [ "$#" -eq 0 ]; then
    ports=(8080 5173)
else
    ports=("$@")
fi

kill_with_lsof() {
    local port="$1"
    local pids
    pids=$(lsof -ti tcp:"$port" | sort -u)
    if [ -z "$pids" ]; then
        echo "Port $port - nothing listening"
        return
    fi

    local pid name
    for pid in $pids; do
        name=$(ps -p "$pid" -o comm= 2>/dev/null)
        kill -9 "$pid" 2>/dev/null
        echo "Port $port - killed PID $pid ($name)"
    done
}

kill_with_netstat_taskkill() {
    local port="$1"
    local pids
    # netstat -ano columns: Proto  LocalAddress  ForeignAddress  State  PID
    # Match TCP + LISTENING + local address ending in :$port (anchored, so 8080
    # doesn't also match 18080), dedupe (IPv4 and IPv6 listeners share one PID).
    pids=$(netstat -ano 2>/dev/null \
        | awk -v p=":$port" '$1=="TCP" && $4=="LISTENING" && $2 ~ (p"$") {print $5}' \
        | sort -u)
    if [ -z "$pids" ]; then
        echo "Port $port - nothing listening"
        return
    fi

    local pid name
    for pid in $pids; do
        name=$(tasklist //FI "PID eq $pid" //FO CSV //NH 2>/dev/null | cut -d',' -f1 | tr -d '"')
        # Double-slash flags (//F, //PID) are required from Git Bash/MSYS -- a
        # single leading slash gets rewritten as a filesystem path (e.g.
        # `/F` -> `C:/Program Files/Git/F`) before it reaches taskkill.exe.
        taskkill //PID "$pid" //F >/dev/null 2>&1
        echo "Port $port - killed PID $pid (${name:-unknown})"
    done
}

if command -v lsof >/dev/null 2>&1; then
    kill_fn=kill_with_lsof
elif command -v netstat >/dev/null 2>&1 && command -v taskkill >/dev/null 2>&1; then
    kill_fn=kill_with_netstat_taskkill
else
    echo "Neither lsof nor netstat+taskkill are available -- can't inspect listening ports." >&2
    exit 1
fi

for port in "${ports[@]}"; do
    "$kill_fn" "$port"
done
