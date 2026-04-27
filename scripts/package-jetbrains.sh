#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JETBRAINS_DIR="$ROOT_DIR/jetbrains"
PLUGIN_VERSION="$(sed -n 's/^pluginVersion=//p' "$JETBRAINS_DIR/gradle.properties" | head -n 1)"
PLUGIN_VERSION="${PLUGIN_VERSION:-0.1.0}"

WEBSTORM_APP="${WEBSTORM_APP:-/Applications/WebStorm.app}"
JAVAC="$WEBSTORM_APP/Contents/jbr/Contents/Home/bin/javac"

if [[ ! -x "$JAVAC" ]]; then
  echo "Cannot find javac at $JAVAC" >&2
  echo "Set WEBSTORM_APP=/path/to/WebStorm.app or install WebStorm." >&2
  exit 1
fi

BUILD_DIR="$JETBRAINS_DIR/build/manual"
CLASSES_DIR="$BUILD_DIR/classes"
JAR_DIR="$BUILD_DIR/jar"
DIST_DIR="$JETBRAINS_DIR/build/distributions"
JAR_FILE="$BUILD_DIR/reader-jetbrains.jar"
ZIP_FILE="$DIST_DIR/reader-jetbrains-$PLUGIN_VERSION.zip"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$JAR_DIR" "$DIST_DIR"

CP="$(find "$WEBSTORM_APP/Contents/lib" "$WEBSTORM_APP/Contents/plugins" -name '*.jar' | tr '\n' ':')"
"$JAVAC" --release 17 -encoding UTF-8 -cp "$CP" -d "$CLASSES_DIR" $(find "$JETBRAINS_DIR/src/main/java" -name '*.java')

cp -R "$JETBRAINS_DIR/src/main/resources/"* "$JAR_DIR/"
mkdir -p "$JAR_DIR/book-source-config"
cp "$ROOT_DIR"/book-source-config/*.json "$ROOT_DIR"/book-source-config/*.list "$ROOT_DIR"/book-source-config/README_zh.md "$JAR_DIR/book-source-config/"
cp -R "$CLASSES_DIR/"* "$JAR_DIR/"

(cd "$JAR_DIR" && zip -qr "$JAR_FILE" .)

rm -rf "$BUILD_DIR/Reader"
mkdir -p "$BUILD_DIR/Reader/lib"
cp "$JAR_FILE" "$BUILD_DIR/Reader/lib/reader-jetbrains.jar"
(cd "$BUILD_DIR" && zip -qr "$ZIP_FILE" Reader)

echo "$ZIP_FILE"
