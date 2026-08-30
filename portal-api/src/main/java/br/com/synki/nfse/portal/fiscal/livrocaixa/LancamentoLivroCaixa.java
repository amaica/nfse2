package br.com.synki.nfse.portal.fiscal.livrocaixa;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoLivroCaixa(
        LocalDate data,
        String origem,
        String numeroDocumento,
        String chaveAcesso,
        String historico,
        String contraparteNome,
        String contraparteDoc,
        BigDecimal valor,
        TipoMovimento tipoMovimento
) {
    public enum TipoMovimento {
        RECEITA,
        DESPESA
    }
}
