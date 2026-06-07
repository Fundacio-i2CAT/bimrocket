#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_JAR="$SCRIPT_DIR/quarkus-run.jar"
RUNNING_FILE="$SCRIPT_DIR/running"

echo '================='
echo 'B I M R O C K E T'
echo '================='
echo ''

# ------------------------------------
# 1. Check existing running file
# ------------------------------------

if [ -f "$RUNNING_FILE" ]; then
  echo "Application is already running:"
  cat "$RUNNING_FILE"
  exit 1
fi

# ------------------------------------
# 2. Setup JVM
# ------------------------------------

. ./jvm.sh

# ------------------------------------
# 3. Start application
# ------------------------------------
echo "Starting application..."

nohup "$JAVA_EXEC" -jar "$APP_JAR" > /dev/null 2>&1 &

# ------------------------------------
# 4. Display startup info from Quarkus
# ------------------------------------

TIMEOUT=60
INTERVAL=1
ELAPSED=0

while [ ! -f "$RUNNING_FILE" ] && [ $ELAPSED -lt $TIMEOUT ]; do
  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
done

if [ -f "$RUNNING_FILE" ]; then
  echo "Application started."
  cat "$RUNNING_FILE"
else
  exit 1
fi
