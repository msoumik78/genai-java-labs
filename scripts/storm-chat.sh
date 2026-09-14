#!/usr/bin/env bash
set -euo pipefail
n=${1:-10}
path=${2:-/chat/naive}
echo "GET $path x$n in parallel — watch /orders in another terminal"
seq "$n" | xargs -P "$n" -I{} curl -s -o /tmp/lab1-{}.txt -w "%{http_code} %{time_total} $path\\n" "http://127.0.0.1:18080${path}"
