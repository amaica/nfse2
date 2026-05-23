#!/usr/bin/env bash
# Configura variáveis para o portal NFS-e em HOMOLOGAÇÃO (produção restrita nacional).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

export CERTIFICADO_PATH="${CERTIFICADO_PATH:-/home/aurelio/FONTES/CERTIFICADOS/ClovisWerlang_safe1283.pfx}"
export CERTIFICADO_SENHA="${CERTIFICADO_SENHA:-safe1283}"
export NFSE_AMBIENTE="${NFSE_AMBIENTE:-homologacao}"
export MUNICIPIO_IBGE="${MUNICIPIO_IBGE:-4310009}"
export PREFEITURA="${PREFEITURA:-Ibiruba/RS}"

if [[ ! -f "$CERTIFICADO_PATH" ]]; then
  echo "ERRO: PFX não encontrado: $CERTIFICADO_PATH"
  echo "Defina CERTIFICADO_PATH e CERTIFICADO_SENHA apontando para seu certificado de homologação."
  exit 1
fi

echo "Homologação NFS-e nacional"
echo "  Certificado: $CERTIFICADO_PATH"
echo "  Ambiente:    $NFSE_AMBIENTE"
echo "  Município:   $MUNICIPIO_IBGE ($PREFEITURA)"
echo ""
echo "Subindo portal..."
exec "$ROOT/start-portal.sh"
