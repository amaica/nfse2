# SyncNota — Roadmap SaaS Multiempresa

Documento de arquitetura para evoluir o **nfse2** de portal fiscal multi-emitente para **SaaS multiempresa, multiemitente, multiusuário com gerenciador de assinaturas**.

Referência de padrões já validados em produção interna:  
`/home/aurelio/FONTES/NEXTJS/nuxt/original/curso_multiatendimento-master`

---

## 1. Resumo executivo

| Hoje (nfse2) | Alvo (SaaS) |
|--------------|-------------|
| Console de operador com troca livre de empresa | Conta isolada com empresas e usuários vinculados |
| 1 usuário → 1 empresa | 1 usuário → N empresas (com papéis) |
| Provisionamento via `NFSE_ADMIN_SECRET` | Self-service + admin de plataforma |
| Sem billing | Assinatura Stripe + cotas por plano |
| Token HMAC permanente | JWT com expiração + refresh |
| `perfil` sem enforcement | RBAC em API e UI |

**Prioridade zero:** corrigir isolamento de tenant antes de cobrar assinatura.

---

## 2. Estado atual do nfse2

### O que já funciona (base sólida)

| Módulo | Implementação | Arquivos-chave |
|--------|---------------|----------------|
| Multi-emitente | `empresa`, certificado, configs NFS-e/NFe/NFCe | `portal-api/.../domain/Empresa.java`, migrations V1–V16 |
| Multi-endereço emitente | `empresa_endereco` | `V10__empresa_endereco.sql` |
| Usuários por empresa | `usuario.empresa_id`, `perfil` | `Usuario.java`, `V2__usuario.sql` |
| Troca de empresa | Search + reemissão de token | `AuthEmpresaService.java`, `EmpresaSwitcher.tsx` |
| Emissão fiscal isolada | `session.empresaId()` nos services | `NfeController`, `NfseController`, cadastros |
| Embed iframe | Token na URL / auto-login | `portal-web/src/app/embed/` |
| UI login/shell | Glass login agro + sidebar fiscal | `LoginShell.tsx`, `fiscal-layout.css` |
| Admin ops | `X-Admin-Key` | `AdminAuthFilter`, `EmpresaAdminController` |

### Lacunas críticas (bloqueadores SaaS)

1. **Sem tenant (`conta`)** — empresas são irmãs no mesmo banco, sem dono.
2. **Troca de empresa sem membership** — qualquer usuário autenticado pode ir para qualquer `empresa` ativa (`AuthEmpresaService`).
3. **`/api/empresas/**` sem escopo** — CRUD global de empresas com Bearer comum.
4. **Email único global** — impede mesmo humano em várias contas/empresas.
5. **RBAC inexistente** — todos têm `ROLE_EMBED`; `perfil` não é checado.
6. **Sem assinatura** — nenhuma tabela de plano, quota ou gateway.
7. **Token sem expiração por padrão** — `NFSE_JWT_EXP_MIN=0`.

---

## 3. O que aproveitar do curso_multiatendimento

Projeto: **Nuxt 4 monolith + Supabase + Stripe**. Não copiar stack; copiar **padrões**.

### 3.1 Modelo de tenancy (ALTA prioridade)

```
multiatendimento                    →  nfse2 proposto
─────────────────────────────────────────────────────────
workspace (tenant container)        →  conta
workspace.user_id (owner)           →  conta.owner_usuario_id
atendentes (membership junction)    →  usuario_empresa
canais (recurso dentro do tenant)   →  empresa (emitente)
workspace_id em todas as tabelas    →  conta_id + empresa_id
```

**Arquivos de referência:**

- Schema: `curso_multiatendimento-master/supabase/schema.sql`
- Guard servidor: `server/utils/checkWorkspace.ts`
- Listagem: `server/api/workspaces/index.get.ts`
- Picker UI: `app/pages/index.vue` + `WorkspaceCard.vue`

