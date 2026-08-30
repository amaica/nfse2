package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeReboqueRequest(
        String placa,
        String uf,
        String rntc
) {}
