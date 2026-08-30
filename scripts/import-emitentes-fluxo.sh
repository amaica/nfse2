#!/usr/bin/env bash
# Importa todos os emitentes de fluxo.empresa (MySQL 100.84.22.20) para nfse_portal
# e vincula admin@synki.demo a todos os emitentes.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API="${API_URL:-http://localhost:8080}"
ADMIN_KEY="${NFSE_ADMIN_SECRET:-admin-change-me}"

if [[ -f "$ROOT/portal-api/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/portal-api/.env"
  set +a
fi
ADMIN_KEY="${NFSE_ADMIN_SECRET:-$ADMIN_KEY}"
API="http://localhost:${PORT:-8080}"

echo "==> Importando emitentes de fluxo.empresa @ 100.84.22.20 ..."
res=$(curl -sf -X POST "${API}/api/admin/fluxo/import?force=true" \
  -H "X-Admin-Key: ${ADMIN_KEY}" \
  -H "Content-Type: application/json") || {
  echo "ERRO: API nao respondeu. Confira FLUXO_IMPORT_ENABLED=true e reinicie o portal."
  exit 1
}

echo "$res" | python3 -m json.tool 2>/dev/null || echo "$res"

echo ""
echo "==> Verificando emitentes no portal..."
mysql -h localhost -u"${DB_USER:-root}" -p"${DB_PASSWORD:-root}" -N -e "
SELECT COUNT(*) AS empresas FROM nfse_portal.empresa;
SELECT COUNT(*) AS vinculos_admin FROM nfse_portal.usuario_empresa ue
  JOIN nfse_portal.usuario u ON u.id = ue.usuario_id
  WHERE u.email = 'admin@synki.demo' AND ue.ativo = 1;
" 2>/dev/null

echo "Concluido."
