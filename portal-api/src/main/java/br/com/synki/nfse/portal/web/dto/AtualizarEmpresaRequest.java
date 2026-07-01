package br.com.synki.nfse.portal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record AtualizarEmpresaRequest(
        String nome,
        Boolean ativo,
        String nomeFantasia,
        String email,
        String telefone,
        String inscricaoEstadual,
        String inscricaoMunicipal,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String municipio,
        String uf,
        String cnaePrincipal,
        String cnaePrincipalDescricao,
        Boolean optanteSimples,
        String prefeitura,
        @Pattern(regexp = "\\d{7}") String codigoMunicipioIbge,
        String ambiente,
        String serieRps,
        Long ultimoNumeroNfse,
        String serieNfe,
        Long ultimoNumeroNfe,
        String serieNfce,
        Long ultimoNumeroNfce,
        String senhaIntegracao,
        List<EnderecoEmpresaRequest> enderecos
) {}
