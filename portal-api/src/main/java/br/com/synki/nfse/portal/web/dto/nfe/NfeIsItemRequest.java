package br.com.synki.nfse.portal.web.dto.nfe;

import java.math.BigDecimal;

/** Imposto Seletivo (IS) — Reforma Tributária / LC 214. */
public record NfeIsItemRequest(
        Boolean habilitar,
        String cst,
        String classificacaoTributaria,
        BigDecimal aliquota
) {}
