package br.com.synki.nfse.portal.web.dto.nfe;

import java.util.List;

public record NfeContingenciaEpecRequest(
        Long enderecoId,
        String naturezaOperacao,
        NfeDestinatarioRequest destinatario,
        List<NfeItemRequest> itens,
        String justificativaContingencia
) {}
