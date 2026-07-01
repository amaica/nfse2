package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeCancelarRequest(
        String chave,
        String protocolo,
        String motivo
) {}
