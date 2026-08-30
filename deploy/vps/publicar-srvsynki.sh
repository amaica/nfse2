#!/usr/bin/env bash
# Publica AgroNota na srvsynki (Traefik já serve *.synkicrm.com.br).
# DNS de agrownota.synkicrm.com.br deve apontar para 72.60.245.119
#
# Uso:
#   ./deploy/vps/publicar-srvsynki.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VPS_HOST="${VPS_HOST:-root@100.73.214.45}"
VPS_PASS="${VPS_PASS:?Defina VPS_PASS no ambiente}"
DOMAIN="${DOMAIN:-agrownota.synkicrm.com.br}"
REMOTE_DIR="/opt/agrownota"
# Portas livres na srvsynki (3000/8080 já ocupadas)
API_PORT="${API_PORT:-8088}"
WEB_PORT="${WEB_PORT:-3010}"
JAR="$ROOT/portal-api/target/nfse-portal-api-0.1.0-SNAPSHOT.jar"

ssh_cmd() {
  SSHPASS="$VPS_PASS" sshpass -e ssh -o StrictHostKeyChecking=no "$VPS_HOST" "$@"
}
rsync_cmd() {
  SSHPASS="$VPS_PASS" sshpass -e rsync -az --delete -e "ssh -o StrictHostKeyChecking=no" "$@"
}

