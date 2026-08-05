package io.github.t3wv.nfse.nacional.danfse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Testes do gerador local de DANFSe (Jasper) — NT 008/2026.
 */
public class DANFSeJasperTest {

    @Test
    public void deveGerarPdfProducaoSemAvisoSemValidade() throws Exception {
        final String xml = resource("danfse/nfse-producao-indumavi.xml");
        final byte[] pdf = DANFSeJasper.gerarPdfDeXml(xml);

        Assertions.assertTrue(pdf.length > 1000, "PDF muito pequeno");
        Assertions.assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        final Map<String, Object> params = DANFSeXmlMapper.fromXml(xml);
        Assertions.assertEquals("1", params.get("TP_AMB"));
        Assertions.assertEquals("Producao", params.get("AMBIENTE_LABEL"));
        Assertions.assertEquals("Autorizada", params.get("SITUACAO"));
        Assertions.assertNotNull(params.get("QR_CODE"));
        Assertions.assertTrue(params.get("CHAVE_FORMATADA").toString().contains("4310009225"));
    }

    @Test
    public void deveGerarPdfHomologacaoComParametrosDeAviso() throws Exception {
        final String xml = resource("danfse/nfse-homologacao-indumavi.xml");
        final Map<String, Object> params = DANFSeXmlMapper.fromXml(xml);

        Assertions.assertEquals("2", params.get("TP_AMB"));
        Assertions.assertEquals("Homologacao", params.get("AMBIENTE_LABEL"));

        final byte[] pdf = DANFSeJasper.gerarPdfDeXml(xml);
        Assertions.assertTrue(pdf.length > 1000);
        Assertions.assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        // grava artefato para inspecao visual
        final Path out = Path.of("target", "danfse-homologacao-indumavi.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, pdf);
        Assertions.assertTrue(Files.size(out) > 1000);
    }

    @Test
    public void deveGerarPdfViaFacadeHelper() throws Exception {
        final String xml = resource("danfse/nfse-homologacao-indumavi.xml");
        final byte[] pdf = new io.github.t3wv.nfse.nacional.WSFacade(
                new io.github.t3wv.nfse.NFSeConfigTest()).gerarDanfsePdfDeXml(xml);
        Assertions.assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    public void deveRejeitarXmlVazio() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DANFSeJasper.gerarPdfDeXml(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DANFSeJasper.gerarPdfDeXml(null));
    }

    @Test
    public void deveRejeitarXmlSemChave() {
        final String xml = "<NFSe xmlns=\"http://www.sped.fazenda.gov.br/nfse\"><infNFSe Id=\"X\"/></NFSe>";
        Assertions.assertThrows(IllegalArgumentException.class, () -> DANFSeJasper.gerarPdfDeXml(xml));
    }

    @Test
    public void deveMapearTomadorEValores() throws Exception {
        final Map<String, Object> params = DANFSeXmlMapper.fromXml(resource("danfse/nfse-producao-indumavi.xml"));
        Assertions.assertEquals(Boolean.TRUE, params.get("TOMA_IDENTIFICADO"));
        Assertions.assertTrue(params.get("TOMA_NOME").toString().contains("INDUMAVI"));
        Assertions.assertTrue(params.get("PREST_NOME").toString().toUpperCase().contains("MAICA"));
        Assertions.assertNotNull(params.get("V_SERV"));
        Assertions.assertNotNull(params.get("V_LIQ"));
        Assertions.assertTrue(params.get("RODAPE").toString().contains("NT 008/2026"));
    }

    private static String resource(final String path) throws Exception {
        try (InputStream in = DANFSeJasperTest.class.getClassLoader().getResourceAsStream(path)) {
            Assertions.assertNotNull(in, "resource ausente: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