**Adaptação nfse2:** `empresa` continua sendo o emitente fiscal (CNPJ, cert A1). `conta` agrupa várias empresas sob um assinante.

### 3.2 Membership e papéis (ALTA)

Tabela `atendentes`:

```sql
workspace_id, admin_user_id, atendente_user_id
```

- **Owner** = `workspace.user_id` / `admin_user_id`
- **Membro** = qualquer `atendente_user_id` na junction

**nfse2 proposto — `usuario_empresa`:**

```sql
usuario_id, empresa_id, conta_id, papel  -- OWNER | ADMIN | OPERADOR | VISUALIZADOR
```

Enforcement Spring (espelho de `checkWorkspace`):

```java
// Antes de qualquer ação em empresaId
membershipService.requireAccess(usuarioId, empresaId, papelMinimo);
```

### 3.3 Assinaturas e cotas (ALTA)

Tabela `profiles` no multiatendimento:

| Campo | Uso |
|-------|-----|
| `customer` | Stripe customer ID |
| `subscription_id` | ID da assinatura |
| `status_assinatura` | `trial`, `pendente`, `ativa`, `vencida`, `cancelada` |
| `canais` | Quota (0 = ilimitado dev) |
| `data_expiracao` | Fim do período |
| `profiles_view.canais_criados` | Uso atual (view) |

**Gate antes de ações** — `server/utils/checkSubscription.ts`:

1. Resolve o admin da conta via `atendentes`
2. Lê assinatura do admin (não do operador)
3. Bloqueia se `pendente` / `cancelada` ou quota estourada

**nfse2 — cotas fiscais sugeridas:**

| Recurso | Equivalente a `canais` |
|---------|-------------------------|
| Empresas emitentes | `empresas_max` |
| Usuários | `usuarios_max` |
| NFS-e / mês | `nfse_mes_max` |
| NF-e / mês | `nfe_mes_max` |
| Certificados A1 | `certificados_max` |

**Arquivos Stripe a portar para Spring:**

- `server/api/stripe/checkout.post.ts`
- `server/api/stripe/portal.post.ts`
- `server/api/stripe/webhook.post.ts`
- UI: `app/pages/assinatura.vue`, `AssinaturaCard.vue`

Lógica de pacotes: `canais = pacotes × 5` → nfse2 pode usar `emitentes = pacotes × 1` ou tabela `plano` formal.

### 3.4 Padrão de API handler (ALTA)

Sequência repetida em ~30 rotas Nitro:

```
1. Autenticar usuário          →  EmbedAuthFilter / JWT
2. Validar membership          →  MembershipService
3. Validar assinatura (se ação paga) →  SubscriptionService
4. Executar com empresaId da sessão (nunca confiar no body)
```

### 3.5 UI já portada / a completar (MÉDIA)

| Componente multiatendimento | nfse2 | Status |
|----------------------------|-------|--------|
| `login.vue` + `LoginLeftSection` | `LoginShell.tsx` | ✅ Feito (tema agro) |
| `LoginForm.vue` (tabs login/signup) | `login/page.tsx` | ⚠️ Falta signup self-service |
| `workspace.vue` (sidebar colapsável) | `AppShell` + `fiscal-layout.css` | ⚠️ Parcial |
| `index.vue` (grid de workspaces) | `EmpresaSwitcher` (dropdown) | ⚠️ Considerar grid de contas |
| `ProfileDropdown` → assinatura | — | ❌ Criar `/conta/assinatura` |
| `AssinaturaCard` | — | ❌ Portar para React |

### 3.6 Embed ERP (MÉDIA)

`app/lib/embedPostMessage.ts`:

```ts
source: 'synki-atendimento'
events: WHATSAPP_CONECTADO, CONVERSA_ABERTA, ...
```