echo "==> Build local..."
if [[ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
elif [[ -x /usr/lib/jvm/java-17-openjdk-amd64/bin/javac ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
fi
export PATH="${JAVA_HOME:-}/bin:$PATH"
(cd "$ROOT" && mvn -q install -DskipTests)
(cd "$ROOT/portal-api" && mvn -q package -DskipTests)
[[ -f "$JAR" ]] || { echo "JAR não encontrado: $JAR"; exit 1; }

JWT_SECRET=$(openssl rand -hex 32)
ADMIN_SECRET=$(openssl rand -hex 16)
DB_PASS=$(openssl rand -hex 16)
STRIPE_ENABLED=false
STRIPE_ENV=test
STRIPE_TEST_SECRET_KEY=""
STRIPE_TEST_PRICE_ID=""
STRIPE_WEBHOOK_SECRET=""
MAIL_ENABLED=false
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=""
MAIL_PASSWORD=""
MAIL_FROM=""
MAIL_FROM_NAME=AgroNota

if ssh_cmd "test -f ${REMOTE_DIR}/portal-api/.env" 2>/dev/null; then
  echo "==> Redeploy: preservando segredos existentes..."
  REMOTE_ENV_FILE=$(ssh_cmd "cat ${REMOTE_DIR}/portal-api/.env")
  extract_env() { echo "$REMOTE_ENV_FILE" | grep -E "^${1}=" | head -1 | cut -d= -f2- || true; }
  _v=$(extract_env DB_PASSWORD); [[ -n "$_v" ]] && DB_PASS="$_v"
  _v=$(extract_env NFSE_JWT_SECRET); [[ -n "$_v" ]] && JWT_SECRET="$_v"
  _v=$(extract_env NFSE_ADMIN_SECRET); [[ -n "$_v" ]] && ADMIN_SECRET="$_v"
  _v=$(extract_env STRIPE_ENABLED); [[ -n "$_v" ]] && STRIPE_ENABLED="$_v"
  _v=$(extract_env STRIPE_ENV); [[ -n "$_v" ]] && STRIPE_ENV="$_v"
  _v=$(extract_env STRIPE_TEST_SECRET_KEY); [[ -n "$_v" ]] && STRIPE_TEST_SECRET_KEY="$_v"
  _v=$(extract_env STRIPE_TEST_PRICE_ID); [[ -n "$_v" ]] && STRIPE_TEST_PRICE_ID="$_v"
  _v=$(extract_env STRIPE_WEBHOOK_SECRET); [[ -n "$_v" ]] && STRIPE_WEBHOOK_SECRET="$_v"
  _v=$(extract_env MAIL_ENABLED); [[ -n "$_v" ]] && MAIL_ENABLED="$_v"
  _v=$(extract_env MAIL_HOST); [[ -n "$_v" ]] && MAIL_HOST="$_v"
  _v=$(extract_env MAIL_PORT); [[ -n "$_v" ]] && MAIL_PORT="$_v"
  _v=$(extract_env MAIL_USER); [[ -n "$_v" ]] && MAIL_USER="$_v"
  _v=$(extract_env MAIL_PASSWORD); [[ -n "$_v" ]] && MAIL_PASSWORD="$_v"
  _v=$(extract_env MAIL_FROM); [[ -n "$_v" ]] && MAIL_FROM="$_v"
  _v=$(extract_env MAIL_FROM_NAME); [[ -n "$_v" ]] && MAIL_FROM_NAME="$_v"
fi
if [[ -f "$ROOT/portal-api/.env" ]]; then
  # shellcheck disable=SC1091
  set -a; source "$ROOT/portal-api/.env"; set +a
fi

echo "==> Preparando srvsynki..."
ssh_cmd bash -s <<REMOTE_PREP
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq curl ca-certificates gnupg rsync wget apt-transport-https mariadb-server

if ! /usr/lib/jvm/temurin-21-jre-amd64/bin/java -version 2>&1 | grep -q '21'; then
  install -d -m 0755 /etc/apt/keyrings
  wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  . /etc/os-release
  echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb \${VERSION_CODENAME:-bookworm} main" > /etc/apt/sources.list.d/adoptium.list
  apt-get update -qq
  apt-get install -y -qq temurin-21-jre
fi

if ! command -v node >/dev/null || [[ \$(node -v | cut -d. -f1 | tr -d v) -lt 20 ]]; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y -qq nodejs
fi

systemctl enable mariadb
systemctl start mariadb

id agrownota &>/dev/null || useradd -r -m -d /opt/agrownota -s /usr/sbin/nologin agrownota
mkdir -p /opt/agrownota/portal-api /opt/agrownota/portal-web /opt/agrownota/deploy /var/log/agrownota
chown -R agrownota:agrownota /opt/agrownota /var/log/agrownota

mysql -e "CREATE DATABASE IF NOT EXISTS nfse_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "CREATE USER IF NOT EXISTS 'nfse'@'localhost' IDENTIFIED BY '${DB_PASS}';" || true
mysql -e "ALTER USER 'nfse'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "GRANT ALL PRIVILEGES ON nfse_portal.* TO 'nfse'@'localhost'; FLUSH PRIVILEGES;"
REMOTE_PREP

echo "==> Enviando artefatos..."
rsync_cmd "$JAR" "$VPS_HOST:$REMOTE_DIR/portal-api/"
rsync_cmd --exclude node_modules --exclude .next/cache "$ROOT/portal-web/" "$VPS_HOST:$REMOTE_DIR/portal-web/"
rsync_cmd "$ROOT/deploy/vps/" "$VPS_HOST:$REMOTE_DIR/deploy/"

echo "==> Ajustando ownership..."
ssh_cmd "chown -R agrownota:agrownota /opt/agrownota /var/log/agrownota"

echo "==> .env produção..."
ssh_cmd bash -s <<REMOTE_ENV
set -euo pipefail
cat > /opt/agrownota/portal-api/.env <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://127.0.0.1:3306/nfse_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo
DB_USER=nfse
DB_PASSWORD=${DB_PASS}
NFSE_JWT_SECRET=${JWT_SECRET}
NFSE_JWT_EXP_MIN=480
NFSE_ADMIN_SECRET=${ADMIN_SECRET}
NFSE_EMBED_BASE_URL=https://${DOMAIN}
CORS_ORIGINS=https://${DOMAIN}
PORT=${API_PORT}
SERVER_PORT=${API_PORT}
MAIL_ENABLED=${MAIL_ENABLED}
MAIL_HOST=${MAIL_HOST}
MAIL_PORT=${MAIL_PORT}
MAIL_USER=${MAIL_USER}
MAIL_PASSWORD=${MAIL_PASSWORD}
MAIL_FROM=${MAIL_FROM:-${MAIL_USER}}
MAIL_FROM_NAME=${MAIL_FROM_NAME}
STRIPE_ENABLED=${STRIPE_ENABLED}
STRIPE_ENV=${STRIPE_ENV}
STRIPE_TEST_SECRET_KEY=${STRIPE_TEST_SECRET_KEY}
STRIPE_TEST_PRICE_ID=${STRIPE_TEST_PRICE_ID}
STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
STRIPE_PORTAL_RETURN_URL=https://${DOMAIN}/conta/assinatura
NFSE_PDF_RETRY_ENABLED=true
NFE_DISTRIBUICAO_ENABLED=true
NFE_DISTRIBUICAO_CRON='0 20 * * * *'
FLUXO_IMPORT_ENABLED=false
EOF
chmod 600 /opt/agrownota/portal-api/.env
chown agrownota:agrownota /opt/agrownota/portal-api/.env

cat > /opt/agrownota/portal-web/.env.local <<EOF
NEXT_PUBLIC_API_URL=https://${DOMAIN}
PORT=${WEB_PORT}
EOF
chmod 600 /opt/agrownota/portal-web/.env.local
chown agrownota:agrownota /opt/agrownota/portal-web/.env.local
REMOTE_ENV

echo "==> npm ci + build portal-web..."
ssh_cmd bash -s <<REMOTE_NPM
set -euo pipefail
cd /opt/agrownota/portal-web
echo -e "NEXT_PUBLIC_API_URL=https://${DOMAIN}\nPORT=${WEB_PORT}" > .env.local
chown agrownota:agrownota .env.local
sudo -u agrownota npm ci
sudo -u agrownota npm run build
REMOTE_NPM

echo "==> systemd + Traefik stack..."
ssh_cmd bash -s <<REMOTE_SVC
set -euo pipefail
cp /opt/agrownota/deploy/agrownota-api.service /etc/systemd/system/
cp /opt/agrownota/deploy/agrownota-web.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable agrownota-api agrownota-web
systemctl restart agrownota-api
sleep 20
systemctl restart agrownota-web
cd /opt/agrownota/deploy
docker stack deploy -c docker-compose.agrownota.yml agrownota
sleep 5
systemctl --no-pager --full status agrownota-api | head -15
systemctl --no-pager --full status agrownota-web | head -15
curl -sS -o /dev/null -w "API_LOCAL=%{http_code}\\n" http://127.0.0.1:${API_PORT}/actuator/health || true
curl -sS -o /dev/null -w "WEB_LOCAL=%{http_code}\\n" http://127.0.0.1:${WEB_PORT}/ || true
docker service ls | grep agrownota || true
REMOTE_SVC

echo ""
echo "============================================"
echo "  AgroNota na srvsynki"
echo "  URL alvo: https://${DOMAIN}/"
echo "  IP Traefik: 72.60.245.119"
echo "  Aponte o DNS A de ${DOMAIN} → 72.60.245.119"
echo "============================================"
