package br.com.synki.nfse.portal.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linha da listagem de NF-e de entrada, sem o XML. */
public class NfeEntradaListaItem {

    private final Long id;
    private final Long empresaId;
    private final String chave;
    private final String nsu;
    private final String numero;
    private final String serie;
    private final String nomeEmitente;
    private final String cnpjEmitente;
    private final LocalDate dataEmissao;
    private final String natureza;
    private final BigDecimal valor;
    private final boolean temXml;

    public NfeEntradaListaItem(
            Long id,
            Long empresaId,
            String chave,
            String nsu,
            String numero,
            String serie,
            String nomeEmitente,
            String cnpjEmitente,
            LocalDate dataEmissao,
            String natureza,
            BigDecimal valor,
            Boolean temXml) {
        this.id = id;
        this.empresaId = empresaId;
        this.chave = chave;
        this.nsu = nsu;
        this.numero = numero;
        this.serie = serie;
        this.nomeEmitente = nomeEmitente;
        this.cnpjEmitente = cnpjEmitente;
        this.dataEmissao = dataEmissao;
        this.natureza = natureza;
        this.valor = valor;
        this.temXml = Boolean.TRUE.equals(temXml);
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getChave() { return chave; }
    public String getNsu() { return nsu; }
    public String getNumero() { return numero; }
    public String getSerie() { return serie; }
    public String getNomeEmitente() { return nomeEmitente; }
    public String getCnpjEmitente() { return cnpjEmitente; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public String getNatureza() { return natureza; }
    public BigDecimal getValor() { return valor; }
    public boolean isTemXml() { return temXml; }
}
