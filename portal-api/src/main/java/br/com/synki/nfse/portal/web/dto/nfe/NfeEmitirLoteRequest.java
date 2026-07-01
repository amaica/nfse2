package br.com.synki.nfse.portal.web.dto.nfe;

import java.util.List;

public record NfeEmitirLoteRequest(
        Long enderecoId,
        Long operacaoFiscalId,
        Boolean sincrono,
        String naturezaOperacao,
        NfeDestinatarioRequest destinatario,
        List<NfeItemRequest> itens
) {}
