#!/usr/bin/env bash
set -euo pipefail
n=${1:-12}
echo "GET /orders x$n in parallel (expect ~fast 200s)"
seq "$n" | xargs -P "$n" -I{} curl -s -o /dev/null -w "%{http_code} %{time_total}\\n" http://127.0.0.1:18080/orders
