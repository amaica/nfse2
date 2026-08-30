# DANFSe — JasperReports (NT 008/2026)

Template: `danfse.jrxml`  
Engine: JasperReports 6.21.3  
Layout: espelha o DANFSe gerado localmente (blocos identificação, prestador, tomador, serviço, ISSQN, valores, complementares + QR).

## Runtime (portal-api)

- Classe: `DanfseJasperPdfService`
- Classpath: `/danfse/danfse.jrxml`
- Endpoint PDF usa Jasper; se falhar, cai no PDFBox (`DanfseLocalPdfService`)

## Parameters

| Parameter | Tipo | Observação |
|-----------|------|------------|
| CHAVE | String | 50 dígitos |
| CHAVE_FORMATADA | String | grupos de 10 |
| TP_AMB | String | `2` = homologação → aviso vermelho |
| AMBIENTE_LABEL | String | Producao / Homologacao |
| QR_CODE | java.awt.Image | consulta pública |
| PREST_* / TOMA_* / V_* / ... | String | valores já formatados |

Preencher com `JREmptyDataSource(1)`.

## Lib (futuro PR em t3wv/nfse)

Cópia espelhada em `src/main/resources/danfse/danfse.jrxml` (raiz do módulo nfse).