**nfse2:** padronizar `source: 'synki-fiscal'` com eventos `NFE_AUTORIZADA`, `NFSE_EMITIDA`, `EMPRESA_TROCADA`, etc.  
Campo `workspace.embed_cnpj` (schema existe, código não usa) → mapear para lookup `empresa` por CNPJ no embed.

### 3.7 O que NÃO portar (BAIXA / N/A)

- Supabase Auth, service role, RLS
- WhatsApp, canais, conversas, Pusher, B2, UAZAPI
- Kanban funil, auto-respostas, insights de contato
- Pinia stores (adaptar para React hooks / context)
- Cor primária `#00DC81` (nfse2 usa paleta agro `#3d6b2f`)

---

## 4. Modelo de dados proposto (nfse2)

### Novas tabelas (Flyway V17+)

```sql
-- Conta SaaS (tenant comercial)
CREATE TABLE conta (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome            VARCHAR(255) NOT NULL,
  owner_usuario_id BIGINT NOT NULL,
  stripe_customer_id VARCHAR(64),
  status          VARCHAR(32) NOT NULL DEFAULT 'trial',  -- trial, ativa, suspensa, cancelada
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL
);

-- Vínculo empresa ↔ conta (N emitentes por conta)
CREATE TABLE conta_empresa (
  conta_id    BIGINT NOT NULL,
  empresa_id  BIGINT NOT NULL,
  PRIMARY KEY (conta_id, empresa_id)
);

-- Membership usuário ↔ empresa (substitui 1:1 rígido)
CREATE TABLE usuario_empresa (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  usuario_id  BIGINT NOT NULL,
  empresa_id  BIGINT NOT NULL,
  conta_id    BIGINT NOT NULL,
  papel       VARCHAR(32) NOT NULL DEFAULT 'OPERADOR',
  ativo       BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (usuario_id, empresa_id)
);

-- Assinatura (espelho profiles + Stripe)
CREATE TABLE assinatura (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  conta_id            BIGINT NOT NULL UNIQUE,
  stripe_subscription_id VARCHAR(64),
  status              VARCHAR(32) NOT NULL,  -- pendente, ativa, vencida, cancelada
  plano_codigo        VARCHAR(64) NOT NULL DEFAULT 'starter',
  empresas_quota      INT NOT NULL DEFAULT 1,
  usuarios_quota      INT NOT NULL DEFAULT 3,
  nfse_mes_quota      INT NOT NULL DEFAULT 100,
  nfe_mes_quota       INT NOT NULL DEFAULT 50,
  periodo_fim         TIMESTAMP NULL,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Uso mensal (para enforcement)
CREATE TABLE uso_mensal (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  conta_id    BIGINT NOT NULL,
  ano_mes     CHAR(7) NOT NULL,  -- 2026-05
  nfse_count  INT NOT NULL DEFAULT 0,
  nfe_count   INT NOT NULL DEFAULT 0,
  UNIQUE (conta_id, ano_mes)
);
```

### Migração de dados existentes

1. Criar `conta` por cada `empresa` atual (1:1 inicial).
2. Popular `conta_empresa` e `usuario_empresa` a partir de `usuario.empresa_id`.
3. Manter `usuario.empresa_id` como empresa padrão (compatibilidade).
4. Assinatura inicial `status=ativa`, quotas altas (clientes legados).

---

## 5. Fases de implementação

## Fase 1 — Tenant e isolamento ✅ (implementado)

- [x] Migrations `conta`, `conta_empresa`, `usuario_empresa` (V17)
- [x] `MembershipService` + `MembershipAuthFilter`
- [x] Restringir `/api/auth/empresas` e `/api/auth/trocar-empresa` a empresas do usuário
- [x] Escopar `/api/empresas/**` por membership
- [x] Troca de empresa mantém o usuário logado (não troca para usuário de integração)
- [x] Certificado/logo portal exigem membership
- [ ] `@PreAuthorize` por `papel` em todos os controllers (parcial: gestão em cert/logo/empresa)
- [ ] Frontend: validar sessão vs empresas permitidas (filtro API já bloqueia)

