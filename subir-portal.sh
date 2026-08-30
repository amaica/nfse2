#!/usr/bin/env bash
# Sobe portal NFS-e completo: MySQL (checagem), portal-api :8080, portal-web :3000
#
# Uso:
#   ./subir-portal.sh              # background (padrao)
#   ./subir-portal.sh --fg         # primeiro plano (Ctrl+C encerra)
#   ./subir-portal.sh --rapido     # pula build se JAR ja existir
#   ./subir-portal.sh --parar      # encerra API e frontend
#   ./subir-portal.sh --status     # mostra se esta rodando
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"
PID_API="$LOG_DIR/portal-api.pid"
PID_WEB="$LOG_DIR/portal-web.pid"
LOG_API="$LOG_DIR/portal-api.log"
LOG_WEB="$LOG_DIR/portal-web.log"
JAR="$ROOT/portal-api/target/nfse-portal-api-0.1.0-SNAPSHOT.jar"

API_PORT=8080
WEB_PORT=3000
MODO="bg"
RAPIDO=false

for arg in "$@"; do
  case "$arg" in
    --fg|-f|--foreground) MODO="fg" ;;
    --rapido|-q|--quick) RAPIDO=true ;;
    --parar|--stop|-s) MODO="stop" ;;
    --status) MODO="status" ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
    *)
      echo "Opcao desconhecida: $arg (use --help)"
      exit 1
      ;;
  esac
done

parar() {
  echo "==> Encerrando portal..."
  pkill -f 'nfse-portal-api-0.1.0-SNAPSHOT.jar' 2>/dev/null || true
  pkill -f 'portal-web.*next dev' 2>/dev/null || true
  pkill -f 'node.*next dev' 2>/dev/null || true
  for port in "$API_PORT" "$WEB_PORT"; do
    if fuser -n tcp "$port" >/dev/null 2>&1; then
      fuser -k -n tcp "$port" >/dev/null 2>&1 || true
    fi
  done
  rm -f "$PID_API" "$PID_WEB"
  sleep 1
  echo "Parado."
}

status() {
  local api_ok=0 web_ok=0 web_code="000"
  curl -sf "http://localhost:${API_PORT}/actuator/health" >/dev/null 2>&1 && api_ok=1
  web_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${WEB_PORT}/login" 2>/dev/null || echo "000")
  [[ "$web_code" == "200" ]] && web_ok=1
  echo "API  (:${API_PORT}): $([[ $api_ok -eq 1 ]] && echo UP || echo DOWN)"
  echo "Web  (:${WEB_PORT}): $([[ $web_ok -eq 1 ]] && echo "UP (HTTP $web_code)" || echo "DOWN (HTTP $web_code)")"
  [[ -f "$PID_API" ]] && echo "PID API: $(cat "$PID_API") (log: $LOG_API)"
  [[ -f "$PID_WEB" ]] && echo "PID Web: $(cat "$PID_WEB") (log: $LOG_WEB)"
  exit 0
}

[[ "$MODO" == "stop" ]] && { parar; exit 0; }
[[ "$MODO" == "status" ]] && status

mkdir -p "$LOG_DIR"

