package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeCartaCorrecaoRequest(
        String chave,
        String texto,
        Integer sequencial
) {}
