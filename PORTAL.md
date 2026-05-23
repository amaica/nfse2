# Portal NFS-e (embed) — documentação

Portal web para **emitir**, **consultar** e **imprimir** NFS-e no padrão **nacional (SEFIN/ADN)**, usando certificado **A1 (.pfx)** e a biblioteca `io.github.t3wv:nfse` deste repositório.

---

## Índice

1. [Arquitetura](#1-arquitetura)
2. [Subir em homologação](#2-subir-em-homologação-passo-a-passo)
3. [Certificado digital](#3-certificado-digital)
4. [Município e ambiente](#4-município-e-ambiente)
5. [Interface (wizard)](#5-interface-wizard)
6. [Lista LC 116](#6-lista-de-serviços-lc-116)
7. [NBS](#7-nbs-nomenclatura-brasileira-de-serviços)
8. [Emissão e impressão](#8-emissão-consulta-e-impressão)
9. [API REST](#9-api-rest-do-portal)
9. [Autenticação (token embed)](#9-autenticação-token-embed)
10. [Variáveis de ambiente](#10-variáveis-de-ambiente)
11. [Problemas comuns](#11-problemas-comuns)

---

## 1. Arquitetura

| Componente | Pasta | Porta | Função |
|------------|-------|-------|--------|
| **portal-api** | `portal-api/` | 8080 | Spring Boot: certificado, emissão DPS, PDF/XML, LC 116 |
| **portal-web** | `portal-web/` | 3000 | Next.js: wizard embed + consulta |
| **lib nfse** | `src/main/java/...` | — | Integração SEFIN/ADN (homolog = produção restrita) |

Fluxo de emissão:

```
Navegador (embed) → portal-api → WSFacade → sefin.producaorestrita.nfse.gov.br (homolog)
                     ↑ mTLS com certificado A1
```

---

## 2. Subir em homologação (passo a passo)

### Pré-requisitos

- JDK **21**
- Node **18+** e `npm` no `portal-web`
- Certificado **e-CNPJ A1** em `.pfx` (o de testes/homologação que vocês já usam)
- Senha do PFX

### Opção A — script pronto (recomendado)

```bash
cd /home/aurelio/FONTES/SPRING/nfse2

export CERTIFICADO_PATH=/home/aurelio/FONTES/CERTIFICADOS/ClovisWerlang_safe1283.pfx
export CERTIFICADO_SENHA=safe1283
export MUNICIPIO_IBGE=4310009          # código IBGE do município do prestador (7 dígitos)
export PREFEITURA="Ibiruba/RS"
export NFSE_AMBIENTE=homologacao

chmod +x scripts/portal-homolog.sh
./scripts/portal-homolog.sh
```

O script chama `start-portal.sh`, que compila a lib, sobe API e Next.js e imprime a URL com token.

### Opção B — manual

```bash
mvn install -DskipTests
cd portal-api && mvn package -DskipTests
java -jar target/nfse-portal-api-0.1.0-SNAPSHOT.jar   # com as variáveis acima exportadas

cd ../portal-web && npm install && npm run dev
```

### Abrir o portal

1. Acesse a URL exibida no terminal, por exemplo:  
   `http://localhost:3000/embed?t=TOKEN`
2. Login da API (se precisar gerar token de novo):  
   **email** `admin@synki.demo` · **senha** `demo123`

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@synki.demo","senha":"demo123"}'
```

Use o campo `token` na query `?t=`.

### Trocar certificado (somente servidor)

A tela **não** pede PFX nem senha. O certificado vem da API (`DemoCertificadoLoader` + variáveis `CERTIFICADO_PATH` / `CERTIFICADO_SENHA`).

Para atualizar sem expor arquivo na UI:

```bash
export CERTIFICADO_PATH=/caminho/seu.pfx
export CERTIFICADO_SENHA=senha
chmod +x scripts/portal-upload-cert.sh
./scripts/portal-upload-cert.sh
```

Ou reinicie a API com as variáveis corretas (e banco sem certificado, se necessário).

---

## 3. Certificado digital

### Carregamento automático na subida

Se não existir certificado no banco, a API lê o PFX configurado em:

- `CERTIFICADO_PATH` (padrão em `application.yml`: `ClovisWerlang_safe1283.pfx`)
- `CERTIFICADO_SENHA`

Classe: `DemoCertificadoLoader`.

O **CNPJ do prestador** é extraído do subject do certificado e gravado na empresa (`CertificadoService` / `CertificadoLeituraService`).

### Requisitos para emitir

| Requisito | Motivo |
|-----------|--------|
| Certificado **e-CNPJ** A1 | Pessoa física (e-CPF) não habilita emissão como CNPJ |
| PFX válido e senha correta | mTLS na SEFIN |
| CNPJ do cert = prestador na DPS | Validação nacional |
| Município conveniado + parâmetros | Alíquota e regras no ADN |

A barra verde/amarela no topo do wizard mostra apenas dados de `GET /api/nfse/emissao/contexto` (ambiente, município, prestador do certificado no servidor).

### Cadeia SSL (truststore)

Na primeira chamada à SEFIN, a API gera `nfse-cacerts-{empresaId}.jks` em `/tmp` com os hosts de homologação/produção (ver README §3 da lib).

---

## 4. Município e ambiente

### Homologação vs produção

| `NFSE_AMBIENTE` / coluna `ambiente` | Lib `config.isTeste()` | Hosts SEFIN/ADN |
|-------------------------------------|------------------------|-----------------|
| `homologacao` | `true` | `*.producaorestrita.nfse.gov.br` |
| `producao` | `false` | `*.nfse.gov.br` |

**Notas em homologação não têm valor fiscal.**

### Ajustar município (IBGE)

O código IBGE de **7 dígitos** deve ser o município do **prestador** (emissor), alinhado ao convênio municipal:

```bash
export MUNICIPIO_IBGE=4216602    # exemplo: São José/SC
export PREFEITURA="Sao Jose/SC"
```

Reinicie a API ou use Flyway/SQL na tabela `configuracao_nfse` (empresa_id = 1).

Migração `V7__homologacao_padrao.sql` define ambiente **homologacao** para a empresa demo.

`DemoHomologConfigUpdater` aplica `MUNICIPIO_IBGE`, `PREFEITURA` e `NFSE_AMBIENTE` a cada subida.

### Conferir convênio

```bash
curl -s "http://localhost:8080/api/nfse/convenio/4310009" \
  -H "Authorization: Bearer SEU_TOKEN"
```

---

## 5. Interface (wizard)

Abas no embed: **Emitir** | **Consultar**.

### Wizard Emitir (4 passos)

1. **Cliente** — CPF/CNPJ (Brasil API + histórico local)
2. **Serviço** — busca na lista LC 116 completa
3. **Valor** — valor, Simples Nacional, ISS retido; alíquota consultada no ADN
4. **Revisão** — resumo e botão **Emitir NFS-e**

**Classificação fiscal do serviço** (passo Serviço): LC 116, **NBS** (920 códigos oficiais MDIC), CNAE e código tributário — autocomplete pesquisável; sugestão automática de NBS ao escolher o item LC 116.

Seção recolhida **Tributação e Classificação Fiscal**: retenções, RPS, IBS/CBS, etc.

Cor de marca: `#61AD3E`, fundo claro (estilo checkout enxuto).

---

## 6. Lista de serviços LC 116

| Aspecto | Detalhe |
|---------|---------|
| Fonte | [Lista LC 116 — gov.br](https://www.gov.br/nfse/pt-br/mei-e-demais-empresas/codigos-de-tributacao-nacional-nbs) |
| Armazenamento | `portal-api/src/main/resources/lc116-servicos.json` (**335** itens) |
| API | `GET /api/nfse/servicos?termo=&limite=400&grupo=` |
| Filtros `grupo` | `todos` (padrão), `agro`, `mecanico` |
| Formato código | `XX.XX.XX.000` (ex.: `01.07.01.000`) |

Na tela **Serviço**, use os chips **Agro** / **Mecânico** ou busque (ex.: `trator`, `agronomia`, `funilaria`). Com **Todos**, a lista completa aparece com agro e mecânico no topo.

A API federal **não** lista LC 116; só consulta alíquota **por código**. O catálogo fica no portal.

---

## 7. NBS (Nomenclatura Brasileira de Serviços)

| Aspecto | Detalhe |
|---------|---------|
| Fonte | [NBS 2.0 — MDIC](https://www.gov.br/mdic/pt-br/assuntos/sdic/comercio-e-servicos/nbs-nomenclatura-brasileira-de-servicos) (`nbs2-0.csv`) |
| Armazenamento | `portal-api/src/main/resources/nbs-servicos.json` (**920** subitens) |
| API | `GET /api/nfse/nbs?termo=&limite=40&lc116=` |
| Na DPS | Campo `cNBS` (9 dígitos, ex.: `114061100`) |
| UX | Bloco **Classificação fiscal do serviço** no passo Serviço; autocomplete; últimos usados no navegador; sugestão ao escolher LC 116 |

Exemplo na tela: `[ 1.1406.11.00 ] Desenvolvimento de software…`

Correlação LC 116 → NBS usa tabela simplificada (Anexo VIII); confira o código exato na [planilha oficial RTC](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/rtc/anexoviii-correlacaoitemnbsindopcclasstrib_ibscbs_v1-00-00.xlsx/view).

---

## 8. Emissão, consulta e impressão

### Emitir

1. Certificado OK (barra verde)
2. Preencher wizard → **Emitir NFS-e**
3. Sucesso: chave de **50 dígitos**

`POST /api/nfse/emitir` monta a DPS (`DpsMontadorService`) e chama `WSFacade.emitirNFSe`.

### Imprimir / baixar (DANFSe)

| Onde | Ação |
|------|------|
| Tela de sucesso | **Imprimir** (PDF inline + diálogo de impressão), **Baixar PDF**, **XML** |
| Consultar → chave | **Imprimir DANFSe**, **Baixar PDF**, **XML** |
| Consultar → tabela | Ícones impressora / download por nota |

| Endpoint | Uso |
|----------|-----|
| `GET /api/nfse/pdf/{chave}` | Download (`attachment`) |
| `GET /api/nfse/pdf/{chave}?inline=true` | Visualizar/imprimir no navegador |
| `GET /api/nfse/xml/{chave}` | XML da NFS-e |
| `GET /api/nfse/consulta/{chave}` | JSON da consulta SEFIN |

Implementação: `WSFacade.downloadNotaPdf` / `downloadNotaXml`.

**Pop-ups** devem estar liberados para **Imprimir**.

### Histórico

`GET /api/nfse/historico` — últimas ações (emissão/consulta) da empresa no portal.

---

## 9. API REST do portal

Todas as rotas abaixo (exceto login/validate) exigem header:

```
Authorization: Bearer TOKEN
```

### Auth

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/auth/login` | `{ "email", "senha" }` → `{ "token" }` |
| GET | `/api/auth/embed/validate?t=` | Valida token embed |

### NFS-e

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/nfse/emissao/contexto` | Prestador, município, ambiente, `podeEmitir` |
| GET | `/api/nfse/config` | Prefeitura, IBGE, ambiente |
| GET | `/api/nfse/servicos?termo=&limite=` | Busca LC 116 |
| GET | `/api/nfse/nbs?termo=&lc116=` | Busca / sugestão NBS |
| GET | `/api/nfse/aliquota?codigoMunicipio=&codigoServico=` | Alíquota ADN |
| GET | `/api/nfse/convenio/{ibge}` | Status convênio |
| POST | `/api/nfse/emitir` | Corpo `EmissaoCompletaRequest` |
| GET | `/api/nfse/consulta/{chave}` | Consulta por chave |
| GET | `/api/nfse/pdf/{chave}` | PDF DANFSe |
| GET | `/api/nfse/xml/{chave}` | XML |
| GET | `/api/nfse/historico` | Log de operações |

Health: `GET http://localhost:8080/actuator/health`

---

## 10. Autenticação (token embed / iframe)

Cada **empresa** usa o portal dentro do próprio sistema via **iframe**. O token identifica `empresaId` + `usuarioId` e autoriza todas as chamadas à API (`Authorization: Bearer …`).

### Formato do token

| Item | Valor |
|------|--------|
| Codificação | Base64 URL-safe, sem padding |
| Payload (antes da assinatura) | `empresaId \| usuarioId \| exp` |
| Assinatura | HMAC-SHA256 do payload, com secret `NFSE_JWT_SECRET`, também em Base64 URL-safe |
| String final | `Base64( empresaId \| usuarioId \| exp \| assinatura )` |

Exemplo de payload decodificado: `1|2|0` → empresa `1`, usuário `2`, **sem expiração** (`exp = 0`).

O token **não contém** CPF, e-mail ou senha — apenas IDs internos do portal.

### Expiração

| `exp` | Comportamento |
|-------|----------------|
| `0` | **Permanente** (padrão). Token válido até trocar o `NFSE_JWT_SECRET` ou revogar o usuário. Ideal para iframe fixo por empresa. |
| `> 0` | Epoch Unix (segundos). Token expira nesse instante. Usado se `NFSE_JWT_EXP_MIN` &gt; 0 na API. |

Variável: `NFSE_JWT_EXP_MIN` — padrão **`0`** (nunca expira). Para sessão temporária (ex.: 480 = 8 horas), defina um valor positivo.

### Como gerar o token (backend da empresa)

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"usuario@empresa.com","senha":"***"}'
```

Resposta:

```json
{
  "token": "…",
  "empresaId": 1,
  "usuarioId": 2,
  "nome": "Operador"
}
```

Guarde o `token` no seu ERP (configuração por empresa). **Não** é necessário gerar um token novo a cada abertura do iframe, desde que `NFSE_JWT_EXP_MIN=0`.

Validação (opcional, ao carregar o iframe):

```http
GET /api/auth/embed/validate?t=TOKEN
```

### Montar o iframe

```html
<iframe
  src="https://portal.seudominio.com.br/embed?t=TOKEN_DA_EMPRESA"
  title="NFS-e"
  style="width:100%;min-height:720px;border:0"
></iframe>
```

O Next.js lê `?t=`, valida na API e envia `Authorization: Bearer TOKEN` em todas as requisições NFS-e.

### Eventos para o sistema pai (`postMessage`)

Origem: `source: "synki-nfse"`. Restrinja no ERP com `event.origin` em produção.

| `type` | Campos | Quando |
|--------|--------|--------|
| `NFSE_EMITIDA` | `chave` | Emissão com sucesso |
| `ERRO_EMISSAO` | `mensagem` | Falha na emissão |
| `NFSE_CANCELADA` | `chave` | Cancelamento (quando implementado) |

Exemplo no ERP:

```javascript
window.addEventListener("message", (event) => {
  if (event.data?.source !== "synki-nfse") return;
  if (event.data.type === "NFSE_EMITIDA") {
    console.log("Chave NFS-e:", event.data.chave);
  }
});
```

### Multiempresa

- Um **usuário** pertence a uma **empresa** (`empresa` no banco).
- Certificado, município, ambiente e histórico são **por `empresaId`** extraído do token.
- Cada cliente do SaaS: cadastro de empresa + usuário de integração + token permanente na configuração.

### Segurança

| Prática | Motivo |
|---------|--------|
| `NFSE_JWT_SECRET` longo e aleatório em produção | Quem tem o secret forja tokens |
| HTTPS no portal e no ERP | Token na query `?t=` pode vazar em logs de proxy |
| `CORS_ORIGINS` com o domínio do portal/ERP | API só aceita origens configuradas |
| Rotacionar o secret invalida todos os tokens | Planeje troca com janela de manutenção |
| Um usuário de integração por empresa | Auditoria e revogação simples |

Tokens antigos gerados com `exp` futuro (quando `NFSE_JWT_EXP_MIN` era 480) continuam válidos até o `exp` passar; novos tokens seguem a configuração atual.

---

## 11. Variáveis de ambiente

Ver `portal-api/.env.example`.

| Variável | Descrição |
|----------|-----------|
| `CERTIFICADO_PATH` | Caminho do `.pfx` para carga inicial |
| `CERTIFICADO_SENHA` | Senha do PFX |
| `NFSE_AMBIENTE` | `homologacao` ou `producao` |
| `MUNICIPIO_IBGE` | 7 dígitos IBGE do prestador |
| `PREFEITURA` | Nome exibido na UI |
| `NFSE_JWT_SECRET` | Assinatura HMAC do token embed |
| `NFSE_JWT_EXP_MIN` | `0` = token permanente (iframe). `>0` = minutos até expirar |
| `PORT` | Porta API (8080) |
| `CORS_ORIGINS` | Origem do Next (3000) |

Frontend: `portal-web/.env.local` → `NEXT_PUBLIC_API_URL=http://localhost:8080`

---

## 12. Problemas comuns

| Sintoma | O que verificar |
|---------|-----------------|
| Botão Emitir desabilitado | Certificado ausente, e-CPF em vez de e-CNPJ, ou aviso na barra superior |
| Erro SSL / handshake | Apague `/tmp/nfse-cacerts-1.jks` e tente de novo (regenera cadeia) |
| Alíquota não encontrada | Código de serviço ou IBGE incorretos; município sem parametrização no ADN |
| Emissão rejeitada pela SEFIN | Tomador/serviço/valores; CNPJ prestador ≠ certificado; ambiente DPS ≠ homolog |
| PDF 404 / erro | Chave inválida, nota ainda não disponível, ou cert sem permissão na nota |
| Imprimir não abre | Pop-up bloqueado; use **Baixar PDF** |
| Certificado não carrega na subida | Arquivo inexistente em `CERTIFICADO_PATH`; use upload manual |

### Resetar banco H2 (dev)

```bash
rm -f portal-api/data/nfse-portal.*
# Subir API de novo — Flyway recria tabelas e seed
```

---

## Referências

- Biblioteca e endpoints nacionais: [README.md](README.md)
- Manual ADN/SEFIN: [gov.br/nfse](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica)
