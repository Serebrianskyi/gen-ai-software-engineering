#!/usr/bin/env bash
# Run the AI banking pipeline with an optional custom configuration.
#
# Usage:
#   ./run.sh                                         # default order: andromeda → sirius → vega → polaris
#   ./run.sh --config=configs/vega-first.json        # scrambled order — agents self-heal routing
#   ./run.sh --config=configs/no-vega.json           # skip compliance reporting
#   ./run.sh --rules=configs/strict-rules.json       # tighter fraud thresholds
#   ./run.sh --config=configs/vega-first.json --rules=configs/strict-rules.json
#   ./run.sh --dry-run                               # validate transactions only, no servers
#
# Both --config and --rules paths are resolved relative to the homework-6 directory.
# They default to pipeline-config.json and rules.json if not specified.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

CONFIG=""
RULES=""
DRY_RUN=""

for arg in "$@"; do
  case "$arg" in
    --config=*) CONFIG="$arg" ;;
    --rules=*)  RULES="$arg" ;;
    --dry-run)  DRY_RUN="--dry-run" ;;
    *)
      echo "Unknown argument: $arg"
      echo "Usage: ./run.sh [--config=FILE] [--rules=FILE] [--dry-run]"
      exit 1
      ;;
  esac
done

# Build the args string for Main.kt
ARGS="${CONFIG} ${RULES} ${DRY_RUN}"
ARGS="$(echo "$ARGS" | xargs)"   # trim extra spaces

echo ""
if [ -n "$CONFIG" ]; then
  echo "  Config : ${CONFIG#--config=}"
else
  echo "  Config : pipeline-config.json (default)"
fi
if [ -n "$RULES" ]; then
  echo "  Rules  : ${RULES#--rules=}"
else
  echo "  Rules  : rules.json (default)"
fi
echo ""

cd "$SCRIPT_DIR"

if [ -n "$ARGS" ]; then
  ./gradlew run --args="$ARGS" --quiet
else
  ./gradlew run --quiet
fi
