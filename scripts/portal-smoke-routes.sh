#!/usr/bin/env bash
# Smoke test: rotas API autenticadas + páginas Next.js
set -euo pipefail

API="${API_URL:-http://localhost:8080}"
WEB="${WEB_URL:-http://localhost:3000}"
EMAIL="${PORTAL_EMAIL:-admin@synki.demo}"
SENHA="${PORTAL_SENHA:-demo123}"

fail=0

check_api() {
  local path="$1"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${API}${path}")
  if [[ "$code" =~ ^2 ]]; then
    echo "OK  $code $path"
  else
    echo "FAIL $code $path"
    fail=$((fail + 1))
  fi
}

check_web() {
  local code path="$1"
  code=$(curl -s -o /dev/null -w "%{http_code}" "${WEB}${path}")
  if [[ "$code" == "200" ]]; then
    echo "OK  $code $path"
  else
    echo "FAIL $code $path"
    fail=$((fail + 1))
  fi
}

echo "==> Login API"
TOKEN=$(curl -sf -X POST "${API}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${SENHA}\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])") || {
  echo "ERRO: login falhou"
  exit 1
}
echo "Token obtido."

echo ""
echo "==> Rotas API (autenticadas)"
for path in \
  /api/auth/me \
  /api/empresas \
  /api/tribut-grupo-tributario \
  /api/tribut-operacao-fiscal \
  /api/tribut-configura-of-gt \
  /api/tribut-nfse-servico \
  /api/pessoas \
  /api/produto \
  /api/cfop \
  /api/ncm \
  /api/veiculo \
  /api/usuario \
  /api/nfse/emissao/contexto \
  /api/conta/assinatura \
  /api/conta/metricas \
  /api/conta/auditoria \
  /api/conta/usuarios \
  /api/conta/convites; do
  check_api "$path"
done

echo ""
echo "==> Páginas web"
for path in \
  /login \
  /registrar \
  /painel \
  /cadastros/empresa \
  /cadastros/pessoas \
  /cadastros/produtos \
  /cadastros/usuarios \
  /tributacao/grupo-tributario \
  /nfse/emissao \
  /nfe/emissao \
  /nfe/notas-emitidas \
  /conta/assinatura; do
  check_web "$path"
done

echo ""
if [[ $fail -eq 0 ]]; then
  echo "Smoke test OK."
  exit 0
fi
echo "Smoke test FALHOU ($fail rota(s))."
exit 1
