#!/usr/bin/env bash
# Envia certificado PFX para o portal (substitui o carregado por CERTIFICADO_PATH na subida).
set -euo pipefail

API="${API_URL:-http://localhost:8080}"
EMAIL="${PORTAL_EMAIL:-admin@synki.demo}"
SENHA_LOGIN="${PORTAL_SENHA:-demo123}"
PFX="${CERTIFICADO_PATH:?Defina CERTIFICADO_PATH}"
PFX_SENHA="${CERTIFICADO_SENHA:?Defina CERTIFICADO_SENHA}"

TOKEN=$(curl -sf -X POST "$API/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"senha\":\"$SENHA_LOGIN\"}" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -sf -X POST "$API/api/certificado" \
  -H "Authorization: Bearer $TOKEN" \
  -F "arquivo=@$PFX" \
  -F "senha=$PFX_SENHA"

echo ""
echo "Certificado enviado. Abra: http://localhost:3000/embed?t=$TOKEN"
