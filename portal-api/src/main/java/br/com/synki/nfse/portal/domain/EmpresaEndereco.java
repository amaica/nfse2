package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "empresa_endereco")
public class EmpresaEndereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 80)
    private String apelido;

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String municipio;
    private String uf;

    @Column(name = "codigo_municipio_ibge", length = 7)
    private String codigoMunicipioIbge;

    @Column(name = "inscricao_estadual", length = 20)
    private String inscricaoEstadual;

    @Column(name = "serie_nfe", nullable = false, length = 10)
    private String serieNfe = "1";

    @Column(name = "ultimo_numero_nfe", nullable = false)
    private long ultimoNumeroNfe = 0;

    @Column(nullable = false)
    private boolean principal = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getApelido() { return apelido; }
    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public String getCodigoMunicipioIbge() { return codigoMunicipioIbge; }
    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public String getSerieNfe() { return serieNfe; }
    public long getUltimoNumeroNfe() { return ultimoNumeroNfe; }
    public boolean isPrincipal() { return principal; }
    public boolean isAtivo() { return ativo; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public void setCep(String cep) { this.cep = cep; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public void setUf(String uf) { this.uf = uf; }
    public void setCodigoMunicipioIbge(String codigoMunicipioIbge) { this.codigoMunicipioIbge = codigoMunicipioIbge; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }
    public void setSerieNfe(String serieNfe) { this.serieNfe = serieNfe; }
    public void setUltimoNumeroNfe(long ultimoNumeroNfe) { this.ultimoNumeroNfe = ultimoNumeroNfe; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
