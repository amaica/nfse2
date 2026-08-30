package br.com.synki.nfse.portal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CriarEmpresaRequest(
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
        String ambiente,
        String serieRps,
        Long ultimoNumeroNfse,
        @NotBlank @Email String emailIntegracao,
        @NotBlank String senhaIntegracao,
        String usuarioNome,
        String serieNfe,
        Long ultimoNumeroNfe,
        String serieNfce,
        Long ultimoNumeroNfce,
        Boolean baixarXml,
        List<EnderecoEmpresaRequest> enderecos
) {}
