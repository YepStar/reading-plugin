#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JETBRAINS_DIR="$ROOT_DIR/jetbrains"

if [[ -z "${JETBRAINS_MARKETPLACE_TOKEN:-}" && -z "${INTELLIJ_PLATFORM_PUBLISHING_TOKEN:-}" ]]; then
  echo "Missing JetBrains Marketplace token." >&2
  echo "Set JETBRAINS_MARKETPLACE_TOKEN, then rerun this script." >&2
  exit 1
fi

TOKEN="${JETBRAINS_MARKETPLACE_TOKEN:-$INTELLIJ_PLATFORM_PUBLISHING_TOKEN}"
CHANNELS="${PLUGIN_CHANNELS:-default}"
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_CMD="${JAVA_CMD:-java}"

if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
  echo "Cannot find Java. Install JDK 17/21 or set JAVA_HOME before publishing." >&2
  exit 1
fi

JAVA_VERSION="$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
JAVA_MAJOR="${JAVA_VERSION%%.*}"
if [[ "$JAVA_MAJOR" == "1" ]]; then
  JAVA_MAJOR="$(echo "$JAVA_VERSION" | cut -d. -f2)"
fi
if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ && "$JAVA_MAJOR" -ge 25 ]]; then
  echo "Current Java is $JAVA_VERSION. Gradle Kotlin DSL for this project should be run with JDK 17 or 21." >&2
  echo "Set JAVA_HOME to a JDK 17/21 installation before publishing." >&2
  exit 1
fi

if [[ -x "$JETBRAINS_DIR/gradlew" ]]; then
  GRADLE="$JETBRAINS_DIR/gradlew"
elif [[ -x "$JETBRAINS_DIR/.tools/gradle-8.13/bin/gradle" ]]; then
  GRADLE="$JETBRAINS_DIR/.tools/gradle-8.13/bin/gradle"
elif [[ -f "$JETBRAINS_DIR/.tools/gradle-8.13-bin.zip" ]]; then
  unzip -q -n "$JETBRAINS_DIR/.tools/gradle-8.13-bin.zip" -d "$JETBRAINS_DIR/.tools"
  GRADLE="$JETBRAINS_DIR/.tools/gradle-8.13/bin/gradle"
else
  GRADLE="gradle"
fi

(cd "$JETBRAINS_DIR" && JAVA_HOME="${JAVA_HOME:-}" "$GRADLE" publishPlugin \
  -PintellijPlatformPublishingToken="$TOKEN" \
  -PpluginChannels="$CHANNELS")
