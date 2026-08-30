# SyncNota — Guia do produto

Portal fiscal **SaaS multiempresa** (NFS-e, NF-e, cadastros, assinatura Stripe).

## O que é

| Público | URL / fluxo |
|---------|-------------|
| Visitante | `/` — landing |
| Novo cliente | `/registrar` → `/onboarding` → `/painel` |
| Cliente | `/login` → `/painel` |
| Admin da conta | `/conta/*`, `/cadastros/usuarios` |
| Admin plataforma | `/auth/admin` + `NFSE_ADMIN_SECRET` |

## Desenvolvimento local

```bash
./subir-portal.sh          # API :8080 + Next dev :3000
./subir-portal.sh --status
./subir-portal.sh --parar
```

Demo (perfil **dev**): `admin@synki.demo` / `demo123`

## Produção

```bash
./deploy-portal.sh
```

Variáveis obrigatórias em `portal-api/.env`:

```bash
SPRING_PROFILES_ACTIVE=prod
NFSE_JWT_SECRET=<32+ chars aleatórios>
NFSE_ADMIN_SECRET=<16+ chars aleatórios>
NFSE_JWT_EXP_MIN=480          # 8h — nunca 0 em prod
CORS_ORIGINS=https://app.seudominio.com.br
DB_URL=jdbc:mysql://...       # useSSL=true em prod real
MAIL_ENABLED=true
MAIL_USER=...
MAIL_PASSWORD=...

# Stripe (cobrança)
STRIPE_ENABLED=true
STRIPE_ENV=test
STRIPE_TEST_SECRET_KEY=sk_test_...
STRIPE_TEST_PRICE_ID=price_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PORTAL_RETURN_URL=https://app.seudominio.com.br/conta/assinatura

# Setup rápido local:
#   ./configurar-stripe.sh sk_test_SUA_CHAVE
#   ./configurar-stripe.sh --webhook-only
#   ./subir-portal.sh --rapido
```

Frontend `portal-web/.env.local`:

```bash
NEXT_PUBLIC_API_URL=https://api.seudominio.com.br
```

## Checklist antes de expor na internet

- [ ] `SPRING_PROFILES_ACTIVE=prod` (desliga seed demo)
- [ ] Segredos JWT/admin únicos e longos
- [ ] `NFSE_JWT_EXP_MIN > 0` + refresh habilitado no frontend
- [ ] HTTPS no reverse proxy (nginx/Caddy)
- [ ] CORS só com domínio do frontend
- [ ] MySQL com backup e SSL
- [ ] Stripe webhook apontando para `/api/billing/stripe/webhook`
- [ ] E-mail SMTP para dunning e DANFSe
- [ ] Certificados A1 por empresa (upload no portal)

## Arquitetura

```
portal-web (Next.js)  →  portal-api (Spring Boot)  →  MySQL
                              ↕ Stripe / BrasilAPI / SEFAZ
```

Documentação técnica: [PORTAL.md](PORTAL.md) · Roadmap SaaS: [SAAS-ROADMAP.md](SAAS-ROADMAP.md)

## CI

GitHub Actions: `.github/workflows/portal.yml` — build API + lint/build frontend em cada push.

## Pendências conhecidas (pós-MVP)

- Reset de senha por e-mail
- MFA
- Docker compose completo (API + web + proxy)
- `@PreAuthorize` em todos os controllers
- Admin key validada server-side antes do shell (hoje só sessionStorage)
