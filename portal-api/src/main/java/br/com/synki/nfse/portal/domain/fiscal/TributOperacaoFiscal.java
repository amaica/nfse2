package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tribut_operacao_fiscal")
public class TributOperacaoFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "tipo_operacao", length = 1)
    private String tipoOperacao;

    @Column(name = "gera_financeiro", length = 1)
    private String geraFinanceiro = "S";

    @Column(name = "movimenta_estoque", length = 1)
    private String movimentaEstoque = "S";

    @Column(name = "descricao_na_nf")
    private String descricaoNaNf;

    private Integer cfop;

    @Column(length = 1000)
    private String observacao;

    @Column(length = 1)
    private String principal = "N";

    private String finalidade;

    @Column(name = "finalidade_operacao", length = 2)
    private String finalidadeOperacao;

    @Column(name = "c_mun_fg_ibs", length = 7)
    private String cMunFGIBS;

    @Column(name = "tp_nf_debito", length = 2)
    private String tpNFDebito;

    @Column(name = "tp_nf_credito", length = 2)
    private String tpNFCredito;

    @Column(name = "tp_ente_gov", length = 1)
    private String tpEnteGov;

    @Column(name = "p_redutor", precision = 15, scale = 4)
    private BigDecimal pRedutor;

    @Column(name = "tp_oper_gov", length = 1)
    private String tpOperGov;

    @Column(name = "ind_intermed", length = 1)
    private String indIntermed = "0";

    @Column(name = "ibs_cbs_cst", length = 3)
    private String ibsCbsCst = "000";

    @Column(name = "ibs_cbs_class_trib", length = 6)
    private String ibsCbsClassTrib = "000001";

    @Column(name = "aliquota_ibs_uf", precision = 7, scale = 4)
    private BigDecimal aliquotaIbsUf = new BigDecimal("0.0090");

    @Column(name = "aliquota_ibs_mun", precision = 7, scale = 4)
    private BigDecimal aliquotaIbsMun = new BigDecimal("0.0010");

    @Column(name = "aliquota_cbs", precision = 7, scale = 4)
    private BigDecimal aliquotaCbs = new BigDecimal("0.0100");

    @Column(name = "habilitar_ibs_cbs", nullable = false)
    private boolean habilitarIbsCbs = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getDescricao() { return descricao; }
    public String getTipoOperacao() { return tipoOperacao; }
    public String getGeraFinanceiro() { return geraFinanceiro; }
    public String getMovimentaEstoque() { return movimentaEstoque; }
    public String getDescricaoNaNf() { return descricaoNaNf; }
    public Integer getCfop() { return cfop; }
    public String getObservacao() { return observacao; }
    public String getPrincipal() { return principal; }
    public String getFinalidade() { return finalidade; }
    public String getFinalidadeOperacao() { return finalidadeOperacao; }
    public String getCMunFGIBS() { return cMunFGIBS; }
    public String getTpNFDebito() { return tpNFDebito; }
    public String getTpNFCredito() { return tpNFCredito; }
    public String getTpEnteGov() { return tpEnteGov; }
    public BigDecimal getPRedutor() { return pRedutor; }
    public String getTpOperGov() { return tpOperGov; }
    public String getIndIntermed() { return indIntermed; }
    public String getIbsCbsCst() { return ibsCbsCst; }
    public String getIbsCbsClassTrib() { return ibsCbsClassTrib; }
    public BigDecimal getAliquotaIbsUf() { return aliquotaIbsUf; }
    public BigDecimal getAliquotaIbsMun() { return aliquotaIbsMun; }
    public BigDecimal getAliquotaCbs() { return aliquotaCbs; }
    public boolean isHabilitarIbsCbs() { return habilitarIbsCbs; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setTipoOperacao(String tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public void setGeraFinanceiro(String geraFinanceiro) { this.geraFinanceiro = geraFinanceiro; }
    public void setMovimentaEstoque(String movimentaEstoque) { this.movimentaEstoque = movimentaEstoque; }
    public void setDescricaoNaNf(String descricaoNaNf) { this.descricaoNaNf = descricaoNaNf; }
    public void setCfop(Integer cfop) { this.cfop = cfop; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public void setPrincipal(String principal) { this.principal = principal; }
    public void setFinalidade(String finalidade) { this.finalidade = finalidade; }
    public void setFinalidadeOperacao(String finalidadeOperacao) { this.finalidadeOperacao = finalidadeOperacao; }
    public void setCMunFGIBS(String cMunFGIBS) { this.cMunFGIBS = cMunFGIBS; }
    public void setTpNFDebito(String tpNFDebito) { this.tpNFDebito = tpNFDebito; }
    public void setTpNFCredito(String tpNFCredito) { this.tpNFCredito = tpNFCredito; }
    public void setTpEnteGov(String tpEnteGov) { this.tpEnteGov = tpEnteGov; }
    public void setPRedutor(BigDecimal pRedutor) { this.pRedutor = pRedutor; }
    public void setTpOperGov(String tpOperGov) { this.tpOperGov = tpOperGov; }
    public void setIndIntermed(String indIntermed) { this.indIntermed = indIntermed; }
    public void setIbsCbsCst(String ibsCbsCst) { this.ibsCbsCst = ibsCbsCst; }
    public void setIbsCbsClassTrib(String ibsCbsClassTrib) { this.ibsCbsClassTrib = ibsCbsClassTrib; }
    public void setAliquotaIbsUf(BigDecimal aliquotaIbsUf) { this.aliquotaIbsUf = aliquotaIbsUf; }
    public void setAliquotaIbsMun(BigDecimal aliquotaIbsMun) { this.aliquotaIbsMun = aliquotaIbsMun; }
    public void setAliquotaCbs(BigDecimal aliquotaCbs) { this.aliquotaCbs = aliquotaCbs; }
    public void setHabilitarIbsCbs(boolean habilitarIbsCbs) { this.habilitarIbsCbs = habilitarIbsCbs; }
}
