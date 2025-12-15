package io.github.t3wv.nacional.webservices;

import io.github.t3wv.NFSeConfig;
import io.github.t3wv.NFSeHttpClient;
import io.github.t3wv.NFSeLogger;
import io.github.t3wv.NFSeObjectMapper;
import io.github.t3wv.nacional.classes.nfsenacional.*;
import io.github.t3wv.utils.NFSeAssinaturaDigital;
import io.github.t3wv.utils.NFSeUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class WSSefinNFSe implements NFSeLogger {

    public static final String URL_BASE_PRODUCAO = "https://sefin.nfse.gov.br/sefinnacional";
    public static final String URL_BASE_HOMOLOGACAO = "https://sefin.producaorestrita.nfse.gov.br/sefinnacional";
    private static final String URL_PRODUCAO_NFSE = URL_BASE_PRODUCAO + "/nfse";
    private static final String URL_HOMOLOGACAO_NFSE = URL_BASE_HOMOLOGACAO + "/nfse";

    private final NFSeObjectMapper objectMapper = new NFSeObjectMapper();
    private final NFSeConfig config;

     WSSefinNFSe(final NFSeConfig config) {
        this.config = config;
    }

    /**
     * Consulta NFSe pela chave de acesso
     *
     * @param chaveAcesso chave de acesso da nfse
     * @return objeto de resposta da consulta
     * @throws Exception
     */
    NFSeSefinNacionalGetResponse buscarNFSeByChaveAcesso(final String chaveAcesso) throws Exception {
        //normaliza a chave de acesso removendo quaisquer caracteres não numéricos
        final var chaveAcessoNormalizada = chaveAcesso.replaceAll("\\D", "");

        //valida o tamanho da chave de acesso, pois precisa ser exatamente 50 caracteres numericos
        if (!chaveAcessoNormalizada.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso da NFSe deve conter exatamente 50 dígitos numéricos!");
        }

        //busca os dados
        final var url = new URI(String.format("%s/%s", config.isTeste() ? URL_HOMOLOGACAO_NFSE : URL_PRODUCAO_NFSE, chaveAcesso));
        final var response = new NFSeHttpClient(config).sendGetRequest(url);
        this.getLogger().info("Response: {}", response);
        if (response.statusCode() != 200) {
            throw new Exception("Consulta de XML do NFSe '%s' retornou erro '%d'!".formatted(chaveAcessoNormalizada, response.statusCode()));
        } else {
            return this.objectMapper.convertValue(this.objectMapper.readTree(response.body()), NFSeSefinNacionalGetResponse.class);
        }
    }

    String buscarNFSeXmlByChaveAcesso(final String nfseChaveAcesso) throws Exception {
        final var nfse = this.buscarNFSeByChaveAcesso(nfseChaveAcesso);
        final byte[] conteudo = Base64.getDecoder().decode(nfse.getNfseXmlGZipB64());//java 8
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(conteudo))) {
            try (BufferedReader bf = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
                StringBuilder outStr = new StringBuilder();
                String line;
                while ((line = bf.readLine()) != null) {
                    outStr.append(line);
                }
                return outStr.toString();
            }
        }
    }


    /**
     * Emite NFSe a partir do DPS
     */
    public NFSeSefinNacionalPostResponseSucesso emitirNFSeByDPS(final NFSeSefinNacionalDPS dps) throws Exception {
        // Gera o id e seta no objeto para preenchimento no xml
        dps.getInfDPS().setId(NFSeUtils.gerarDPSId(dps));

        byte[] gzipped;
        try (var baos = new ByteArrayOutputStream(); var gos = new java.util.zip.GZIPOutputStream(baos)) {
            gos.write(new NFSeAssinaturaDigital(this.config).setOmitirDeclaracaoXML(false).setUsarIdComoReferencia(false).assinarDocumento(dps.toXml()).getBytes(StandardCharsets.UTF_8));
            gos.finish();
            gzipped = baos.toByteArray();
        }
        ;

        final var uri = new URI(String.format("%s", config.isTeste() ? URL_HOMOLOGACAO_NFSE : URL_PRODUCAO_NFSE));
        final var body = String.format("{dpsXmlGZipB64:\"%s\"}", Base64.getEncoder().encodeToString(gzipped));
        final var response = new NFSeHttpClient(config).sendPostRequest(uri, body);
        if (response.statusCode() != 201) {
            final var responseError = this.objectMapper.convertValue(this.objectMapper.readTree(response.body()), NFSeSefinNacionalPostResponseErro.class);
            throw new IllegalStateException(String.format("Erro ao enviar DPS. Erros: %s", responseError.getErros()));
        }

        return this.objectMapper.convertValue(this.objectMapper.readTree(response.body()), NFSeSefinNacionalPostResponseSucesso.class);
    }

    /**
     * Emite um evento de cancelamento para uma NFSe
     */
    public NFSeSefinNacionalPostResponseSucesso enviarPedidoRegistroEvento(final NFSeSefinNacionalPedRegEvt pedidoRegistroEvento) throws Exception {
        // Gera o id e seta no objeto para preenchimento no xml
        pedidoRegistroEvento.getInfPedReg().setId(NFSeUtils.gerarEventoId(pedidoRegistroEvento));

        byte[] gzipped;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(baos)) {
            gos.write(new NFSeAssinaturaDigital(this.config).setOmitirDeclaracaoXML(false).setUsarIdComoReferencia(false).assinarDocumento(pedidoRegistroEvento.toXml(), "infPedReg").getBytes(StandardCharsets.UTF_8));
            gos.finish();
            gzipped = baos.toByteArray();
        }

        final var uri = new URI(String.format("%s/%s/eventos", config.isTeste() ? URL_HOMOLOGACAO_NFSE : URL_PRODUCAO_NFSE, pedidoRegistroEvento.getInfPedReg().getChaveAcessoNFSE()));
        final var body = String.format("{pedidoRegistroEventoXmlGZipB64:\"%s\"}", Base64.getEncoder().encodeToString(gzipped));
        final var response = new NFSeHttpClient(config).sendPostRequest(uri, body);

        if (response.statusCode() != 201) {
            final var responseError = this.objectMapper.convertValue(this.objectMapper.readTree(response.body()), NFSeSefinNacionalPostResponseErro.class);
            throw new IllegalStateException(String.format("Erro ao enviar pedido de registro de evento. Erros: %s", responseError.getErros()));
        }

        return this.objectMapper.convertValue(this.objectMapper.readTree(response.body()), NFSeSefinNacionalPostResponseSucesso.class);
    }
}