**Arquivos principais:** `V17__conta_membership.sql`, `MembershipService.java`, `MembershipAuthFilter.java`

### Fase 2 — Identidade multiusuário ✅ (implementado)

- [x] Usuário em várias empresas da mesma conta (`usuario_empresa` + vínculo por e-mail existente)
- [x] Convites (`usuario_convite`) + `/convite?token=`
- [x] Refresh token (`refresh_token`, 30 dias) + `POST /api/auth/refresh`
- [x] RBAC: `VISUALIZADOR` (leitura), `OPERADOR` (emissão), `ADMIN`/`OWNER` (gestão)
- [x] API `/api/conta/usuarios`, `/api/conta/convites`
- [x] UI convite + gestão de usuários em `/cadastros/usuarios`
- [ ] Reset de senha por e-mail (pendente)
- [ ] MFA (pendente)

**Arquivos:** `V18__convite_refresh_token.sql`, `UsuarioContaService.java`, `ContaUsuarioController.java`

### Fase 3 — Assinaturas Stripe ✅ (implementado)

- [x] Tabelas `assinatura`, `uso_mensal`, `conta.stripe_customer_id` (V19)
- [x] Stripe Java SDK + checkout + portal + webhook
- [x] Cotas por pacote: 1 empresa, 5 usuários, 100 NFS-e/mês, 50 NF-e/mês
- [x] Gate emissão NF-e/NFS-e + contagem mensal
- [x] UI `/conta/assinatura` com barras de uso
- [ ] E-mail inadimplência / dunning (pendente)

**Env vars:** `STRIPE_ENABLED`, `STRIPE_TEST_SECRET_KEY`, `STRIPE_TEST_PRICE_ID`, `STRIPE_WEBHOOK_SECRET`

Contas legadas migradas com plano **ativa** generoso (Stripe off por padrão).

**Referência:** `server/api/stripe/*`, `profiles` schema

### Fase 4 — UX SaaS e onboarding ✅ (implementado)

- [x] Signup self-service → cria `conta` + trial 14 dias (`POST /api/auth/register`)
- [x] Home com grid de empresas (cards clicáveis + troca de sessão)
- [x] Wizard primeira empresa (CNPJ, ambiente NFS-e) em `/onboarding`
- [x] Admin plataforma separado (`/auth/admin` + `NFSE_ADMIN_SECRET`) vs owner da conta
- [x] Menu perfil: assinatura, usuários, empresas (dropdown no topbar)
- [x] Consulta CNPJ pública (`GET /api/public/cnpj/{cnpj}`)

**Arquivos:** `V20__onboarding_usuario_empresa_null.sql`, `OnboardingService.java`, `/registrar`, `/onboarding`

### Fase 5 — Escala e compliance ✅ (implementado)

- [x] Audit log por conta (`audit_event`) + listagem `/conta/auditoria`
- [x] Métricas de uso no painel (`/conta/metricas`, histórico 12 meses)
- [x] Dunning / e-mail inadimplência (webhook + job diário 9h)
- [x] Export LGPD por conta (`GET /api/conta/lgpd/export`, UI `/conta/lgpd`)
- [x] RLS/schema-per-tenant: **não adotado** — isolamento lógico via `conta_id` + membership (suficiente para MVP; reavaliar se exigência regulatória)

**Env:** `MAIL_ENABLED`, `MAIL_USER`, `MAIL_PASSWORD`, `NFSE_DUNNING_CRON` (opcional)

**Arquivos:** `V21__audit_dunning_fase5.sql`, `AuditLogService`, `BillingDunningService`, `LgpdExportService`

---

## 6. Mapa de porte: página ↔ página

