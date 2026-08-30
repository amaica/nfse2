package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeReferenciaRequest(
        String tipo,
        String chave,
        String codigoUf,
        String anoMes,
        String cnpj,
        String cpf,
        String inscricaoEstadual,
        String modelo,
        String serie,
        String numero
) {}
