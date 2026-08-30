package br.com.synki.nfse.portal.web.dto.nfe;

public record NfePagamentoRequest(
        String meioPagamento,
        String indicadorPagamento
) {}
