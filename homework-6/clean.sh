#!/usr/bin/env bash
# Removes all pipeline-generated JSON files from shared/results/.
# Run before ./run.sh to start with a clean state.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS="$SCRIPT_DIR/shared/results"

if [ -d "$RESULTS" ]; then
    count=$(find "$RESULTS" -maxdepth 1 -name "*.json" | wc -l | tr -d ' ')
    if [ "$count" -gt 0 ]; then
        rm -f "$RESULTS"/*.json
        echo "  cleared $count file(s) from shared/results/"
    else
        echo "  shared/results/ already empty"
    fi
else
    mkdir -p "$RESULTS"
    echo "  created shared/results/"
fi

echo ""
echo "Done."
