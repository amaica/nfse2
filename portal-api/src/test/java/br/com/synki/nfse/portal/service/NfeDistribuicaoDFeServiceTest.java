package br.com.synki.nfse.portal.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NfeDistribuicaoDFeServiceTest {

    @Test
    void padNsuCompletaQuinzeDigitos() {
        assertEquals("000000000000000", NfeDistribuicaoDFeService.padNsu(null));
        assertEquals("000000000000000", NfeDistribuicaoDFeService.padNsu(""));
        assertEquals("000000000085877", NfeDistribuicaoDFeService.padNsu("85877"));
        assertEquals("000000000085877", NfeDistribuicaoDFeService.padNsu("000000000085877"));
    }

    @Test
    void decodeGzipDescompactaBase64() throws Exception {
        var original = "<procNFe><chNFe>123</chNFe></procNFe>";
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed)) {
            gzip.write(original.getBytes(StandardCharsets.UTF_8));
        }
        var encoded = Base64.getEncoder().encodeToString(compressed.toByteArray());
        assertEquals(original, NfeDistribuicaoDFeService.decodeGzip(encoded));
    }
}
