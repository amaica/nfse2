#!/usr/bin/env bash
# Roda DemoRun sem Maven exec (evita ClassNotFound em IDE/STS com target inconsistente)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

PROPS="${NFSE_CONFIG:-$ROOT/src/main/resources/nfse.properties}"
[[ -f "$PROPS" ]] || { echo "Crie $PROPS (copie nfse.properties.example)"; exit 1; }

mvn -q compile
CP="$ROOT/target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)"
exec java -Dnfse.config="$PROPS" -cp "$CP" io.github.t3wv.nfse.DemoRun "$@"
