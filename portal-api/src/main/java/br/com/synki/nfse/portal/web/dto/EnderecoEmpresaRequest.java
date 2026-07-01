package br.com.synki.nfse.portal.web.dto;

public record EnderecoEmpresaRequest(
        Long id,
        String apelido,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String municipio,
        String uf,
        String codigoMunicipioIbge,
        String inscricaoEstadual,
        String serieNfe,
        Long ultimoNumeroNfe,
        Boolean principal,
        Boolean ativo
) {}
