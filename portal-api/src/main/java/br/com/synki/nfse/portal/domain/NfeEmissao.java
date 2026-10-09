package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

    /** 1 = produção, 2 = homologação (tpAmb da NF-e). */
    @Column(nullable = false, length = 1)
    private String ambiente = "1";

    @Column(name = "valor_total", precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "destinatario_nome", length = 255)
    private String destinatarioNome;

    @Column(length = 60)
    private String cfop;

    @Column(name = "tipo_operacao", length = 20)
    private String tipoOperacao;

    @Column(name = "produtos_resumo", columnDefinition = "TEXT")
    private String produtosResumo;

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
    public String getAmbiente() { return ambiente; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getDestinatarioNome() { return destinatarioNome; }
    public String getCfop() { return cfop; }
    public String getTipoOperacao() { return tipoOperacao; }
    public String getProdutosResumo() { return produtosResumo; }
    public java.time.Instant getCreatedAt() { return createdAt; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setChave(String chave) { this.chave = chave; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setNumero(long numero) { this.numero = numero; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setStatusProtocolo(String statusProtocolo) { this.statusProtocolo = statusProtocolo; }
    public void setMotivoProtocolo(String motivoProtocolo) { this.motivoProtocolo = motivoProtocolo; }
    public void setXmlProc(String xmlProc) { this.xmlProc = xmlProc; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public void setDestinatarioNome(String destinatarioNome) { this.destinatarioNome = destinatarioNome; }
    public void setCfop(String cfop) { this.cfop = cfop; }
    public void setTipoOperacao(String tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public void setProdutosResumo(String produtosResumo) { this.produtosResumo = produtosResumo; }
}
