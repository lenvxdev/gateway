#!/usr/bin/env bash
set -euo pipefail

JAR_PATH="${1:-build/libs/Gateway-1.0.1-1.21.11.jar}"
WARMUP_SECONDS="${2:-15}"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Jar not found: $JAR_PATH" >&2
  exit 1
fi

start_epoch="$(date +%s)"
java -jar "$JAR_PATH" --nogui >/dev/null 2>&1 &
pid="$!"

sleep "$WARMUP_SECONDS"

if kill -0 "$pid" 2>/dev/null; then
  current_epoch="$(date +%s)"
  startup_seconds="$(( current_epoch - start_epoch ))"
  peak_kb="$(ps -o rss= -p "$pid" | awk '{print $1}')"
  peak_mb="$(awk "BEGIN { printf \"%.2f\", ${peak_kb:-0}/1024 }")"
  echo "startup_seconds=$startup_seconds"
  echo "peak_rss_mb=$peak_mb"
  kill -9 "$pid" || true
else
  echo "Process exited before warmup window."
fi

