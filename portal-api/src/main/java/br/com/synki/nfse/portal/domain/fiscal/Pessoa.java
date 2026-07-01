package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String nome;

    @Column(length = 1, nullable = false)
    private String tipo = "J";

    @Column(name = "cpf_cnpj", length = 14)
    private String cpfCnpj;

    private String email;

    @Column(name = "inscricao_estadual", length = 20)
    private String inscricaoEstadual;

    private String logradouro;
    private String numero;
    private String bairro;
    private String municipio;

    @Column(length = 2)
    private String uf;

    @Column(length = 8)
    private String cep;

    @Column(name = "codigo_municipio_ibge", length = 7)
    private String codigoMunicipioIbge;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getEmail() { return email; }
    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getBairro() { return bairro; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public String getCep() { return cep; }
    public String getCodigoMunicipioIbge() { return codigoMunicipioIbge; }
    public boolean isAtivo() { return ativo; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public void setEmail(String email) { this.email = email; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public void setUf(String uf) { this.uf = uf; }
    public void setCep(String cep) { this.cep = cep; }
    public void setCodigoMunicipioIbge(String codigoMunicipioIbge) { this.codigoMunicipioIbge = codigoMunicipioIbge; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
