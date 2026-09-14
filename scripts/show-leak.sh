#!/usr/bin/env bash
set -euo pipefail
BASE=${BASE:-http://127.0.0.1:18082}
Q=${Q:-"What is PROJECT-ORION and the acquisition price?"}
TENANT=${TENANT:-acme}

hit() {
  local path=$1
  echo "=== $path ==="
  curl -sS -G "$BASE$path" --data-urlencode "tenant=$TENANT" --data-urlencode "q=$Q"
  echo
  echo
}

hit /search/naive
hit /search/prompt
hit /search/sql
hit /search/rls
