#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_JAR="$SCRIPT_DIR/bimrocket-console.jar"

# ------------------------------------
# 1. Setup JVM
# ------------------------------------

. ./jvm.sh

if [ -z "$JAVA_EXEC" ]; then
    echo "Java not installed."
    exit 1
fi

# ------------------------------------
# 2. Start application
# ------------------------------------

"$JAVA_EXEC" -Xmx4g -jar "$APP_JAR" "$1"