| multiatendimento | nfse2 atual | nfse2 alvo |
|------------------|-------------|------------|
| `/login` | `/login` | ✅ manter |
| `/` (grid workspaces) | `/` (cards módulos) | `/conta` ou `/` com empresas |
| `/workspace/[id]/dashboard` | `/` após login | `/empresa/[id]` ou sessão |
| `/workspace/[id]/atendentes` | `/cadastros/usuarios` | `/conta/usuarios` |
| `/workspace/[id]/configuracoes` | `/cadastros/empresa` | `/empresa/[id]/config` |
| `/assinatura` | — | `/conta/assinatura` |
| `/auth/admin` | `/auth/admin` | manter só plataforma |

---

## 7. Riscos de segurança atuais

| Risco | Severidade | Mitigação |
|-------|------------|-----------|
| Troca para qualquer empresa | **Crítica** | Fase 1 — membership |
| CRUD empresa sem escopo | **Crítica** | Fase 1 — `conta_id` |
| Admin key no `sessionStorage` | Alta | Restringir a rota admin; HttpOnly cookie server-side |
| Token permanente | Alta | Expiração + refresh |
| Sem rate limit login | Média | Bucket por IP |
| `perfil` ignorado | Média | RBAC Fase 2 |

---

## 8. Estimativa de esforço por módulo

| Módulo | Esforço | Dependência |
|--------|---------|-------------|
| Migrations + entidades conta/membership | 1 sem | — |
| MembershipService + scoping API | 2 sem | migrations |
| Frontend troca empresa segura | 1 sem | membership API |
| RBAC papéis | 1–2 sem | membership |
| JWT refresh | 1 sem | — |
| Stripe checkout/portal/webhook | 2–3 sem | conta |
| Subscription gate emissão | 1 sem | assinatura |
| UI assinatura | 1 sem | billing API |
| Onboarding self-service | 2 sem | conta + billing |
| **Total MVP SaaS** | **~12–16 sem** | 1 dev full-stack |

---

## 9. Como usar o Claude Code neste roadmap

```bash
cd /home/aurelio/FONTES/SPRING/nfse2

# Sessão focada na Fase 1
claude -p "Implemente Fase 1 do SAAS-ROADMAP.md: migrations conta/usuario_empresa, MembershipService, restrinja AuthEmpresaService e EmpresaPortalController. Use como referência checkWorkspace.ts em curso_multiatendimento-master." \
  --add-dir portal-api --add-dir portal-web

# Sessão focada em billing
claude -p "Porte Stripe de curso_multiatendimento-master/server/api/stripe para portal-api Spring conforme SAAS-ROADMAP.md Fase 3." \
  --add-dir portal-api
```

---

## 10. Decisões em aberto

1. **Uma conta = um CNPJ raiz ou vários emitentes?** Recomendado: vários (cooperativa, grupo agro).
2. **Stripe BR:** conta Stripe já configurada? Definir moeda BRL e produtos.
3. **Trial:** 14 dias sem cartão ou exigir cartão upfront (como multiatendimento)?
4. **Plano por volume ou flat?** Começar com quotas simples (espelho `pacotes × N`).
5. **Manter embed operador** para ERP interno ou só SaaS self-service?

---

## 11. Referências rápidas

### nfse2

- Auth: `portal-api/.../security/EmbedTokenService.java`, `AuthController.java`
- Troca empresa: `AuthEmpresaService.java`, `EmpresaSwitcher.tsx`
- Migrations: `portal-api/src/main/resources/db/migration/`
- Session web: `portal-web/src/lib/app-session.ts`

### curso_multiatendimento

- Schema: `supabase/schema.sql`
- Workspace guard: `server/utils/checkWorkspace.ts`
- Subscription gate: `server/utils/checkSubscription.ts`
- Stripe: `server/api/stripe/`
- UI assinatura: `app/pages/assinatura.vue`
- Workspace picker: `app/pages/index.vue`
- Embed protocol: `app/lib/embedPostMessage.ts`

---

*Última atualização: 2026-05-22 — gerado a partir da análise de nfse2 + curso_multiatendimento-master.*
