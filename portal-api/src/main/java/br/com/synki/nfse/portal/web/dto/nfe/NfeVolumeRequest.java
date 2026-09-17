package br.com.synki.nfse.portal.web.dto.nfe;

import java.math.BigDecimal;

public record NfeVolumeRequest(
        Integer quantidade,
        String especie,
        String marca,
        String numeracao,
        BigDecimal pesoLiquido,
        BigDecimal pesoBruto
) {}
