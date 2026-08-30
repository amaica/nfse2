#!/usr/bin/env bash
# Configura Stripe (modo test ou live) para o SyncNota.
#
# Uso:
#   ./configurar-stripe.sh sk_test_...              # cria produto/preço e atualiza .env
#   ./configurar-stripe.sh --live sk_live_...       # produção (STRIPE_ENV=live)
#   ./configurar-stripe.sh --webhook-only           # só inicia stripe listen (dev local)
#
# Requer: curl, jq (opcional mas recomendado)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$ROOT/portal-api/.env"
STRIPE_BIN="${STRIPE_BIN:-$ROOT/stripe}"
API_PORT="${PORT:-8080}"
WEBHOOK_PATH="/api/billing/stripe/webhook"
PID_FILE="$ROOT/logs/stripe-listen.pid"
LOG_FILE="$ROOT/logs/stripe-listen.log"

MODO="test"
ACAO="setup"
SECRET_KEY=""

for arg in "$@"; do
  case "$arg" in
    --live) MODO="live" ;;
    --webhook-only) ACAO="webhook" ;;
    --help|-h)
      sed -n '2,10p' "$0"
      exit 0
      ;;
    sk_test_*|sk_live_*)
      SECRET_KEY="$arg"
      [[ "$arg" == sk_live_* ]] && MODO="live"
      ;;
    *)
      echo "Argumento desconhecido: $arg"
      exit 1
      ;;
  esac
done

mkdir -p "$ROOT/logs"

if [[ -z "$SECRET_KEY" && -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  if [[ "$MODO" == "live" ]]; then
    SECRET_KEY="${STRIPE_LIVE_SECRET_KEY:-}"
  else
    SECRET_KEY="${STRIPE_TEST_SECRET_KEY:-}"
  fi
fi

patch_env() {
  local key="$1"
  local value="$2"
  if [[ ! -f "$ENV_FILE" ]]; then
    cp "$ROOT/portal-api/.env.example" "$ENV_FILE"
  fi
  if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$ENV_FILE"
  else
    echo "${key}=${value}" >> "$ENV_FILE"
  fi
}

stripe_api() {
  local method="$1"
  local path="$2"
  shift 2
  curl -sf -X "$method" "https://api.stripe.com/v1${path}" \
    -u "${SECRET_KEY}:" \
    "$@"
}

json_field() {
  local json="$1"
  local field="$2"
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq -r "$field"
  else
    echo "$json" | grep -o "\"${field#*.}\": *\"[^\"]*\"" | head -1 | sed 's/.*"\([^"]*\)"$/\1/'
  fi
}

criar_produto_preco() {
  echo "==> Criando produto SyncNota Starter ($MODO)..."
  local product_json
  product_json=$(stripe_api POST /products \
    -d "name=SyncNota Starter" \
    -d "description=Pacote mensal: +1 emitente, +100 NFS-e/mês, +50 NF-e/mês, +5 usuários. Trial 14 dias." \
    -d "metadata[app]=syncnota")

  local product_id
  product_id=$(json_field "$product_json" ".id")
  if [[ -z "$product_id" || "$product_id" == "null" ]]; then
    echo "Erro ao criar produto Stripe:"
    echo "$product_json"
    exit 1
  fi
  echo "    Produto: $product_id"

  echo "==> Criando preço R\$ 97/mês (BRL)..."
  local price_json
  price_json=$(stripe_api POST /prices \
    -d "product=${product_id}" \
    -d "unit_amount=9700" \
    -d "currency=brl" \
    -d "recurring[interval]=month" \
    -d "metadata[pacote]=starter")

  local price_id
  price_id=$(json_field "$price_json" ".id")
  if [[ -z "$price_id" || "$price_id" == "null" ]]; then
    echo "Erro ao criar preço:"
    echo "$price_json"
    exit 1
  fi
  echo "    Preço: $price_id"

  patch_env "STRIPE_ENABLED" "true"
  patch_env "STRIPE_ENV" "$MODO"
  if [[ "$MODO" == "live" ]]; then
    patch_env "STRIPE_LIVE_SECRET_KEY" "$SECRET_KEY"
    patch_env "STRIPE_LIVE_PRICE_ID" "$price_id"
  else
    patch_env "STRIPE_TEST_SECRET_KEY" "$SECRET_KEY"
    patch_env "STRIPE_TEST_PRICE_ID" "$price_id"
  fi
  patch_env "STRIPE_PORTAL_RETURN_URL" "${STRIPE_PORTAL_RETURN_URL:-http://localhost:3000/conta/assinatura}"

  echo ""
  echo "============================================"
  echo "  Stripe configurado ($MODO)"
  echo "============================================"
  echo "  STRIPE_ENABLED=true"
  echo "  Price ID: $price_id"
  echo ""
  echo "  Próximo passo (dev local):"
  echo "    ./configurar-stripe.sh --webhook-only"
  echo ""
  echo "  Depois reinicie:"
  echo "    ./subir-portal.sh --rapido"
  echo "============================================"
}

iniciar_webhook() {
  if [[ ! -x "$STRIPE_BIN" ]]; then
    echo "Stripe CLI não encontrada em $STRIPE_BIN"
    echo "Baixe: https://stripe.com/docs/stripe-cli"
    exit 1
  fi

  if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Stripe listen já rodando (PID $(cat "$PID_FILE"))."
    echo "Log: $LOG_FILE"
    exit 0
  fi

  if [[ -z "$SECRET_KEY" ]]; then
    echo "Defina STRIPE_TEST_SECRET_KEY no .env ou passe sk_test_... como argumento."
    exit 1
  fi

  echo "==> Iniciando stripe listen → localhost:${API_PORT}${WEBHOOK_PATH}"
  echo "    (aguarde alguns segundos para obter whsec_...)"

  nohup "$STRIPE_BIN" listen \
    --api-key "$SECRET_KEY" \
    --forward-to "localhost:${API_PORT}${WEBHOOK_PATH}" \
    --events invoice.payment_succeeded,invoice.payment_failed,customer.subscription.updated,customer.subscription.deleted \
    >"$LOG_FILE" 2>&1 &
  echo $! >"$PID_FILE"
  sleep 3

  local whsec=""
  for _ in $(seq 1 15); do
    whsec=$(grep -o 'whsec_[a-zA-Z0-9]*' "$LOG_FILE" 2>/dev/null | head -1 || true)
    [[ -n "$whsec" ]] && break
    sleep 1
  done

  if [[ -n "$whsec" ]]; then
    patch_env "STRIPE_WEBHOOK_SECRET" "$whsec"
    echo "    Webhook secret gravado em portal-api/.env"
    echo "    $whsec"
  else
    echo "    Não foi possível ler whsec_ automaticamente."
    echo "    Veja: tail -f $LOG_FILE"
    echo "    Copie whsec_... para STRIPE_WEBHOOK_SECRET no .env"
  fi

  echo ""
  echo "Stripe listen em background. Parar: kill \$(cat $PID_FILE)"
}

case "$ACAO" in
  setup)
    if [[ -z "$SECRET_KEY" ]]; then
      echo "Uso: ./configurar-stripe.sh sk_test_SUA_CHAVE"
      echo ""
      echo "Obtenha em: https://dashboard.stripe.com/test/apikeys"
      exit 1
    fi
    criar_produto_preco
    ;;
  webhook)
    iniciar_webhook
    ;;
esac