# .env da API
if [[ -f "$ROOT/portal-api/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/portal-api/.env"
  set +a
fi
API_PORT="${PORT:-8080}"
unset PORT

# JDK 21
if [[ -x /tmp/jdk-21/bin/javac ]]; then
  export JAVA_HOME=/tmp/jdk-21
elif [[ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
else
  echo "ERRO: JDK 21 necessario."
  echo "  sudo apt install openjdk-21-jdk"
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"

# Node
if ! command -v node >/dev/null 2>&1; then
  echo "ERRO: Node.js nao encontrado (npm install no portal-web)."
  exit 1
fi

# MySQL (aviso se nao responder)
if command -v mysqladmin >/dev/null 2>&1; then
  DB_USER_CHECK="${DB_USER:-root}"
  DB_PASS_CHECK="${DB_PASSWORD:-root}"
  if ! mysqladmin ping -h localhost -u"$DB_USER_CHECK" -p"$DB_PASS_CHECK" --silent 2>/dev/null; then
    echo "AVISO: MySQL nao respondeu. Confira se o servico esta ativo e portal-api/.env"
  fi
fi

# Frontend .env.local
if [[ ! -f "$ROOT/portal-web/.env.local" ]]; then
  echo "NEXT_PUBLIC_API_URL=http://localhost:${API_PORT}" > "$ROOT/portal-web/.env.local"
  echo "==> Criado portal-web/.env.local"
fi

if [[ ! -d "$ROOT/portal-web/node_modules" ]]; then
  echo "==> npm install (portal-web)..."
  (cd "$ROOT/portal-web" && npm install)
fi

if [[ "$RAPIDO" == true && -f "$JAR" ]]; then
  echo "==> Modo rapido: usando JAR existente"
else
  echo "==> Instalando lib nfse..."
  (cd "$ROOT" && mvn -q install -DskipTests)
  echo "==> Empacotando portal-api..."
  (cd "$ROOT/portal-api" && mvn -q package -DskipTests)
fi

parar

echo "==> Subindo API :${API_PORT} ..."
nohup java -jar "$JAR" >"$LOG_API" 2>&1 &
echo $! >"$PID_API"

echo "==> Subindo Next.js :${WEB_PORT} ..."
rm -rf "$ROOT/portal-web/.next" "$ROOT/portal-web/node_modules/.cache"
(
  cd "$ROOT/portal-web"
  PORT="$WEB_PORT" nohup npm run dev >"$LOG_WEB" 2>&1 &
  echo $! >"$PID_WEB"
)

web_http_ok() {
  local code chunk_code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${WEB_PORT}/login" 2>/dev/null || echo "000")
  [[ "$code" != "200" ]] && return 1
  local chunk
  chunk=$(curl -sf "http://localhost:${WEB_PORT}/login" 2>/dev/null \
    | grep -oE '/_next/static/chunks/[^"?]+' | head -1 || true)
  [[ -z "$chunk" ]] && return 1
  chunk_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${WEB_PORT}${chunk}" 2>/dev/null || echo "000")
  [[ "$chunk_code" == "200" ]]
}

echo "Aguardando servicos (ate 90s)..."
api_ok=0
web_ok=0
for i in $(seq 1 45); do
  curl -sf "http://localhost:${API_PORT}/actuator/health" >/dev/null 2>&1 && api_ok=1
  web_http_ok && web_ok=1
  [[ $api_ok -eq 1 && $web_ok -eq 1 ]] && break
  sleep 2
done

if [[ $web_ok -ne 1 ]]; then
  echo ""
  echo "AVISO: frontend com cache quebrado ou ainda compilando."
  echo "  Tente: rm -rf portal-web/.next portal-web/node_modules/.cache && ./subir-portal.sh"
  echo "  Log: tail -50 $LOG_WEB"
fi

TOKEN=""
TOKEN=$(curl -sf -X POST "http://localhost:${API_PORT}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@synki.demo","senha":"demo123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || true)

echo ""
echo "============================================"
echo "  Portal NFS-e — rodando"
echo "============================================"
echo "  Site:      http://localhost:${WEB_PORT}/"
echo "  Login:     http://localhost:${WEB_PORT}/login"
echo "  Painel:    http://localhost:${WEB_PORT}/painel"
echo "  NFS-e:     http://localhost:${WEB_PORT}/nfse/emissao"
echo "  Embed:     http://localhost:${WEB_PORT}/embed"
echo "  Admin:     http://localhost:${WEB_PORT}/admin"
echo "  API:       http://localhost:${API_PORT}"
echo ""
echo "  Demo:      admin@synki.demo / demo123"
echo "  MAICA:     CNPJ 57514533000109 / Maica@2026"
echo ""
if [[ -n "$TOKEN" ]]; then
  echo "  Embed demo: http://localhost:${WEB_PORT}/embed?t=${TOKEN}"
fi
echo ""
echo "  Logs:  tail -f $LOG_API"
echo "         tail -f $LOG_WEB"
echo "  Parar: ./subir-portal.sh --parar"
echo "============================================"

if [[ "$MODO" == "fg" ]]; then
  echo "Modo primeiro plano — Ctrl+C encerra."
  trap 'parar; exit 0' INT TERM
  tail -f "$LOG_API" "$LOG_WEB"
else
  echo "Rodando em background."
fi
