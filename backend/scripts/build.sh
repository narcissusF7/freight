#!/usr/bin/env bash
# Build with the Maven bundled in IntelliJ IDEA (no Maven on PATH needed).
# Example: ./scripts/build.sh clean compile
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IDEA_MAVEN="${IDEA_MAVEN_HOME:-/d/Program Files/JetBrains/IntelliJ IDEA 2026.1/plugins/maven/lib/maven3}"
MVN="$IDEA_MAVEN/bin/mvn"

if [ ! -f "$MVN" ]; then
  echo "[build] IDEA Maven not found: $MVN"
  echo "[build] set IDEA_MAVEN_HOME to your IDEA plugins/maven/lib/maven3 directory"
  exit 1
fi

echo "[build] using $MVN"
cd "$ROOT"
"$MVN" "$@"
