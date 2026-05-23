package br.com.synki.nfse.portal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Payload completo do emissor municipal — mapeado para DPS nacional. */
public record EmissaoCompletaRequest(
        @Valid @NotNull Identificacao identificacao,
        @Valid @NotNull Regime regime,
        @Valid Prestador prestador,
        @Valid @NotNull Tomador tomador,
        EnderecoTomador enderecoTomador,
        @Valid @NotNull Servico servico,
        @Valid @NotNull Valores valores,
        RetencoesFederais retencoesFederais,
        TributacaoFederalDetalhada tributacaoFederal,
        IbsCbs ibsCbs,
        ClassificacaoServico classificacao,
        ConstrucaoCivil construcaoCivil,
        Intermediario intermediario,
        InformacoesAdicionais informacoesAdicionais
) {
    public record Identificacao(
            Long numeroRps,
            @NotBlank String serieRps,
            String tipoRps,
            String dataEmissao,
            @NotBlank String competencia
    ) {}

    public record Regime(
            String tributacaoIssqn,
            String regimeEspecialTributacao,
            String simplesNacional,
            String issRetido,
            Boolean incentivoFiscal
    ) {}

    public record Prestador(
            String inscricaoMunicipal
    ) {}

    public record Tomador(
            @NotBlank String documento,
            @NotBlank String razaoSocial,
            String email,
            String telefone,
            String inscricaoEstadual,
            String inscricaoMunicipal
    ) {}

    public record EnderecoTomador(
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            String codigoMunicipioIbge,
            String pais
    ) {}

    public record ClassificacaoServico(String atividadePrincipal) {}

    public record Servico(
            @NotBlank String itemListaServico,
            String codigoTributacaoMunicipio,
            String cnae,
            String nbs,
            @NotBlank String descricaoServico,
            String municipioIncidencia,
            String municipioPrestacao,
            String localPrestacao
    ) {}

    public record Valores(
            @NotNull BigDecimal valorServicos,
            BigDecimal deducoes,
            BigDecimal descontoIncondicionado,
            BigDecimal descontoCondicionado,
            BigDecimal baseCalculo,
            BigDecimal aliquota,
            BigDecimal valorIss,
            BigDecimal valorLiquidoNfse,
            String responsavelRetencaoIss
    ) {}

    public record TributacaoFederalDetalhada(
            String cstPisCofins,
            String tipoRetencaoPisCofins,
            BigDecimal baseCalculoPisCofins,
            BigDecimal aliquotaPis,
            BigDecimal aliquotaCofins,
            BigDecimal valorPis,
            BigDecimal valorCofins,
            BigDecimal retencaoIrrf,
            BigDecimal retencaoCsll,
            BigDecimal retencaoIss,
            BigDecimal retencaoInss,
            Boolean habilitarRetencoes
    ) {}

    public record IbsCbs(
            String cst,
            String classificacaoTributaria,
            String classificacaoOperacao,
            BigDecimal baseCalculo,
            BigDecimal aliquotaIbs,
            BigDecimal aliquotaCbs,
            BigDecimal reducaoIbs,
            BigDecimal reducaoCbs,
            BigDecimal valorIbs,
            BigDecimal valorCbs,
            BigDecimal valorTotal,
            Boolean habilitar
    ) {}

    public record RetencoesFederais(
            BigDecimal pis,
            BigDecimal cofins,
            BigDecimal inss,
            BigDecimal ir,
            BigDecimal csll,
            BigDecimal outrasRetencoes
    ) {}

    public record ConstrucaoCivil(
            String codigoObra,
            String art
    ) {}

    public record Intermediario(
            String documento,
            String razaoSocial,
            String inscricaoMunicipal
    ) {}

    public record InformacoesAdicionais(
            String observacoes,
            String informacoesComplementares
    ) {}
}
