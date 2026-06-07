#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_JAR="$SCRIPT_DIR/bimrocket-console.jar"

# ------------------------------------
# 1. Setup JVM
# ------------------------------------

. ./jvm.sh

# ------------------------------------
# 2. Start application
# ------------------------------------

"$JAVA_EXEC" -Xmx4g -jar "$APP_JAR"
