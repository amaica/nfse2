# DANFSe Jasper (NT 008/2026)

A lib gera o PDF do DANFSe localmente (API ADN suspensa).

## Uso

```java
WSFacade facade = new WSFacade(config);

// Baixa XML na SEFIN e gera PDF via Jasper
byte[] pdf = facade.downloadNotaPdf(chave50);

// Ou a partir de XML ja em mao
byte[] pdf2 = facade.gerarDanfsePdfDeXml(xml);
// equivalente:
byte[] pdf3 = DANFSeJasper.gerarPdfDeXml(xml);
```

## Interno

| Classe | Papel |
|--------|--------|
| `WSFacade.downloadNotaPdf` | XML SEFIN → Jasper PDF |
| `DANFSeJasper` | Compila/preenche `danfse.jrxml` |
| `DANFSeXmlMapper` | XML → parameters do report |
| `resources/danfse/danfse.jrxml` | Layout |

`WSDANFSe` (ADN) permanece apenas em `downloadNotaPdfAdn` (deprecated).
