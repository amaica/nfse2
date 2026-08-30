package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 14, unique = true)
    private String cnpj;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    private String email;
    private String telefone;

    @Column(name = "inscricao_estadual", length = 20)
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = 20)
    private String inscricaoMunicipal;

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String municipio;
    private String uf;

    @Column(name = "cnae_principal", length = 7)
    private String cnaePrincipal;

    @Column(name = "cnae_principal_descricao")
    private String cnaePrincipalDescricao;

    @Column(name = "optante_simples", nullable = false)
    private boolean optanteSimples = false;

    @Column(name = "situacao_cadastral", length = 40)
    private String situacaoCadastral;

    @Column(name = "fluxo_legacy_id")
    private Integer fluxoLegacyId;

    /** Se true, o job baixa XMLs de NF-e destinadas a este emitente (despesas / livro caixa). */
    @Column(name = "baixar_xml", nullable = false)
    private boolean baixarXml = false;

    @Column(name = "ultimo_nsu", length = 15)
    private String ultimoNsu;

    @Column(name = "ultimo_nsu_baixado_em")
    private Instant ultimoNsuBaixadoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public boolean isAtivo() { return ativo; }
    public String getNomeFantasia() { return nomeFantasia; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public String getInscricaoMunicipal() { return inscricaoMunicipal; }
    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public String getCnaePrincipal() { return cnaePrincipal; }
    public String getCnaePrincipalDescricao() { return cnaePrincipalDescricao; }
    public boolean isOptanteSimples() { return optanteSimples; }
    public String getSituacaoCadastral() { return situacaoCadastral; }
    public Integer getFluxoLegacyId() { return fluxoLegacyId; }
    public boolean isBaixarXml() { return baixarXml; }
    public String getUltimoNsu() { return ultimoNsu; }
    public Instant getUltimoNsuBaixadoEm() { return ultimoNsuBaixadoEm; }

    public static Empresa criar(String nome, String cnpj) {
        var e = new Empresa();
        e.nome = nome.trim();
        e.cnpj = cnpj.replaceAll("\\D", "");
        e.ativo = true;
        return e;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }
    public void setInscricaoMunicipal(String inscricaoMunicipal) { this.inscricaoMunicipal = inscricaoMunicipal; }
    public void setCep(String cep) { this.cep = cep; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public void setUf(String uf) { this.uf = uf; }
    public void setCnaePrincipal(String cnaePrincipal) { this.cnaePrincipal = cnaePrincipal; }
    public void setCnaePrincipalDescricao(String cnaePrincipalDescricao) { this.cnaePrincipalDescricao = cnaePrincipalDescricao; }
    public void setOptanteSimples(boolean optanteSimples) { this.optanteSimples = optanteSimples; }
    public void setSituacaoCadastral(String situacaoCadastral) { this.situacaoCadastral = situacaoCadastral; }
    public void setFluxoLegacyId(Integer fluxoLegacyId) { this.fluxoLegacyId = fluxoLegacyId; }
    public void setBaixarXml(boolean baixarXml) { this.baixarXml = baixarXml; }
    public void setUltimoNsu(String ultimoNsu) { this.ultimoNsu = ultimoNsu; }
    public void setUltimoNsuBaixadoEm(Instant ultimoNsuBaixadoEm) { this.ultimoNsuBaixadoEm = ultimoNsuBaixadoEm; }
}
