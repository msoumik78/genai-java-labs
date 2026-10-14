#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
dir="$root/models/nomic-embed-text-v1.5"
mkdir -p "$dir"
base="https://huggingface.co/nomic-ai/nomic-embed-text-v1.5/resolve/main"
curl -L --fail -o "$dir/model_quantized.onnx" "$base/onnx/model_quantized.onnx"
curl -L --fail -o "$dir/tokenizer.json" "$base/tokenizer.json"
echo "Saved model under $dir"
