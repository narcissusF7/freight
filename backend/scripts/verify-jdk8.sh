#!/usr/bin/env bash
# Local JDK 8 capability probe: no Maven, no Spring, no network required.
# Compiles the pure-JDK integration mock layer and runs TLS / JCE / sign checks.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

CLIENT_DIR="freight-infra/src/main/java/com/lukachangtong/freight/infra/client"
OUT="target/jdk8-probe-classes"
SRC_LIST="target/jdk8-sources.txt"

mkdir -p "$OUT"
find "$CLIENT_DIR" -type f -name '*.java' > "$SRC_LIST"

echo "[verify] javac      : $(javac -version 2>&1)"
echo "[verify] sources    : $(wc -l < "$SRC_LIST") pure-JDK files"
javac -source 8 -target 8 -encoding UTF-8 -Xlint:-options -d "$OUT" @"$SRC_LIST"
echo "[verify] running Jdk8CapabilityProbe ..."
java -cp "$OUT" com.lukachangtong.freight.infra.client.mock.support.Jdk8CapabilityProbe
