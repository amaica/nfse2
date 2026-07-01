package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nfe_emissao")
public class NfeEmissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 44)
    private String chave;

    private String serie;

    @Column(nullable = false)
    private long numero;

    @Column(nullable = false, length = 3)
    private String modelo;

    @Column(name = "status_protocolo", length = 5)
    private String statusProtocolo;

    @Column(name = "motivo_protocolo", length = 500)
    private String motivoProtocolo;

    @Column(name = "xml_proc", columnDefinition = "CLOB")
    private String xmlProc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getChave() { return chave; }
    public String getSerie() { return serie; }
    public long getNumero() { return numero; }
    public String getModelo() { return modelo; }
    public String getStatusProtocolo() { return statusProtocolo; }
    public String getMotivoProtocolo() { return motivoProtocolo; }
    public String getXmlProc() { return xmlProc; }
    public java.time.Instant getCreatedAt() { return createdAt; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setChave(String chave) { this.chave = chave; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setNumero(long numero) { this.numero = numero; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setStatusProtocolo(String statusProtocolo) { this.statusProtocolo = statusProtocolo; }
    public void setMotivoProtocolo(String motivoProtocolo) { this.motivoProtocolo = motivoProtocolo; }
    public void setXmlProc(String xmlProc) { this.xmlProc = xmlProc; }
}
