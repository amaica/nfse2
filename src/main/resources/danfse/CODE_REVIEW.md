# Code review — DANFSe Jasper (lib)

## O que está certo
- Geração local na lib (`WSFacade.downloadNotaPdf` → XML SEFIN → Jasper), alinhado à NT 008/2026.
- Template em classpath (`/danfse/danfse.jrxml`); compile cache thread-safe (`volatile` + sync).
- Homologação: aviso vermelho só com `tpAmb=2` (não usa `ambGer`).
- Validação de XML vazio e PDF exportado (`%PDF`).
- Money format com `DecimalFormat` pt-BR (sem gambiarra de replace).

## Ajustes feitos neste review
- Import limpo de `DANFSeJasper` no `WSFacade` (sem FQCN).
- Log SLF4J na primeira compilação do template.
- Guard clauses em `gerarPdf` / `gerarPdfDeXml`.
- Testes unitários da lib: produção, homologação, facade helper, XML inválido.
- `junit-jupiter-engine` no POM.

## Melhorias futuras (não bloqueantes)
1. **Pré-compilar** `.jasper` no build Maven (plugin) para evitar compile em runtime.
2. Extrair `DANFSeXmlMapper` para API pública se consumidores quiserem customizar params.
3. Layout pixel-perfect Anexo I da NT 008 (hoje espelha o PDFBox interno).
4. Teste de regressão visual (compare hash de PDF ou render PNG).
5. Remover `WSDANFSe` ADN quando a comunidade confirmar desuso total.
