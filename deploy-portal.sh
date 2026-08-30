#!/usr/bin/env bash
# Deploy produção: build JAR + Next.js standalone start
#
# Uso:
#   ./deploy-portal.sh              # build + sobe em background
#   ./deploy-portal.sh --build-only # só compila
#   ./deploy-portal.sh --parar
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"
PID_API="$LOG_DIR/portal-api.pid"
PID_WEB="$LOG_DIR/portal-web.pid"
LOG_API="$LOG_DIR/portal-api.log"
LOG_WEB="$LOG_DIR/portal-web.log"
JAR="$ROOT/portal-api/target/nfse-portal-api-0.1.0-SNAPSHOT.jar"

API_PORT="${PORT:-8080}"
WEB_PORT="${WEB_PORT:-3000}"
MODO="deploy"

for arg in "$@"; do
  case "$arg" in
    --build-only) MODO="build" ;;
    --parar|--stop) MODO="stop" ;;
    -h|--help)
      sed -n '2,8p' "$0"
      exit 0
      ;;
  esac
done

parar() {
  echo "==> Encerrando serviços..."
  pkill -f 'nfse-portal-api-0.1.0-SNAPSHOT.jar' 2>/dev/null || true
  pkill -f 'next start' 2>/dev/null || true
  rm -f "$PID_API" "$PID_WEB"
  echo "Parado."
}

[[ "$MODO" == "stop" ]] && { parar; exit 0; }

mkdir -p "$LOG_DIR"

if [[ -f "$ROOT/portal-api/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/portal-api/.env"
  set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
API_PORT="${PORT:-8080}"

if [[ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
elif [[ -x /tmp/jdk-21/bin/javac ]]; then
  export JAVA_HOME=/tmp/jdk-21
fi
export PATH="${JAVA_HOME:-}/bin:$PATH"

echo "==> Build lib nfse..."
(cd "$ROOT" && mvn -q install -DskipTests)

echo "==> Build portal-api..."
(cd "$ROOT/portal-api" && mvn -q package -DskipTests)

echo "==> Build portal-web..."
(cd "$ROOT/portal-web" && npm ci && npm run build)

[[ "$MODO" == "build" ]] && { echo "Build concluído."; exit 0; }

parar

echo "==> Subindo API (profile=$SPRING_PROFILES_ACTIVE) :${API_PORT}..."
nohup java -jar "$JAR" --spring.profiles.active="$SPRING_PROFILES_ACTIVE" >"$LOG_API" 2>&1 &
echo $! >"$PID_API"

echo "==> Subindo Next.js produção :${WEB_PORT}..."
(
  cd "$ROOT/portal-web"
  PORT="$WEB_PORT" nohup npm run start >"$LOG_WEB" 2>&1 &
  echo $! >"$PID_WEB"
)

echo ""
echo "============================================"
echo "  SyncNota — produção"
echo "============================================"
echo "  Site:   http://localhost:${WEB_PORT}/"
echo "  Painel: http://localhost:${WEB_PORT}/painel"
echo "  API:    http://localhost:${API_PORT}"
echo "  Logs:   tail -f $LOG_API $LOG_WEB"
echo "  Parar:  ./deploy-portal.sh --parar"
echo "============================================"
