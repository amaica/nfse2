package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeDestinatarioRequest(
        String nome,
        String documento,
        String email,
        String inscricaoEstadual
) {}
