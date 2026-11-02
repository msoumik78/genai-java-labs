#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
dest="$root/models/ms-marco-MiniLM-L6"
mkdir -p "$dest"
curl -fsSL -o "$dest/tokenizer.json" \
  "https://huggingface.co/Xenova/ms-marco-MiniLM-L-6-v2/resolve/main/tokenizer.json"
curl -fsSL -o "$dest/model.onnx" \
  "https://huggingface.co/Xenova/ms-marco-MiniLM-L-6-v2/resolve/main/onnx/model_quantized.onnx"
echo "Saved cross-encoder to $dest"
