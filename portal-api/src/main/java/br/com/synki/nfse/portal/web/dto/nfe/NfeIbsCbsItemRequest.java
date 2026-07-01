package br.com.synki.nfse.portal.web.dto.nfe;

import java.math.BigDecimal;

public record NfeIbsCbsItemRequest(
        String cst,
        String classificacaoTributaria,
        BigDecimal aliquotaIbsUf,
        BigDecimal aliquotaIbsMun,
        BigDecimal aliquotaCbs,
        Boolean habilitar
) {}
