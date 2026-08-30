package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(name = "descricao_pdv", length = 120)
    private String descricaoPdv;

    @Column(length = 14)
    private String gtin;

    @Column(name = "codigo_ncm", length = 8)
    private String codigoNcm;

    @Column(length = 7)
    private String cest;

    @Column(name = "ex_tipi", length = 3)
    private String exTipi;

    @Column(length = 6, nullable = false)
    private String unidade = "UN";

    /** Origem da mercadoria (0–8), padrão NF-e. */
    @Column(length = 1, columnDefinition = "varchar(1)")
    private String origem = "0";

    /** P = produto / mercadoria, S = serviço. */
    @Column(length = 1, nullable = false, columnDefinition = "varchar(1)")
    private String tipo = "P";

    @Column(name = "valor_unitario", precision = 15, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "valor_custo", precision = 15, scale = 4)
    private BigDecimal valorCusto;

    /** Markup sobre o custo, em percentual (ex.: 35 = 35%). */
    @Column(precision = 7, scale = 2)
    private BigDecimal markup;

    @Column(precision = 15, scale = 4)
    private BigDecimal peso;

    @Column(name = "estoque_minimo", precision = 15, scale = 4)
    private BigDecimal estoqueMinimo;

    @Column(name = "estoque_atual", precision = 15, scale = 4)
    private BigDecimal estoqueAtual;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "grupo_tributario_id")
    private Long grupoTributarioId;

    @Column(name = "grupo_id")
    private Long grupoId;

    @Column(name = "subgrupo_id")
    private Long subgrupoId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getDescricaoPdv() { return descricaoPdv; }
    public String getGtin() { return gtin; }
    public String getCodigoNcm() { return codigoNcm; }
    public String getCest() { return cest; }
    public String getExTipi() { return exTipi; }
    public String getUnidade() { return unidade; }
    public String getOrigem() { return origem; }
    public String getTipo() { return tipo; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public BigDecimal getValorCusto() { return valorCusto; }
    public BigDecimal getMarkup() { return markup; }
    public BigDecimal getPeso() { return peso; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public BigDecimal getEstoqueAtual() { return estoqueAtual; }
    public String getObservacoes() { return observacoes; }
    public Long getGrupoTributarioId() { return grupoTributarioId; }
    public Long getGrupoId() { return grupoId; }
    public Long getSubgrupoId() { return subgrupoId; }
    public boolean isAtivo() { return ativo; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricaoPdv(String descricaoPdv) { this.descricaoPdv = descricaoPdv; }
    public void setGtin(String gtin) { this.gtin = gtin; }
    public void setCodigoNcm(String codigoNcm) { this.codigoNcm = codigoNcm; }
    public void setCest(String cest) { this.cest = cest; }
    public void setExTipi(String exTipi) { this.exTipi = exTipi; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public void setOrigem(String origem) { this.origem = origem; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public void setValorCusto(BigDecimal valorCusto) { this.valorCusto = valorCusto; }
    public void setMarkup(BigDecimal markup) { this.markup = markup; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
    public void setEstoqueMinimo(BigDecimal estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public void setEstoqueAtual(BigDecimal estoqueAtual) { this.estoqueAtual = estoqueAtual; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public void setGrupoTributarioId(Long grupoTributarioId) { this.grupoTributarioId = grupoTributarioId; }
    public void setGrupoId(Long grupoId) { this.grupoId = grupoId; }
    public void setSubgrupoId(Long subgrupoId) { this.subgrupoId = subgrupoId; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
