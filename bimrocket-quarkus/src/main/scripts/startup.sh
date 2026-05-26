#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_JAR="$SCRIPT_DIR/quarkus-run.jar"
RUNNING_FILE="$SCRIPT_DIR/running"
LOCAL_JVM_DIR="$SCRIPT_DIR/jvm"

JAVA_VERSION="${JAVA_VERSION:-21}"   # Default LTS version

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
# 2. Resolve Java executable
# ------------------------------------
JAVA_EXEC=""

# 2.1 Check local bundled JVM first
if [ -d "$LOCAL_JVM_DIR" ] && [ -x "$LOCAL_JVM_DIR/bin/java" ]; then
  echo "Local JVM found in $LOCAL_JVM_DIR, using it."
  JAVA_EXEC="$LOCAL_JVM_DIR/bin/java"
else
  echo "Checking system Java..."

  # 2.2 Check system Java installation
  if command -v java >/dev/null 2>&1; then
    SYS_JAVA_VER=$(java -version 2>&1 \
      | awk -F '"' '/version/ {print $2}' \
      | awk -F '.' '{if ($1=="1") print $2; else print $1}')

    if [ -n "$SYS_JAVA_VER" ] && [ "$SYS_JAVA_VER" -eq 21 ]; then
      echo "System Java found (version $SYS_JAVA_VER). Using system Java."
      JAVA_EXEC="java"
    else
      echo "System Java version ($SYS_JAVA_VER) is not supported (requires 21)."
    fi
  else
    echo "Java is not available in system PATH."
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
      "https://api.adoptium.net/v3/assets/latest/${JAVA_VERSION}/hotspot?image_type=jdk&os=${OS}&architecture=${ARCH}&heap_size=normal&type=tar.gz" \
      | jq -r '.[0].binary.package.link')

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
    else
      echo "Error: Unexpected JVM archive structure."
      exit 1
    fi
  fi
fi

# ------------------------------------
# 4. Start application
# ------------------------------------
echo "Starting application..."

nohup "$JAVA_EXEC" -jar -Dstartup-info.path=$FILE "$APP_JAR" > /dev/null 2>&1 &

# ------------------------------------
# 5. Display startup info from Quarkus
# ------------------------------------

TIMEOUT=10
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
