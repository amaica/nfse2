# NFS-e — biblioteca Java

Biblioteca para emissão e consulta de **Nota Fiscal de Serviço Eletrônica (NFS-e)** — ambiente **nacional (SEFIN/ADN)** e integrações **municipais** (São Paulo, Barueri, São José/SC).

- **Java:** 21  
- **Build:** Maven (`mvn clean package`)  
- **Artifact:** `io.github.t3wv:nfse`

> Esta lib **não é uma aplicação web**. Não há servidor HTTP embutido: o uso é via classes Java (`WSFacade`, etc.) ou pelo script de demonstração `./run.sh`.

---

## Índice

1. [Executar / validar ambiente](#1-executar--validar-ambiente)
2. [Configuração (produção e homologação)](#2-configuração-produção-e-homologação)
3. [Certificado e cadeia SSL (cacerts)](#3-certificado-e-cadeia-ssl-cacerts)
4. [Uso no código — facade nacional](#4-uso-no-código--facade-nacional)
5. [Mapa de endpoints — produção](#5-mapa-de-endpoints--produção)
6. [Mapa de endpoints — homologação](#6-mapa-de-endpoints--homologação)
7. [Integrações municipais](#7-integrações-municipais)
8. [Testes Maven](#8-testes-maven)
9. [Dependência em outro projeto](#9-dependência-em-outro-projeto)
10. [Limitações](#10-limitações)

---

## 1. Executar / validar ambiente

### Pré-requisitos

- Java 21  
- Maven 3.8+  
- Certificado digital **A1** (`.pfx`)  
- Senha do certificado  

### Demo rápido (consulta API nacional em produção)

Configuração local já pode existir em `src/main/resources/nfse.properties` (gitignored).

```bash
cd /caminho/para/nfse2
./run.sh
# ou (alternativa direta com java):
./run-demo.sh
```

**No Cursor:** abra **Run and Debug** (Ctrl+Shift+D) → **NFS-e DemoRun** → Play (F5).

Com município e serviço customizados:

```bash
./run.sh 4216602 01.01.01.000
```

O script `run.sh`:

1. Define variáveis de ambiente (certificado, cacerts, produção)  
2. Gera `/tmp/nfse_cacerts.jks` na primeira execução, se não existir  
3. Roda `io.github.t3wv.nfse.DemoRun` (convênio + alíquota)  

Saída esperada (exemplo):

```
Ambiente: PRODUCAO
Certificado: /caminho/certificado.pfx
--- Convenio municipio 4216602 ---
NFSeParametrosMunicipaisParametrosConvenio{aderenteAmbienteNacional=true, ...}
--- Aliquota servico 01.01.01.000 ---
Aliquota: 2
OK - biblioteca operacional.
```

### Configuração recomendada: `nfse.properties`

```bash
cp src/main/resources/nfse.properties.example src/main/resources/nfse.properties
# edite caminhos, senhas e nfse.municipio.codigo-ibge da sua cidade
./run.sh
```

O `run.sh` usa `src/main/resources/nfse.properties` se existir. Sem esse arquivo, cai no fallback com `export` (legado).

### IDE (Cursor / IntelliJ)

- **Main class:** `io.github.t3wv.nfse.DemoRun`  
- **VM options (opcional):** `-Dnfse.config=/caminho/nfse.properties`  
- Ou coloque `nfse.properties` em `src/main/resources`  

### Spring Tool Suite (STS) / Eclipse

Este projeto **nao e Spring Boot**. O botao **Run As → Spring Boot App** nao funciona aqui.

**Passo a passo:**

1. **Java 21** no STS: `Window → Preferences → Java → Installed JREs` (JDK 21).
2. Importar: `File → Import → Maven → Existing Maven Projects` → pasta `nfse2`.
3. Atualizar Maven: botao direito no projeto → `Maven → Update Project` (F5).
4. Criar configuracao:
   ```bash
   ./setup-sts.sh
   ```
   Edite `src/main/resources/nfse.properties` (certificado e `nfse.municipio.codigo-ibge`).
5. Executar de **uma** destas formas:
   - Botao direito em **`DemoRun.launch`** → **Run As**
   - Botao direito em **`DemoRun.java`** → **Run As → Java Application** (nao Spring Boot)
6. Se a launch der erro de projeto: abra `DemoRun.launch` e ajuste `PROJECT_ATTR` para o nome exato do projeto no Package Explorer (ex.: `nfse2`).

**VM arguments** (se configurar manualmente em Run Configurations):

```
-Dnfse.config=${project_loc}/src/main/resources/nfse.properties
```

Working directory: `${project_loc}` (raiz do projeto Maven).

**Problemas comuns:**

| Sintoma | Solucao |
|---------|---------|
| "Main class not found" | `Maven → Update Project`, depois `Project → Clean` |
| Erro de certificado / config | Criar `nfse.properties` (passo 4) |
| Java version mismatch | Projeto usa Java **21** no `pom.xml` |
| Spring Boot App nao inicia | Use **Java Application** em `DemoRun` |

### Build

```bash
mvn clean test      # compila + testes unitários
mvn clean install   # instala no ~/.m2
mvn clean package   # gera target/nfse-*-SNAPSHOT.jar
```

---

## 2. Configuração (produção e homologação)

### Arquivo `nfse.properties` (recomendado)

Copie o exemplo e edite:

```bash
cp src/main/resources/nfse.properties.example src/main/resources/nfse.properties
```

```properties
# Certificado A1 da sua empresa (prestador)
nfse.certificado.path=/home/aurelio/FONTES/CERTIFICADOS/seu_certificado.pfx
nfse.certificado.senha=sua_senha

nfse.cadeia-certificados.path=/tmp/nfse_cacerts.jks
nfse.cadeia-certificados.senha=senha

# producao | homologacao
nfse.ambiente=producao

# Sua cidade (IBGE, 7 digitos)
nfse.municipio.codigo-ibge=4216602
```

Onde colocar o arquivo:

| Local | Quando usar |
|-------|-------------|
| `src/main/resources/nfse.properties` | App Maven / Spring Boot no mesmo projeto |
| `application.properties` | Spring Boot — pode usar as **mesmas chaves** `nfse.*` abaixo |
| Caminho externo | `-Dnfse.config=/etc/nfse/nfse.properties` |

**Precedência** (o que vale se estiver definido nos dois):  
`variável de ambiente` > `System property` > `arquivo properties`

### No código

```java
import io.github.t3wv.nfse.NFSeConfigProperties;
import io.github.t3wv.nfse.nacional.WSFacade;

final var config = new NFSeConfigProperties();
final var facade = new WSFacade(config);
String ibge = config.getCodigoMunicipioIbge(); // opcional, do properties
```

### Spring Boot (`application.properties`)

Esta biblioteca **não depende do Spring**, mas as chaves são compatíveis:

```properties
nfse.certificado.path=/caminho/certificado.pfx
nfse.certificado.senha=${NFSE_CERT_SENHA}
nfse.cadeia-certificados.path=/tmp/nfse_cacerts.jks
nfse.cadeia-certificados.senha=senha
nfse.ambiente=producao
nfse.municipio.codigo-ibge=4216602
```

```java
@Bean
NFSeConfig nfseConfig() {
    return new NFSeConfigProperties(); // le application.properties do classpath
}

@Bean
WSFacade nfseFacade(NFSeConfig config) {
    return new WSFacade(config);
}
```

> Em produção, **não commite** senhas: use variáveis de ambiente do servidor ou Spring `${...}` para `nfse.certificado.senha`.

### Variáveis de ambiente (alternativa ao properties)

Úteis em Docker/CI; **sobrescrevem** o arquivo se definidas:

| Property | Variável de ambiente equivalente |
|----------|----------------------------------|
| `nfse.certificado.path` | `CERTIFICADO_PATH` |
| `nfse.certificado.senha` | `CERTIFICADO_SENHA` |
| `nfse.cadeia-certificados.path` | `CADEIA_CERTIFICADOS_PATH` |
| `nfse.cadeia-certificados.senha` | `CADEIA_CERTIFICADOS_SENHA` |
| `nfse.ambiente=producao` | `NFSE_PRODUCAO=true` |

| `nfse.ambiente` | Efeito |
|-----------------|--------|
| `producao` | `config.isTeste() == false` |
| `homologacao` | `config.isTeste() == true` |

Implementação alternativa: interface `io.github.t3wv.nfse.NFSeConfig` (implementação customizada).

**Autenticação TLS (nacional, São Paulo, Barueri):** certificado A1 no cliente + truststore (`cacerts.jks`).

**Exceção:** São José/SC usa **HTTP Basic** (login/senha do portal Atende.net), sem certificado na requisição.

---

## 3. Certificado e cadeia SSL (cacerts)

### Gerar `cacerts.jks`

A cadeia é obtida automaticamente no `DemoRun` / `run.sh` na primeira execução.

Geração manual (teste sob demanda):

- Classe: `src/test/java/io/github/t3wv/nfse/utils/NFSeCadeiaCertificadosTest.java`  
- Método: `geraCadeiaCertificados()` (está `@Disabled`; habilitar para rodar)  
- API: `NFSeCadeiaCertificados.geraCadeiaCertificados(config)`  

### Hosts incluídos na cadeia

| Produção | Homologação |
|----------|-------------|
| `sefin.nfse.gov.br` | `sefin.producaorestrita.nfse.gov.br` |
| `adn.nfse.gov.br` | `adn.producaorestrita.nfse.gov.br` |
| `www.barueri.sp.gov.br` | `testeeiss.barueri.sp.gov.br` |
| `nfews.prefeitura.sp.gov.br` | — |

---

## 4. Uso no código — facade nacional

Classe: `io.github.t3wv.nfse.nacional.WSFacade`

### Consultar convênio do município

```java
final var convenio = facade.consultaConvenioMunicipio("4216602"); // São José, SC
// convenio.isAderenteAmbienteNacional(), isAderenteEmissorNacional(), etc.
```

### Consultar alíquota

```java
final var aliquota = facade.consultaAliquotaMunicipioServicoCompetencia("4216602", "01.01.01.000");
// Ex.: BigDecimal 2 → 2%
```

### Emitir NFS-e (DPS)

```java
import io.github.t3wv.nfse.nacional.classes.nfsenacional.*;

final var dps = new NFSeSefinNacionalDPS()
    .setInfDPS(new NFSeSefinNacionalInfDPS()
        // ... preencher prestador, tomador, serviço, valores, tpAmb PRODUCAO em produção
    );

final var resultado = facade.emitirNFSe(dps);
// HTTP 201 → NFSeSefinNacionalNFSePostResponseSucesso
```

O ambiente do DPS (`tpAmb`) **deve coincidir** com `config.isTeste()`.

### Consultar / baixar nota emitida

```java
// PDF (DANFSe)
final byte[] pdf = facade.downloadNotaPdf(chave50Digitos);
Files.write(Paths.get("/tmp/nota.pdf"), pdf);

// XML
final String xml = facade.downloadNotaXml(chave50Digitos);
Files.writeString(Paths.get("/tmp/nota.xml"), xml);

// Consulta estruturada (status HTTP + objeto)
final var resposta = facade.buscarNFSeByChaveAcesso(chave50Digitos);
```

### Cancelar (evento)

```java
final var evento = new NFSeSefinNacionalPedRegEvt()
    .setInfPedReg(new NFSeSefinNacionalInfPedReg()
        .setChaveAcessoNFSE(chave50)
        .setEvento(new NFSeSefinNacionalInfPedRegTE101101()
            .setcMotivo(NFSeSefinNacionalTSCodJustCanc.OUTROS)
            .setxMotivo("Motivo do cancelamento")));

facade.cancelarNFSe(evento);
```

### Consultar eventos

```java
facade.consultaEventosNFSe(chave50, null, null);
facade.consultaEventoNFSe(chave50, codigoEvento, sequencial);
```

### Parâmetros municipais (métodos adicionais na facade)

```java
facade.consultaHistoricoAliquotaMunicipioServico(ibge, codigoServico);
facade.consultaBeneficioMunicipioBeneficioCompetencia(ibge, numeroBeneficio, competencia);
facade.consultaRegimesEspeciaisMunicipioServicoCompetencia(ibge, codigoServico, competencia);
facade.consultaRetencoesMunicipioCompetencia(ibge, competencia);
```

---

## 5. Mapa de endpoints — produção

Facade nacional: `io.github.t3wv.nfse.nacional.WSFacade`  
Config: `NFSE_PRODUCAO=true`

### 5.1 Parâmetros municipais (ADN)

**Base:** `https://adn.nfse.gov.br/parametrizacao`  
**Classe:** `WSParametrosMunicipais`

| Operação | HTTP | URL (produção) | Método Java |
|----------|------|----------------|-------------|
| Convênio | GET | `/parametrizacao/{ibge7}/convenio` | `consultaConvenioMunicipio` |
| Alíquota | GET | `/parametrizacao/{ibge7}/{servico}/{MM-DD-AAAA}/aliquota` | `consultaAliquotaMunicipioServicoCompetencia` |
| Histórico alíquotas | GET | `/parametrizacao/{ibge7}/{servico}/historicoaliquotas` | `consultaHistoricoAliquotaMunicipioServico` |
| Benefício fiscal | GET | `/parametrizacao/{ibge7}/{beneficio}/{MM-DD-AAAA}/beneficio` | `consultaBeneficioMunicipioBeneficioCompetencia` |
| Regimes especiais | GET | `/parametrizacao/{ibge7}/{servico}/{MM-DD-AAAA}/regimes_especiais` | `consultaRegimesEspeciaisMunicipioServicoCompetencia` |
| Retenções | GET | `/parametrizacao/{ibge7}/{MM-DD-AAAA}/retencoes` | `consultaRetencoesMunicipioCompetencia` |

- `{servico}`: `XX.XX.XX.XXX` (ex.: `01.01.01.000`; aceita 6 ou 9 dígitos numéricos, normalizado pela lib)  
- `{MM-DD-AAAA}`: competência (ex.: `05-22-2026`)  

### 5.2 Emissão, consulta e eventos (SEFIN)

**Base:** `https://sefin.nfse.gov.br/sefinnacional/nfse`  
**Classe:** `WSSefinNFSe`

| Operação | HTTP | URL (produção) | Body | Método Java |
|----------|------|----------------|------|-------------|
| Emitir NFS-e | POST | `/sefinnacional/nfse` | `{dpsXmlGZipB64:"..."}` | `emitirNFSe` |
| Consultar NFS-e | GET | `/sefinnacional/nfse/{chave50}` | — | `buscarNFSeByChaveAcesso` |
| XML | — | (via consulta) | — | `downloadNotaXml` |
| Evento (cancelamento) | POST | `/sefinnacional/nfse/{chave50}/eventos` | `{pedidoRegistroEventoXmlGZipB64:"..."}` | `cancelarNFSe` |
| Eventos | GET | `/sefinnacional/nfse/{chave50}/eventos/{cod}/{seq}` | — | `consultaEventosNFSe` / `consultaEventoNFSe` |

- `{chave50}`: exatamente **50 dígitos** (não numéricos são removidos)  
- Sucesso emissão/evento: HTTP **201**  
- Erros comuns: **400, 401, 403, 404**  

### 5.3 DANFSe (PDF)

**Base:** `https://adn.nfse.gov.br`  
**Classe:** `WSDANFSe`

| Operação | HTTP | URL (produção) | Método Java |
|----------|------|----------------|-------------|
| PDF | GET | `/danfse/{chave50}` | `downloadNotaPdf` |

---

## 6. Mapa de endpoints — homologação

Mesmas rotas, bases diferentes (`config.isTeste() == true`, `NFSE_PRODUCAO=false`):

| Serviço | Base homologação |
|---------|------------------|
| Parâmetros | `https://adn.producaorestrita.nfse.gov.br/parametrizacao` |
| SEFIN NFS-e | `https://sefin.producaorestrita.nfse.gov.br/sefinnacional/nfse` |
| DANFSe | `https://adn.producaorestrita.nfse.gov.br/danfse/{chave50}` |
| Barueri SOAP | `https://testeeiss.barueri.sp.gov.br/nfeservice/wsrps.asmx` |

> Nem todos os municíodos/serviços respondem em homologação (ex.: alíquota pode retornar 404).

---

## 7. Integrações municipais

### Quando usar qual fluxo

| Situação | Usar |
|----------|------|
| Município aderente ao **ambiente nacional** | `nacional.WSFacade` (DPS na SEFIN) |
| NFS-e **Prefeitura de São Paulo** (capital) | `municipal.nfseSPSaoPaulo.WSFacade` |
| NFS-e **Barueri/SP** | `WSBarueri` + arquivos posicionais |
| Portal **São José/SC** (Atende.net) | `WSSaoJose` |

---

### 7.1 São Paulo (capital) — SOAP

**URL (produção, única no código):** `https://nfews.prefeitura.sp.gov.br/lotenfe.asmx`  
**Facade:** `io.github.t3wv.nfse.municipal.nfseSPSaoPaulo.WSFacade`  
**Protocolo:** POST SOAP 1.2 — `Content-Type: application/soap+xml; charset=utf-8`  
**Auth:** certificado A1 + cacerts  

| Operação SOAP | Método Java |
|---------------|-------------|
| `TesteEnvioLoteRPS` | `enviarTesteLoteRPS` |
| `EnvioRPS` | `enviarRPS` |
| `ConsultaNFe` | `enviarPedidoConsultaNFe` |
| `CancelamentoNFe` | `enviarPedidoCancelamentoNFe` |

```java
import io.github.t3wv.nfse.municipal.nfseSPSaoPaulo.WSFacade;
import io.github.t3wv.nfse.municipal.nfseSPSaoPaulo.requests.*;

final var sp = new WSFacade(config);
sp.enviarRPS(pedidoEnvioRPS);
sp.enviarPedidoConsultaNFe(pedidoConsulta);
sp.enviarPedidoCancelamentoNFe(pedidoCancelamento);
```

---

### 7.2 Barueri/SP — SOAP

| Ambiente | URL |
|----------|-----|
| Produção | `https://www.barueri.sp.gov.br/nfeservice/wsrps.asmx` |
| Homologação | `https://testeeiss.barueri.sp.gov.br/nfeservice/wsrps.asmx` |

**Classe:** `io.github.t3wv.nfse.municipal.nfseSPBarueri.WSBarueri`  
**Auth:** certificado A1 + cacerts  

| Operação SOAP | Método Java |
|---------------|-------------|
| `NFeLoteEnviarArquivo` | `loteEnviarArquivo` |
| `NFeLoteStatusArquivo` | `loteStatusArquivo` |
| `NFeLoteBaixarArquivo` | `loteBaixarArquivo` |

Arquivo de remessa/retorno: formato **posicional** (registros tipo 1–9) — pacote `nfseSPBarueri.arquivos`.

---

### 7.3 São José/SC — REST multipart

**URL (produção):**  
`https://saojose.atende.net/atende.php?pg=rest&service=WNERestServiceNFSe&cidade=padrao`

**Classe:** `io.github.t3wv.nfse.municipal.nfseSCSaoJose.WSSaoJose`  
**Auth:** `Authorization: Basic base64(login:senha)` — **não usa** `NFSeConfig`/certificado na HTTP  

```java
final var ws = new WSSaoJose("cnpjOuLogin", "senhaPortal");
ws.enviarEmissaoNFSe(arquivoEmissao, "arquivo.xml");
ws.enviarCancelamentoNFSe(arquivoCancelamento, "cancelamento.xml");
ws.enviarSolicitacaoCancelamentoNFSe(arquivoSolicitacao, "solicitacao.xml");
ws.consultaNFSeCodigoAutenticidade(arquivoPesquisa, "pesquisa.xml");
```

| Operação | Método Java | XML envio |
|----------|-------------|-----------|
| Emissão | `enviarEmissaoNFSe` | `NFSeSCSaoJoseEmissaoEnvio` |
| Cancelamento | `enviarCancelamentoNFSe` | `NFSeSCSaoJoseCancelamentoEnvio` |
| Solicitação cancelamento | `enviarSolicitacaoCancelamentoNFSe` | `NFSeSCSaoJoseCancelamentoSolicitacaoEnvio` |
| Consulta autenticidade | `consultaNFSeCodigoAutenticidade` | `NFSeSCSaoJosePesquisaCodigoAutenticidadeEnvio` |

---

## 8. Testes Maven

```bash
mvn clean test
```

Variáveis para testes de integração (com `@Disabled` por padrão):

```
CADEIA_CERTIFICADOS_PATH=/tmp/cacerts.jks
CADEIA_CERTIFICADOS_SENHA=senha
CERTIFICADO_PATH=/tmp/certificado.pfx
CERTIFICADO_SENHA=senha
```

Classes de referência / documentação viva:

| Classe | Conteúdo |
|--------|----------|
| `NFSeNacionalTest` | Exemplos API nacional |
| `NFSeSPSaoPauloTest` | São Paulo |
| `NFSeSPBarueriTest` | Barueri |
| `NFSeSCSaoJoseTest` | São José |
| `NFSeCadeiaCertificadosTest` | Geração cacerts |

---

## 9. Dependência em outro projeto

Após `mvn install` neste repositório:

```xml
<dependency>
    <groupId>io.github.t3wv</groupId>
    <artifactId>nfse</artifactId>
    <version>1.0.41-SNAPSHOT</version>
</dependency>
```

---

## 10. Portal web (embed)

Interface para emitir NFS-e nacional com certificado A1, wizard de emissão, lista LC 116 completa e impressão do DANFSe (PDF).

**Documentação completa:** [PORTAL.md](PORTAL.md)

**Homologação rápida:**

```bash
export CERTIFICADO_PATH=/home/aurelio/FONTES/CERTIFICADOS/seu_certificado.pfx
export CERTIFICADO_SENHA=sua_senha
export MUNICIPIO_IBGE=4310009
export NFSE_AMBIENTE=homologacao
./scripts/portal-homolog.sh
```

Portal: `http://localhost:3000/embed?t=TOKEN` (token via login `admin@synki.demo` / `demo123`).

---

## 11. Limitações

- Não implementa **CNC**, distribuição DFe nem todos os serviços do manual nacional  
- Municípios fora de: **nacional**, **SP capital**, **Barueri**, **São José/SC**  
- São Paulo: apenas as **4 operações** em `WSLoteNFe` (sem lote assíncrono completo, guias, etc.)  
- Benefício municipal: formato do código de benefício ainda incerto nos testes (`NFSeNacionalTest` comentado)  

Para novo município: seguir padrão `NFSeHttpClient` + `NFSeConfig` (ou SOAP/multipart conforme o caso).

---

## 12. Referência rápida — classes principais

| Pacote / classe | Função |
|-----------------|--------|
| `NFSeConfig` / `NFSeConfigEnv` | Certificado, cadeia, ambiente |
| `nacional.WSFacade` | API nacional (recomendado) |
| `nacional.WSSefinNFSe` | SEFIN direto |
| `nacional.WSParametrosMunicipais` | Parâmetros ADN direto |
| `nacional.WSDANFSe` | PDF direto |
| `municipal.nfseSPSaoPaulo.WSFacade` | São Paulo |
| `municipal.nfseSPBarueri.WSBarueri` | Barueri |
| `municipal.nfseSCSaoJose.WSSaoJose` | São José |
| `DemoRun` | Executável de demonstração |
| `NFSeCadeiaCertificados` | Gerar truststore |
