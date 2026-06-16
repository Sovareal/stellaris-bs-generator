#!/usr/bin/env bash
# Frees the backend (8080) and frontend (5173) dev ports by killing whatever
# process is listening on them, regardless of how it was started (detached
# `npm run dev &`, an orphaned gradlew daemon, a crashed previous session).
if [ "$#" -eq 0 ]; then
    ports=(8080 5173)
else
    ports=("$@")
fi

for port in "${ports[@]}"; do
    pids=$(lsof -ti tcp:"$port" | sort -u)
    if [ -z "$pids" ]; then
        echo "Port $port - nothing listening"
        continue
    fi

    for pid in $pids; do
        name=$(ps -p "$pid" -o comm= 2>/dev/null)
        kill -9 "$pid" 2>/dev/null
        echo "Port $port - killed PID $pid ($name)"
    done
done
