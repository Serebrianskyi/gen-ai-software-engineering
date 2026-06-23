#!/usr/bin/env bash
# Removes all pipeline-generated JSON files from shared/ subdirectories.
# Run before ./gradlew run to start with a completely clean state.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SHARED="$SCRIPT_DIR/shared"

dirs=(input processing output results)
total=0

for dir in "${dirs[@]}"; do
    path="$SHARED/$dir"
    if [ -d "$path" ]; then
        count=$(find "$path" -maxdepth 1 -name "*.json" | wc -l | tr -d ' ')
        if [ "$count" -gt 0 ]; then
            rm -f "$path"/*.json
            echo "  cleared $count file(s) from shared/$dir/"
        else
            echo "  shared/$dir/ already empty"
        fi
        total=$((total + count))
    fi
done

echo ""
echo "Done — $total file(s) removed."