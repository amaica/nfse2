#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PROPS="$ROOT/src/main/resources/nfse.properties"
EXAMPLE="$ROOT/src/main/resources/nfse.properties.example"

if [[ ! -f "$PROPS" ]]; then
  cp "$EXAMPLE" "$PROPS"
  echo "Criado $PROPS — edite certificado e municipio antes de rodar no STS."
else
  echo "Ja existe: $PROPS"
fi

mvn -q compile
echo "OK. No STS: botao direito em DemoRun.launch -> Run As, ou Run As -> Java Application em DemoRun.java"
