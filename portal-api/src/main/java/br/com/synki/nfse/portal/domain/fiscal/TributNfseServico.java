package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tribut_nfse_servico")
public class TributNfseServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "item_lista_servico", nullable = false, length = 15)
    private String itemListaServico;

    @Column(name = "codigo_tributacao_municipio", length = 20)
    private String codigoTributacaoMunicipio;

    @Column(length = 9)
    private String nbs;

    @Column(length = 7)
    private String cnae;

    @Column(name = "descricao_servico", length = 2000)
    private String descricaoServico;

    @Column(name = "municipio_prestacao_ibge", length = 7)
    private String municipioPrestacaoIbge;

    @Column(name = "aliquota_iss", precision = 7, scale = 4)
    private BigDecimal aliquotaIss;

    @Column(name = "tributacao_issqn", nullable = false, length = 1)
    private String tributacaoIssqn = "1";

    @Column(name = "iss_retido", nullable = false, length = 1)
    private String issRetido = "1";

    @Column(name = "simples_nacional", nullable = false, length = 1)
    private String simplesNacional = "1";

    @Column(name = "regime_especial", nullable = false, length = 1)
    private String regimeEspecial = "0";

    @Column(name = "cst_pis_cofins", length = 2)
    private String cstPisCofins = "08";

    @Column(name = "aliquota_pis", precision = 7, scale = 4)
    private BigDecimal aliquotaPis;

    @Column(name = "aliquota_cofins", precision = 7, scale = 4)
    private BigDecimal aliquotaCofins;

    @Column(name = "tipo_retencao_pis_cofins", length = 1)
    private String tipoRetencaoPisCofins;

    @Column(name = "habilitar_retencoes", nullable = false)
    private boolean habilitarRetencoes = false;

    @Column(name = "retencao_inss", precision = 15, scale = 2)
    private BigDecimal retencaoInss;

    @Column(name = "retencao_irrf", precision = 15, scale = 2)
    private BigDecimal retencaoIrrf;

    @Column(name = "retencao_csll", precision = 15, scale = 2)
    private BigDecimal retencaoCsll;

    @Column(name = "ibs_cbs_cst", nullable = false, length = 3)
    private String ibsCbsCst = "000";

    @Column(name = "ibs_cbs_class_trib", nullable = false, length = 6)
    private String ibsCbsClassTrib = "000001";

    @Column(name = "aliquota_ibs", nullable = false, precision = 7, scale = 4)
    private BigDecimal aliquotaIbs = new BigDecimal("0.0100");

    @Column(name = "aliquota_cbs", nullable = false, precision = 7, scale = 4)
    private BigDecimal aliquotaCbs = new BigDecimal("0.0100");

    @Column(name = "habilitar_ibs_cbs", nullable = false)
    private boolean habilitarIbsCbs = true;

    @Column(nullable = false)
    private boolean principal = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getDescricao() { return descricao; }
    public String getItemListaServico() { return itemListaServico; }
    public String getCodigoTributacaoMunicipio() { return codigoTributacaoMunicipio; }
    public String getNbs() { return nbs; }
    public String getCnae() { return cnae; }
    public String getDescricaoServico() { return descricaoServico; }
    public String getMunicipioPrestacaoIbge() { return municipioPrestacaoIbge; }
    public BigDecimal getAliquotaIss() { return aliquotaIss; }
    public String getTributacaoIssqn() { return tributacaoIssqn; }
    public String getIssRetido() { return issRetido; }
    public String getSimplesNacional() { return simplesNacional; }
    public String getRegimeEspecial() { return regimeEspecial; }
    public String getCstPisCofins() { return cstPisCofins; }
    public BigDecimal getAliquotaPis() { return aliquotaPis; }
    public BigDecimal getAliquotaCofins() { return aliquotaCofins; }
    public String getTipoRetencaoPisCofins() { return tipoRetencaoPisCofins; }
    public boolean isHabilitarRetencoes() { return habilitarRetencoes; }
    public BigDecimal getRetencaoInss() { return retencaoInss; }
    public BigDecimal getRetencaoIrrf() { return retencaoIrrf; }
    public BigDecimal getRetencaoCsll() { return retencaoCsll; }
    public String getIbsCbsCst() { return ibsCbsCst; }
    public String getIbsCbsClassTrib() { return ibsCbsClassTrib; }
    public BigDecimal getAliquotaIbs() { return aliquotaIbs; }
    public BigDecimal getAliquotaCbs() { return aliquotaCbs; }
    public boolean isHabilitarIbsCbs() { return habilitarIbsCbs; }
    public boolean isPrincipal() { return principal; }
    public boolean isAtivo() { return ativo; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setItemListaServico(String itemListaServico) { this.itemListaServico = itemListaServico; }
    public void setCodigoTributacaoMunicipio(String codigoTributacaoMunicipio) { this.codigoTributacaoMunicipio = codigoTributacaoMunicipio; }
    public void setNbs(String nbs) { this.nbs = nbs; }
    public void setCnae(String cnae) { this.cnae = cnae; }
    public void setDescricaoServico(String descricaoServico) { this.descricaoServico = descricaoServico; }
    public void setMunicipioPrestacaoIbge(String municipioPrestacaoIbge) { this.municipioPrestacaoIbge = municipioPrestacaoIbge; }
    public void setAliquotaIss(BigDecimal aliquotaIss) { this.aliquotaIss = aliquotaIss; }
    public void setTributacaoIssqn(String tributacaoIssqn) { this.tributacaoIssqn = tributacaoIssqn; }
    public void setIssRetido(String issRetido) { this.issRetido = issRetido; }
    public void setSimplesNacional(String simplesNacional) { this.simplesNacional = simplesNacional; }
    public void setRegimeEspecial(String regimeEspecial) { this.regimeEspecial = regimeEspecial; }
    public void setCstPisCofins(String cstPisCofins) { this.cstPisCofins = cstPisCofins; }
    public void setAliquotaPis(BigDecimal aliquotaPis) { this.aliquotaPis = aliquotaPis; }
    public void setAliquotaCofins(BigDecimal aliquotaCofins) { this.aliquotaCofins = aliquotaCofins; }
    public void setTipoRetencaoPisCofins(String tipoRetencaoPisCofins) { this.tipoRetencaoPisCofins = tipoRetencaoPisCofins; }
    public void setHabilitarRetencoes(boolean habilitarRetencoes) { this.habilitarRetencoes = habilitarRetencoes; }
    public void setRetencaoInss(BigDecimal retencaoInss) { this.retencaoInss = retencaoInss; }
    public void setRetencaoIrrf(BigDecimal retencaoIrrf) { this.retencaoIrrf = retencaoIrrf; }
    public void setRetencaoCsll(BigDecimal retencaoCsll) { this.retencaoCsll = retencaoCsll; }
    public void setIbsCbsCst(String ibsCbsCst) { this.ibsCbsCst = ibsCbsCst; }
    public void setIbsCbsClassTrib(String ibsCbsClassTrib) { this.ibsCbsClassTrib = ibsCbsClassTrib; }
    public void setAliquotaIbs(BigDecimal aliquotaIbs) { this.aliquotaIbs = aliquotaIbs; }
    public void setAliquotaCbs(BigDecimal aliquotaCbs) { this.aliquotaCbs = aliquotaCbs; }
    public void setHabilitarIbsCbs(boolean habilitarIbsCbs) { this.habilitarIbsCbs = habilitarIbsCbs; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
