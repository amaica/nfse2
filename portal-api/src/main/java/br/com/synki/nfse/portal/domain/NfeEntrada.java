package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "nfe_entrada")
public class NfeEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 44)
    private String chave;

    @Column(length = 15)
    private String nsu;

    @Column(name = "schema_xml", length = 80)
    private String schemaXml;

    @Column(name = "cnpj_emitente", length = 14)
    private String cnpjEmitente;

    @Column(name = "nome_emitente")
    private String nomeEmitente;

    @Column(length = 20)
    private String numero;

    @Column(length = 10)
    private String serie;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    private String natureza;

    @Column(precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String xml;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getChave() { return chave; }
    public String getNsu() { return nsu; }
    public String getSchemaXml() { return schemaXml; }
    public String getCnpjEmitente() { return cnpjEmitente; }
    public String getNomeEmitente() { return nomeEmitente; }
    public String getNumero() { return numero; }
    public String getSerie() { return serie; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public String getNatureza() { return natureza; }
    public BigDecimal getValor() { return valor; }
    public String getXml() { return xml; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setChave(String chave) { this.chave = chave; }
    public void setNsu(String nsu) { this.nsu = nsu; }
    public void setSchemaXml(String schemaXml) { this.schemaXml = schemaXml; }
    public void setCnpjEmitente(String cnpjEmitente) { this.cnpjEmitente = cnpjEmitente; }
    public void setNomeEmitente(String nomeEmitente) { this.nomeEmitente = nomeEmitente; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
    public void setNatureza(String natureza) { this.natureza = natureza; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public void setXml(String xml) { this.xml = xml; }
}
