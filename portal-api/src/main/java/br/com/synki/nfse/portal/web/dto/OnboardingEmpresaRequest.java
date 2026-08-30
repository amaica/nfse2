package br.com.synki.nfse.portal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OnboardingEmpresaRequest(
        @NotBlank String cnpj,
        @NotBlank String nome,
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
        String situacaoCadastral,
        @NotBlank String prefeitura,
        @NotBlank @Pattern(regexp = "\\d{7}") String codigoMunicipioIbge,
        String ambiente
) {}
