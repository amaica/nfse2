# NFS-e

Lib para emissão de notas fiscais de serviço brasileira.

## Geração dos CACERTS

``` java
[NFSeCadeiaCertificadosTest] (src/test/java/io/github/t3wv/utils/NFSeCadeiaCertificadosTest.java);
```

## Configuração das variáveis para execução dos testes

```
CADEIA_CERTIFICADOS_PATH=/tmp/cacerts.jks;
CADEIA_CERTIFICADOS_SENHA=senha;
CERTIFICADO_PATH=/tmp/certificado.pfx;
CERTIFICADO_SENHA=senha;
```

## Consultar nota emitida
``` java
final var chave = "0000..."; //chave de acesso (50 caracteres)
final var facade = new WSFacade(config);

//PDF
final var pdf = facade.downloadNotaPdf(chave);
Files.write(Paths.get("/tmp/%s.pdf".formatted(chave)), pdf);

//XML
final var xml = facade.downloadNotaXml(chave);
Files.writeString(Paths.get("/tmp/%s.xml".formatted(chave)), xml);
```

