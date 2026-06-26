#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"

# Variáveis da API (certificado, município, JWT): copie portal-api/.env.example → portal-api/.env
if [[ -f "$ROOT/portal-api/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/portal-api/.env"
  set +a
fi

if [[ -x /tmp/jdk-21/bin/javac ]]; then
  export JAVA_HOME=/tmp/jdk-21
elif [[ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
else
  echo "JDK 21 necessario. Rode uma vez:"
  echo "  curl -fsSL 'https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk' -o /tmp/jdk21.tar.gz"
  echo "  tar -xzf /tmp/jdk21.tar.gz -C /tmp && mv /tmp/jdk-21* /tmp/jdk-21"
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> Instalando lib nfse..."
(cd "$ROOT" && mvn -q install -DskipTests)

echo "==> Compilando portal-api..."
(cd "$ROOT/portal-api" && mvn -q compile)

echo "==> Frontend .env.local"
if [[ ! -f "$ROOT/portal-web/.env.local" ]]; then
  cp "$ROOT/portal-web/.env.local.example" "$ROOT/portal-web/.env.local"
fi

echo "==> Empacotando API..."
(cd "$ROOT/portal-api" && mvn -q package -DskipTests)

echo "==> Subindo API :8080 ..."
(java -jar "$ROOT/portal-api/target/nfse-portal-api-0.1.0-SNAPSHOT.jar") &
API_PID=$!

echo "==> Subindo Next.js :3000 ..."
(cd "$ROOT/portal-web" && npm run dev) &
WEB_PID=$!

cleanup() { kill $API_PID $WEB_PID 2>/dev/null || true; }
trap cleanup EXIT

echo "Aguardando API..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

TOKEN=$(curl -sf -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@synki.demo","senha":"demo123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")

echo ""
echo "============================================"
if [[ -n "$TOKEN" ]]; then
  echo "Portal: http://localhost:3000/embed?t=$TOKEN"
else
  echo "Portal: http://localhost:3000/embed  (obtenha token via login)"
fi
echo "API:    http://localhost:8080"
echo "Login:  admin@synki.demo / demo123"
echo ""
echo "Homologacao (padrao): NFSE_AMBIENTE=${NFSE_AMBIENTE:-homologacao}"
echo "Certificado: ${CERTIFICADO_PATH:- (application.yml)}"
echo "Municipio:   ${MUNICIPIO_IBGE:-4310009} ${PREFEITURA:-Ibiruba/RS}"
echo "Doc:         ./PORTAL.md"
echo "Ctrl+C para encerrar"
echo "============================================"
wait
