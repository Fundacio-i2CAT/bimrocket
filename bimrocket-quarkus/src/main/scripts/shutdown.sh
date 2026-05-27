#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RUNNING_FILE="$SCRIPT_DIR/running"

if [ ! -f "$RUNNING_FILE" ]; then
  echo "Is application running?"
  exit 1
fi

rm -f "$RUNNING_FILE"
echo "Application stopped."
