package br.com.synki.nfse.portal.web.dto.nfe;

import java.math.BigDecimal;

public record NfeItemRequest(
        Long produtoId,
        String codigo,
        String descricao,
        String ncm,
        String cfop,
        String unidade,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        NfeIbsCbsItemRequest ibsCbs
) {
    public NfeItemRequest(
            String codigo,
            String descricao,
            String ncm,
            String cfop,
            String unidade,
            BigDecimal quantidade,
            BigDecimal valorUnitario) {
        this(null, codigo, descricao, ncm, cfop, unidade, quantidade, valorUnitario, null);
    }
}
