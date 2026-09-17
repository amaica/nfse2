package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pessoa_endereco")
public class PessoaEndereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pessoa_id", nullable = false)
    private Long pessoaId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "inscricao_estadual", length = 20)
    private String inscricaoEstadual;

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String municipio;

    @Column(length = 2)
    private String uf;

    @Column(length = 8)
    private String cep;

    @Column(name = "codigo_municipio_ibge", length = 7)
    private String codigoMunicipioIbge;

    @Column(nullable = false)
    private boolean principal = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Rótulo estilo Fluxo: IE:xxx - logradouro - cidade */
    @Transient
    public String getValores() {
        var ie = inscricaoEstadual != null && !inscricaoEstadual.isBlank() ? inscricaoEstadual : "—";
        var log = logradouro != null ? logradouro : "";
        var cid = municipio != null ? municipio : "";
        if (uf != null && !uf.isBlank()) {
            cid = cid.isBlank() ? uf : cid + "/" + uf;
        }
        return "IE:" + ie + " - " + log + (cid.isBlank() ? "" : " - " + cid);
    }

    public Long getId() { return id; }
    public Long getPessoaId() { return pessoaId; }
    public Long getEmpresaId() { return empresaId; }
    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public String getCep() { return cep; }
    public String getCodigoMunicipioIbge() { return codigoMunicipioIbge; }
    public boolean isPrincipal() { return principal; }
    public boolean isAtivo() { return ativo; }

    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public void setUf(String uf) { this.uf = uf; }
    public void setCep(String cep) { this.cep = cep; }
    public void setCodigoMunicipioIbge(String codigoMunicipioIbge) { this.codigoMunicipioIbge = codigoMunicipioIbge; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
