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
        TipoMovimento tipoMovimento,
        /** tpNF: "0" entrada, "1" saída; vazio para NFS-e ou quando não aplicável. */
        String tipoNota
) {
    public enum TipoMovimento {
        RECEITA,
        DESPESA
    }

    public LancamentoLivroCaixa(
            LocalDate data,
            String origem,
            String numeroDocumento,
            String chaveAcesso,
            String historico,
            String contraparteNome,
            String contraparteDoc,
            BigDecimal valor,
            TipoMovimento tipoMovimento) {
        this(data, origem, numeroDocumento, chaveAcesso, historico, contraparteNome, contraparteDoc,
                valor, tipoMovimento, "");
    }
}
