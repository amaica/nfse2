#!/usr/bin/env bash
# Teste ponta a ponta: login → contexto → emissão NFS-e homologação (nova arquitetura portal-api).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT/portal-api/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/portal-api/.env"
  set +a
fi

API="${API_URL:-http://localhost:8080}"
CNPJ_LOGIN="${PORTAL_CNPJ:-06866960000115}"
SENHA_LOGIN="${PORTAL_SENHA:-demo123}"
COMPETENCIA="$(date +%Y-%m-%d)"
DATA_EMISSAO="$(date -Iseconds | cut -c1-16)"

echo "==> API: $API"
echo "==> Login CNPJ: $CNPJ_LOGIN"

for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$API/api/auth/login" -X POST \
    -H 'Content-Type: application/json' -d '{"senha":"x"}' 2>/dev/null || echo "000")
  if [[ "$code" =~ ^[245] ]]; then
    echo "==> API disponível (HTTP $code)"
    break
  fi
  if [[ $i -eq 40 ]]; then
    echo "API indisponível em $API — rode ./start-portal.sh"
    exit 1
  fi
  sleep 2
done

LOGIN_JSON=$(curl -sf -X POST "$API/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"cnpj\":\"$CNPJ_LOGIN\",\"senha\":\"$SENHA_LOGIN\"}")

TOKEN=$(echo "$LOGIN_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
EMPRESA=$(echo "$LOGIN_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('empresaNome','?'), d.get('empresaId','?'))")
echo "==> Autenticado: $EMPRESA"

CTX=$(curl -sf "$API/api/nfse/emissao/contexto" -H "Authorization: Bearer $TOKEN")
echo "==> Contexto:"
echo "$CTX" | python3 -m json.tool

PODE=$(echo "$CTX" | python3 -c "import sys,json; print(json.load(sys.stdin).get('podeEmitir', False))")
if [[ "$PODE" != "True" ]]; then
  echo "ERRO: podeEmitir=false — cadastre certificado e-CNPJ A1 para a empresa."
  exit 1
fi

IBGE=$(echo "$CTX" | python3 -c "import sys,json; print(json.load(sys.stdin)['codigoMunicipioIbge'])")
SERVICO=$(echo "$CTX" | python3 -c "import sys,json; print(json.load(sys.stdin).get('codigoServicoPadrao','04.02.01.000'))")
DESCR=$(echo "$CTX" | python3 -c "import sys,json; print(json.load(sys.stdin).get('descricaoServicoPadrao','Servico de analises laboratoriais'))")
ALIQ=$(echo "$CTX" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('aliquotaPadraoPercentual') or 2)")

PAYLOAD=$(python3 - <<PY
import json
from decimal import Decimal, ROUND_HALF_UP

def moeda(v):
    return format(Decimal(str(v)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP), "f")

ibge = "$IBGE"
serv = "$SERVICO"
descr = """$DESCR"""
aliquota = float("$ALIQ")
valor = Decimal("150.00")
iss = (valor * Decimal(str(aliquota)) / Decimal("100")).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
print(json.dumps({
  "identificacao": {
    "serieRps": "1",
    "tipoRps": "1",
    "dataEmissao": "$DATA_EMISSAO",
    "competencia": "$COMPETENCIA"
  },
  "regime": {
    "tributacaoIssqn": "1",
    "regimeEspecialTributacao": "0",
    "simplesNacional": "1",
    "issRetido": "1",
    "incentivoFiscal": False
  },
  "prestador": { "inscricaoMunicipal": "" },
  "tomador": {
    "documento": "00000000000191",
    "razaoSocial": "TOMADOR TESTE HOMOLOGACAO",
    "email": "tomador@teste.local"
  },
  "enderecoTomador": {
    "cep": "98200000",
    "logradouro": "Rua Coronel Chicuta",
    "numero": "100",
    "bairro": "Centro",
    "cidade": "Ibiruba",
    "uf": "RS",
    "codigoMunicipioIbge": ibge
  },
  "servico": {
    "itemListaServico": "04.02.01.000",
    "descricaoServico": "Analises clinicas - teste homologacao portal",
    "municipioPrestacao": ibge,
    "localPrestacao": ibge,
    "nbs": "114061100"
  },
  "valores": {
    "valorServicos": moeda(valor),
    "deducoes": moeda(0),
    "descontoIncondicionado": moeda(0),
    "descontoCondicionado": moeda(0),
    "baseCalculo": moeda(valor),
    "aliquota": aliquota,
    "valorIss": moeda(iss),
    "valorLiquidoNfse": moeda(valor - iss),
    "responsavelRetencaoIss": "1"
  },
  "tributacaoFederal": {
    "cstPisCofins": "08",
    "tipoRetencaoPisCofins": "0",
    "aliquotaPis": moeda(0),
    "aliquotaCofins": moeda(0),
    "valorPis": moeda(0),
    "valorCofins": moeda(0),
    "retencaoIrrf": moeda(0),
    "retencaoCsll": moeda(0),
    "retencaoIss": moeda(0),
    "retencaoInss": moeda(0),
    "habilitarRetencoes": False
  },
  "informacoesAdicionais": {
    "observacoes": "Teste homologacao portal-api"
  }
}))
PY
)

echo "==> Emitindo NFS-e (homologação)..."
echo "$PAYLOAD" | python3 -m json.tool | head -40
echo "..."

RESP=$(curl -s -w "\nHTTP:%{http_code}" -X POST "$API/api/nfse/emitir" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "$PAYLOAD") || true

HTTP=$(echo "$RESP" | tail -1 | cut -d: -f2)
BODY=$(echo "$RESP" | sed '$d')

if [[ "$HTTP" == "200" ]]; then
  echo ""
  echo "=== EMISSÃO OK (homologação) ==="
  echo "$BODY" | python3 -m json.tool
  CHAVE=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('chaveAcesso',''))")
  if [[ -n "$CHAVE" ]]; then
    echo ""
    echo "Chave: $CHAVE"
    echo "Consulta: curl -s $API/api/nfse/consulta/$CHAVE -H \"Authorization: Bearer \$TOKEN\""
  fi
  exit 0
fi

echo ""
echo "=== EMISSÃO FALHOU (HTTP $HTTP) ==="
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
exit 1
