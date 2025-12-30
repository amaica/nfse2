package io.github.t3wv.nacional;

import io.github.t3wv.NFSeConfig;
import io.github.t3wv.NFSeLogger;
import io.github.t3wv.utils.NFSeHttpClient;

import java.net.URI;
import java.net.http.HttpResponse;

/**
 * Classe responsável por interagir com o serviço de download do DANFSe.
 */
public class WSDANFSe implements NFSeLogger {

    public static final String URL_BASE_PRODUCAO = "https://adn.nfse.gov.br";
    public static final String URL_BASE_HOMOLOGACAO = "https://adn.producaorestrita.nfse.gov.br";


    private final NFSeConfig config;

    public WSDANFSe(final NFSeConfig config) {
        this.config = config;
    }

    /**
     * Faz o download do DANFSe em PDF utilizando a chave de acesso da NFSe.
     *
     * @param chaveAcesso Chave de acesso da NFSe com 50 dígitos.
     * @return Array de bytes representando o arquivo PDF do DANFSe.
     * @throws Exception Se ocorrer um erro durante a requisição ou no processamento da resposta.
     */
    public byte[] downloadDANFSePdfByChaveAcesso(final String chaveAcesso) throws Exception {
        //normaliza a chave de acesso removendo quaisquer caracteres não numéricos
        final var chaveAcessoNormalizada = chaveAcesso.replaceAll("\\D", "");

        //valida o tamanho da chave de acesso, pois precisa ser exatamente 50 caracteres numericos
        if (!chaveAcessoNormalizada.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso da NFSe deve conter exatamente 50 dígitos numéricos!");
        }

        //monta a URL de consulta do DANFSe
        final var url = new URI(String.format("%s/danfse/%s", this.config.isTeste() ? URL_BASE_HOMOLOGACAO : URL_BASE_PRODUCAO, chaveAcessoNormalizada));
        final var response = new NFSeHttpClient(this.config).sendGetRequest(url, HttpResponse.BodyHandlers.ofByteArray());
        this.getLogger().info("Response: {}", response);
        if (response.statusCode() != 200) {
            throw new Exception("Consulta de DANFSe '%s' retornou erro '%d'!".formatted(chaveAcessoNormalizada, response.statusCode()));
        } else {
            return response.body();
        }
    }
}
