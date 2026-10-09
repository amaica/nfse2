package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeAjustarNumeracaoRequest(
        Long enderecoId,
        String serie,
        Long ultimoNumero,
        Long proximoNumero) {
}
