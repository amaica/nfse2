#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

# Preferencia: src/main/resources/nfse.properties (copie de nfse.properties.example)
PROPS_FILE="${NFSE_CONFIG:-$ROOT/src/main/resources/nfse.properties}"
EXEC_ARGS="-Dexec.mainClass=io.github.t3wv.nfse.DemoRun"

if [[ -f "$PROPS_FILE" ]]; then
  EXEC_ARGS="$EXEC_ARGS -Dnfse.config=$PROPS_FILE"
else
  CERT_DIR="${CERTIFICADO_DIR:-/home/aurelio/FONTES/CERTIFICADOS}"
  export CADEIA_CERTIFICADOS_PATH="${CADEIA_CERTIFICADOS_PATH:-/tmp/nfse_cacerts.jks}"
  export CADEIA_CERTIFICADOS_SENHA="${CADEIA_CERTIFICADOS_SENHA:-senha}"
  export CERTIFICADO_PATH="${CERTIFICADO_PATH:-${CERT_DIR}/ClovisWerlang_safe1283.pfx}"
  export CERTIFICADO_SENHA="${CERTIFICADO_SENHA:-safe1283}"
  export NFSE_PRODUCAO="${NFSE_PRODUCAO:-true}"
  if [[ ! -f "${CERTIFICADO_PATH}" ]]; then
    echo "Crie $PROPS_FILE (veja nfse.properties.example) ou defina CERTIFICADO_PATH."
    exit 1
  fi
fi

if [[ $# -gt 0 ]]; then
  mvn -q compile exec:java $EXEC_ARGS -Dexec.args="$*"
else
  mvn -q compile exec:java $EXEC_ARGS
fi
