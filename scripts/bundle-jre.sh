#!/usr/bin/env sh
# Bundle a minimal JRE for Stellaris BS Generator using jlink
# Output: frontend/src-tauri/jre/
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/../frontend/src-tauri/jre"

MODULES="java.base,java.logging,java.sql,java.naming,java.management,java.instrument,java.desktop,java.net.http,java.security.jgss,java.compiler,java.datatransfer,java.prefs,java.rmi,java.scripting,java.xml,java.xml.crypto,jdk.unsupported,jdk.crypto.ec,jdk.zipfs"

if [ -d "$OUTPUT_DIR" ]; then
    echo "Removing previous JRE bundle..."
    rm -rf "$OUTPUT_DIR"
fi

echo "Creating minimal JRE with jlink..."
echo "Modules: $MODULES"

jlink \
    --add-modules "$MODULES" \
    --output "$OUTPUT_DIR" \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress zip-6

echo ""
echo "JRE bundled successfully to: $OUTPUT_DIR"
du -sh "$OUTPUT_DIR" | awk '{print "Bundle size: " $1}'
