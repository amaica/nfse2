package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "nfse_operacao_mensal")
public class NfseOperacaoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "tomador_cnpj", nullable = false, length = 14)
    private String tomadorCnpj;

    @Column(name = "tomador_razao", nullable = false)
    private String tomadorRazao;

    @Column(name = "tomador_email")
    private String tomadorEmail;

    @Column(name = "tomador_telefone", length = 30)
    private String tomadorTelefone;

    @Column(length = 8)
    private String cep;

    @Column(length = 255)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 120)
    private String bairro;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "codigo_municipio_ibge", length = 7)
    private String codigoMunicipioIbge;

    @Column(name = "valor_servicos", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorServicos;

    @Column(name = "descricao_servico", nullable = false, length = 2000)
    private String descricaoServico;

    @Column(name = "item_lista_servico", nullable = false, length = 15)
    private String itemListaServico;

    @Column(length = 9)
    private String nbs;

    @Column(length = 7)
    private String cnae;

    @Column(name = "municipio_prestacao_ibge", length = 7)
    private String municipioPrestacaoIbge;

    @Column(name = "aliquota_iss", precision = 7, scale = 4)
    private BigDecimal aliquotaIss;

    @Column(name = "tributacao_issqn", nullable = false, length = 1)
    private String tributacaoIssqn = "1";

    @Column(name = "iss_retido", nullable = false, length = 1)
    private String issRetido = "1";

    @Column(name = "simples_nacional", nullable = false, length = 1)
    private String simplesNacional = "1";

    @Column(name = "regime_especial", nullable = false, length = 1)
    private String regimeEspecial = "0";

    @Column(name = "serie_rps", nullable = false, length = 5)
    private String serieRps = "1";

    @Column(length = 1000)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultima_emissao_chave", length = 50)
    private String ultimaEmissaoChave;

    @Column(name = "ultima_emissao_em")
    private Instant ultimaEmissaoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getNome() { return nome; }
    public String getTomadorCnpj() { return tomadorCnpj; }
    public String getTomadorRazao() { return tomadorRazao; }
    public String getTomadorEmail() { return tomadorEmail; }
    public String getTomadorTelefone() { return tomadorTelefone; }
    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public String getCodigoMunicipioIbge() { return codigoMunicipioIbge; }
    public BigDecimal getValorServicos() { return valorServicos; }
    public String getDescricaoServico() { return descricaoServico; }
    public String getItemListaServico() { return itemListaServico; }
    public String getNbs() { return nbs; }
    public String getCnae() { return cnae; }
    public String getMunicipioPrestacaoIbge() { return municipioPrestacaoIbge; }
    public BigDecimal getAliquotaIss() { return aliquotaIss; }
    public String getTributacaoIssqn() { return tributacaoIssqn; }
    public String getIssRetido() { return issRetido; }
    public String getSimplesNacional() { return simplesNacional; }
    public String getRegimeEspecial() { return regimeEspecial; }
    public String getSerieRps() { return serieRps; }
    public String getObservacoes() { return observacoes; }
    public boolean isAtivo() { return ativo; }
    public String getUltimaEmissaoChave() { return ultimaEmissaoChave; }
    public Instant getUltimaEmissaoEm() { return ultimaEmissaoEm; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTomadorCnpj(String tomadorCnpj) { this.tomadorCnpj = tomadorCnpj; }
    public void setTomadorRazao(String tomadorRazao) { this.tomadorRazao = tomadorRazao; }
    public void setTomadorEmail(String tomadorEmail) { this.tomadorEmail = tomadorEmail; }
    public void setTomadorTelefone(String tomadorTelefone) { this.tomadorTelefone = tomadorTelefone; }
    public void setCep(String cep) { this.cep = cep; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public void setUf(String uf) { this.uf = uf; }
    public void setCodigoMunicipioIbge(String codigoMunicipioIbge) { this.codigoMunicipioIbge = codigoMunicipioIbge; }
    public void setValorServicos(BigDecimal valorServicos) { this.valorServicos = valorServicos; }
    public void setDescricaoServico(String descricaoServico) { this.descricaoServico = descricaoServico; }
    public void setItemListaServico(String itemListaServico) { this.itemListaServico = itemListaServico; }
    public void setNbs(String nbs) { this.nbs = nbs; }
    public void setCnae(String cnae) { this.cnae = cnae; }
    public void setMunicipioPrestacaoIbge(String municipioPrestacaoIbge) { this.municipioPrestacaoIbge = municipioPrestacaoIbge; }
    public void setAliquotaIss(BigDecimal aliquotaIss) { this.aliquotaIss = aliquotaIss; }
    public void setTributacaoIssqn(String tributacaoIssqn) { this.tributacaoIssqn = tributacaoIssqn; }
    public void setIssRetido(String issRetido) { this.issRetido = issRetido; }
    public void setSimplesNacional(String simplesNacional) { this.simplesNacional = simplesNacional; }
    public void setRegimeEspecial(String regimeEspecial) { this.regimeEspecial = regimeEspecial; }
    public void setSerieRps(String serieRps) { this.serieRps = serieRps; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setUltimaEmissaoChave(String ultimaEmissaoChave) { this.ultimaEmissaoChave = ultimaEmissaoChave; }
    public void setUltimaEmissaoEm(Instant ultimaEmissaoEm) { this.ultimaEmissaoEm = ultimaEmissaoEm; }
}
