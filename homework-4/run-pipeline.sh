#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f "node_modules/.bin/jest" ]; then
  echo "[pipeline] Installing dependencies..."
  npm install --silent
fi

node pipeline.js