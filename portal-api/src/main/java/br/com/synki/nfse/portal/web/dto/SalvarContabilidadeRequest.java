package br.com.synki.nfse.portal.web.dto;

public record SalvarContabilidadeRequest(
        String emailContabilidade,
        boolean envioAutomatico,
        boolean enviarNfse,
        boolean enviarNfe,
        Boolean enviarNfeEntrada
) {}
