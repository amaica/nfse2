package br.com.synki.nfse.portal.web.dto.nfe;

import java.math.BigDecimal;
import java.util.List;

public record NfeTransporteRequest(
        String modalidadeFrete,
        String transportadorNome,
        String transportadorDocumento,
        String transportadorIe,
        String transportadorMunicipio,
        String transportadorUf,
        String placa,
        String placaUf,
        String rntc,
        Integer volumeQuantidade,
        String volumeEspecie,
        String volumeMarca,
        String volumeNumeracao,
        BigDecimal pesoLiquido,
        BigDecimal pesoBruto,
        BigDecimal valorFrete,
        List<NfeReboqueRequest> reboques
) {}
