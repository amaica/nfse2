package br.com.synki.nfse.portal.service;

import java.math.BigDecimal;
import java.time.Instant;

/** Linha da listagem de NF-e, sem o XML. */
public class NfeListaItem {

    private final Long id;
    private final Long empresaId;
    private final String chave;
    private final String serie;
    private final long numero;
    private final String modelo;
    private final String statusProtocolo;
    private final String motivoProtocolo;
    private final String ambiente;
    private final Instant createdAt;
    private final boolean temXml;
    private final BigDecimal valorTotal;
    private final String destinatarioNome;
    private final String cfop;
    private final String tipoOperacao;
    private final String produtosResumo;

    public NfeListaItem(
            Long id,
            Long empresaId,
            String chave,
            String serie,
            Long numero,
            String modelo,
            String statusProtocolo,
            String motivoProtocolo,
            String ambiente,
            Instant createdAt,
            Boolean temXml,
            BigDecimal valorTotal,
            String destinatarioNome,
            String cfop,
            String tipoOperacao,
            String produtosResumo) {
        this.id = id;
        this.empresaId = empresaId;
        this.chave = chave;
        this.serie = serie;
        this.numero = numero == null ? 0 : numero;
        this.modelo = modelo;
        this.statusProtocolo = statusProtocolo;
        this.motivoProtocolo = motivoProtocolo;
        this.ambiente = ambiente;
        this.createdAt = createdAt;
        this.temXml = Boolean.TRUE.equals(temXml);
        this.valorTotal = valorTotal;
        this.destinatarioNome = destinatarioNome;
        this.cfop = cfop;
        this.tipoOperacao = tipoOperacao;
        this.produtosResumo = produtosResumo;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getChave() { return chave; }
    public String getSerie() { return serie; }
    public long getNumero() { return numero; }
    public String getModelo() { return modelo; }
    public String getStatusProtocolo() { return statusProtocolo; }
    public String getMotivoProtocolo() { return motivoProtocolo; }
    public String getAmbiente() { return ambiente; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isTemXml() { return temXml; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getDestinatarioNome() { return destinatarioNome; }
    public String getCfop() { return cfop; }
    public String getTipoOperacao() { return tipoOperacao; }
    public String getProdutosResumo() { return produtosResumo; }
}
