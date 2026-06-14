#!/bin/bash

# Use system JVM
USE_SYSTEM_JVM=true

# Default Java LTS version
JAVA_VERSION="21"

# Local JVM path
LOCAL_JVM_DIR="$HOME/bimrocket-jvm-$JAVA_VERSION"

JAVA_EXEC=""

# ------------------------------------
# 1. Check local bundled JVM first
# ------------------------------------

if [ -x "$LOCAL_JVM_DIR/bin/java" ]; then
  echo "Local JVM found in $LOCAL_JVM_DIR, using it."
  JAVA_EXEC="$LOCAL_JVM_DIR/bin/java"
elif [ -x "$LOCAL_JVM_DIR/Contents/Home/bin/java" ]; then
  echo "Local JVM found in $LOCAL_JVM_DIR, using it."
  JAVA_EXEC="$LOCAL_JVM_DIR/Contents/Home/bin/java"
else

# ------------------------------------
# 2. Check System JVM
# ------------------------------------

  if $USE_SYSTEM_JVM; then
    echo "Checking system Java..."

    if command -v java >/dev/null 2>&1; then
      SYS_JAVA_VER=$(java -version 2>&1 \
        | awk -F '"' '/version/ {print $2}' \
        | awk -F '.' '{if ($1=="1") print $2; else print $1}')

      if [ -n "$SYS_JAVA_VER" ] && [ "$SYS_JAVA_VER" -eq "$JAVA_VERSION" ]; then
        echo "System Java found (version $SYS_JAVA_VER). Using system Java."
        JAVA_EXEC="java"
      else
        echo "System Java version ($SYS_JAVA_VER) is not supported (requires $JAVA_VERSION)."
      fi
    else
      echo "Java is not available in system PATH."
    fi
  fi

# ------------------------------------
# 3. Download JVM only if required
# ------------------------------------

  if [ -z "$JAVA_EXEC" ]; then
    echo "No valid Java found. Preparing JVM download..."

    # Detect operating system
    OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
    ARCH="$(uname -m)"

    case "$OS" in
      linux) OS="linux" ;;
      darwin) OS="mac" ;;
      mingw*|msys*|cygwin*) OS="windows" ;;
      *) echo "Unsupported operating system: $OS" && exit 1 ;;
    esac

    # Detect architecture
    case "$ARCH" in
      x86_64|amd64) ARCH="x64" ;;
      aarch64|arm64) ARCH="aarch64" ;;
      armv7l) ARCH="arm" ;;
      *) echo "Unsupported architecture: $ARCH" && exit 1 ;;
    esac

    echo "Resolving Temurin JVM for ${OS}/${ARCH} (Java ${JAVA_VERSION})..."

    # Query Adoptium API for latest JDK download URL
    JVM_DOWNLOAD_URL=$(curl -s \
      "https://api.adoptium.net/v3/assets/latest/${JAVA_VERSION}/hotspot?image_type=jre&os=${OS}&architecture=${ARCH}&heap_size=normal&type=tar.gz" \
      | python3 -c '
import sys, json
print(json.load(sys.stdin)[0]["binary"]["package"]["link"])
')

    if [ -z "$JVM_DOWNLOAD_URL" ] || [ "$JVM_DOWNLOAD_URL" == "null" ]; then
      echo "Error: Unable to resolve JVM download URL"
      exit 1
    fi

    echo "Downloading JVM from: $JVM_DOWNLOAD_URL"

    # Create JVM directory and extract runtime
    mkdir -p "$LOCAL_JVM_DIR"

    curl -L "$JVM_DOWNLOAD_URL" | tar -xzp -C "$LOCAL_JVM_DIR" --strip-components=1

    if [ -x "$LOCAL_JVM_DIR/bin/java" ]; then
      echo "JVM successfully downloaded and installed."
      JAVA_EXEC="$LOCAL_JVM_DIR/bin/java"
    elif [ -x "$LOCAL_JVM_DIR/Contents/Home/bin/java" ]; then
      echo "JVM successfully downloaded and installed."
      JAVA_EXEC="$LOCAL_JVM_DIR/Contents/Home/bin/java"
    else
      echo "Error: Unexpected JVM archive structure."
      exit 1
    fi
  fi
fi
