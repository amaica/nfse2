package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeInutilizarRequest(
        Integer ano,
        String serie,
        String numeroInicial,
        String numeroFinal,
        String justificativa
) {}
