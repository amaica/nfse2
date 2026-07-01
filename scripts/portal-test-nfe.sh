#!/usr/bin/env bash
# Teste NF-e homologação: admin setup → login → contexto → emissão lote síncrono.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT/portal-api/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/portal-api/.env"
  set +a
fi

API="${API_URL:-http://localhost:8080}"
ADMIN_KEY="${NFSE_ADMIN_SECRET:-@lface#81}"
CNPJ="${PORTAL_CNPJ_NFE:-22659870004}"
SENHA="${PORTAL_SENHA:-demo123}"
CERT="${NFE_CERT_PATH:-$ROOT/ClovisWerlang_safe1283.pfx}"
CERT_SENHA="${NFE_CERT_SENHA:-safe1283}"

echo "==> API: $API"
echo "==> CNPJ: $CNPJ"
echo "==> Certificado: $CERT"

for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$API/api/auth/login" -X POST \
    -H 'Content-Type: application/json' -d '{"senha":"x"}' 2>/dev/null || echo "000")
  if [[ "$code" =~ ^[245] ]]; then break; fi
  if [[ $i -eq 40 ]]; then echo "API indisponível — rode ./start-portal.sh"; exit 1; fi
  sleep 2
done

EMPRESA_JSON=$(curl -sf -H "X-Admin-Key: $ADMIN_KEY" "$API/api/admin/empresas/cnpj/$CNPJ" 2>/dev/null || echo "")

if [[ -z "$EMPRESA_JSON" ]]; then
  echo "==> Criando empresa $CNPJ"
  EMPRESA_JSON=$(curl -sf -X POST "$API/api/admin/empresas" \
    -H "X-Admin-Key: $ADMIN_KEY" \
    -H 'Content-Type: application/json' \
    -d "$(python3 - <<PY
import json
print(json.dumps({
  "cnpj": "$CNPJ",
  "nome": "CLOVIS ANTONIO WERLANG",
  "nomeFantasia": "CLOVIS WERLANG",
  "email": "nfe.cloviswerlang@cloviswerlang.com.br",
  "telefone": "54999999999",
  "inscricaoEstadual": "2281016751",
  "cep": "99450000",
  "logradouro": "LI CRISTAL",
  "numero": "100",
  "bairro": "SEGUNDO",
  "municipio": "SELBACH",
  "uf": "RS",
  "cnaePrincipal": "4712100",
  "optanteSimples": False,
  "prefeitura": "Selbach",
  "codigoMunicipioIbge": "4320305",
  "ambiente": "homologacao",
  "emailIntegracao": "nfe.cloviswerlang@cloviswerlang.com.br",
  "senhaIntegracao": "$SENHA",
  "usuarioNome": "Integracao NF-e",
  "serieNfe": "921",
  "ultimoNumeroNfe": 0,
  "enderecos": [{
    "apelido": "Matriz Selbach",
    "cep": "99450000",
    "logradouro": "LI CRISTAL",
    "numero": "100",
    "bairro": "SEGUNDO",
    "municipio": "SELBACH",
    "uf": "RS",
    "codigoMunicipioIbge": "4320305",
    "inscricaoEstadual": "2281016751",
    "serieNfe": "921",
    "ultimoNumeroNfe": 0,
    "principal": True,
    "ativo": True
  }]
}))
PY
)")
  EMPRESA_ID=$(echo "$EMPRESA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['empresa']['id'])")
else
  EMPRESA_ID=$(echo "$EMPRESA_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  echo "==> Empresa existente id=$EMPRESA_ID"
fi

echo "==> Empresa id=$EMPRESA_ID"

echo "==> Garantindo série NF-e 921 (e-CPF Selbach)"
curl -sf -X PUT "$API/api/admin/empresas/$EMPRESA_ID" \
  -H "X-Admin-Key: $ADMIN_KEY" \
  -H 'Content-Type: application/json' \
  -d "$(python3 - <<PY
import json
det = json.loads('''$(curl -sf -H "X-Admin-Key: $ADMIN_KEY" "$API/api/admin/empresas/$EMPRESA_ID")''')
ends = det.get("enderecos") or []
if not ends:
    ends = [{"apelido": "Matriz Selbach", "principal": True, "ativo": True,
             "municipio": "SELBACH", "uf": "RS", "codigoMunicipioIbge": "4320305",
             "inscricaoEstadual": "2281016751", "serieNfe": "921", "ultimoNumeroNfe": 0}]
else:
    for e in ends:
        e["serieNfe"] = "921"
print(json.dumps({"serieNfe": "921", "enderecos": ends}))
PY
)" > /dev/null || true

if [[ ! -f "$CERT" ]]; then
  ALT="/home/aurelio/FONTES/CERTIFICADOS/ClovisWerlang_safe1283.pfx"
  if [[ -f "$ALT" ]]; then CERT="$ALT"; else echo "Certificado não encontrado: $CERT"; exit 1; fi
fi

echo "==> Upload certificado"
curl -sf -X POST "$API/api/admin/empresas/$EMPRESA_ID/certificado" \
  -H "X-Admin-Key: $ADMIN_KEY" \
  -F "arquivo=@${CERT}" \
  -F "senha=${CERT_SENHA}" | python3 -m json.tool

LOGIN_JSON=$(curl -sf -X POST "$API/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"cnpj\":\"$CNPJ\",\"senha\":\"$SENHA\"}")
TOKEN=$(echo "$LOGIN_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "==> Login OK"

echo "==> Status serviço NF-e"
curl -s "$API/api/nfe/status-servico" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "==> Contexto emissão"
CTX=$(curl -sf "$API/api/nfe/emissao/contexto" -H "Authorization: Bearer $TOKEN")
echo "$CTX" | python3 -m json.tool

END_ID=$(echo "$CTX" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['enderecos'][0]['id'] if d.get('enderecos') else '')")

echo "==> Enviando lote NF-e homologação"
RESULT=$(curl -s -X POST "$API/api/nfe/lotes/enviar" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "$(python3 - <<PY
import json
body = {"sincrono": True, "naturezaOperacao": "VENDA DE MERCADORIA"}
if "$END_ID":
    body["enderecoId"] = int("$END_ID")
print(json.dumps(body))
PY
)")
echo "$RESULT" | python3 -m json.tool

SUCESSO=$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('statusProtocolo')=='100' or d.get('sucesso') is True and d.get('statusProtocolo') in (None,''))")
STATUS_PROT=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('statusProtocolo','?'))")
if [[ "$STATUS_PROT" != "100" ]]; then
  echo "ERRO na emissão NF-e (status protocolo: $STATUS_PROT)"
  exit 1
fi

echo "==> NF-e emitida com sucesso"
CHAVE=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('chaveAcesso','').replace('NFe',''))")
PROTOCOLO=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('protocolo',''))")
echo "    Chave: $CHAVE"
echo "    Protocolo: $PROTOCOLO"

OUT_DIR="$ROOT/out"
mkdir -p "$OUT_DIR"
DANFE_PATH="$OUT_DIR/danfe-${CHAVE}.pdf"
echo "==> Gerando DANFE"
curl -sf "$API/api/nfe/notas/${CHAVE}/danfe" \
  -H "Authorization: Bearer $TOKEN" \
  -o "$DANFE_PATH"
echo "    DANFE salvo em: $DANFE_PATH"
