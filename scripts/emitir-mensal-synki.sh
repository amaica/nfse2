#!/usr/bin/env bash
# Emite NFS-e mensais Synki (MAICA) a partir das operações salvas e baixa DANFSe PDF.
# Uso:
#   ./scripts/emitir-mensal-synki.sh                  # local :8080
#   API_URL=http://127.0.0.1:8088 ./scripts/emitir-mensal-synki.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API="${API_URL:-http://127.0.0.1:8080}"
EMAIL="${PORTAL_EMAIL:-admin@synki.demo}"
SENHA="${PORTAL_SENHA:-demo123}"
EMPRESA_ID="${EMPRESA_ID:-107}"
OUT_DIR="${OUT_DIR:-$ROOT/tmp/danfse-$(date +%Y%m)}"
COMPETENCIA="${COMPETENCIA:-$(date +%Y-%m-%d)}"
mkdir -p "$OUT_DIR"

echo "==> API $API | empresa $EMPRESA_ID | competência $COMPETENCIA"

LOGIN=$(curl -sf -X POST "$API/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"senha\":\"$SENHA\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "==> Login OK"

TROCA=$(curl -sf -X POST "$API/api/auth/trocar-empresa" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"empresaId\":$EMPRESA_ID}")
TOKEN=$(echo "$TROCA" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "==> Empresa ativa: $(echo "$TROCA" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('empresaNome'), d.get('empresaId'))")"

OPS=$(curl -sf "$API/api/nfse/operacoes-mensais" -H "Authorization: Bearer $TOKEN")
echo "$OPS" | python3 -m json.tool
IDS=$(echo "$OPS" | python3 -c "import sys,json; print(' '.join(str(o['id']) for o in json.load(sys.stdin)))")
if [[ -z "${IDS// }" ]]; then
  echo "Nenhuma operação mensal cadastrada."
  exit 1
fi

for ID in $IDS; do
  NOME=$(echo "$OPS" | python3 -c "import sys,json; ops=json.load(sys.stdin); print(next(o['nome'] for o in ops if o['id']==$ID))")
  echo ""
  echo "==> Emitindo #$ID — $NOME"
  RESP=$(curl -sS -w "\nHTTP:%{http_code}" -X POST "$API/api/nfse/operacoes-mensais/$ID/emitir" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"competencia\":\"$COMPETENCIA\"}")
  HTTP=$(echo "$RESP" | tail -1 | cut -d: -f2)
  BODY=$(echo "$RESP" | sed '$d')
  echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
  if [[ "$HTTP" != "200" ]]; then
    echo "Falha HTTP $HTTP em $NOME"
    exit 1
  fi
  CHAVE=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('chaveAcesso',''))")
  PDF="$OUT_DIR/${NOME// /_}-${CHAVE}.pdf"
  echo "==> Baixando DANFSe $CHAVE"
  for i in 1 2 3 4 5 6 7 8; do
    CODE=$(curl -sS -o "$PDF" -w "%{http_code}" \
      "$API/api/nfse/pdf/$CHAVE" -H "Authorization: Bearer $TOKEN" || echo 000)
    if [[ "$CODE" == "200" && -s "$PDF" ]]; then
      echo "PDF OK: $PDF ($(wc -c < "$PDF") bytes)"
      break
    fi
    echo "PDF ainda não disponível (HTTP $CODE), tentando de novo..."
    sleep 5
  done
done

echo ""
echo "=== Concluído. PDFs em $OUT_DIR ==="
ls -la "$OUT_DIR"
