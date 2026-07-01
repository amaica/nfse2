package br.com.synki.nfse.portal.domain.fiscal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tribut_icms_uf")
public class TributIcmsUf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configura_of_gt_id", nullable = false)
    private TributConfiguraOfGt configuraOfGt;

    @Column(name = "uf_destino", nullable = false, length = 2)
    private String ufDestino;

    private Integer cfop;

    @Column(length = 3)
    private String cst;

    @Column(length = 3)
    private String csosn;

    @Column(precision = 7, scale = 4)
    private BigDecimal aliquota;

    @Column(name = "origem_mercadoria", length = 1)
    private String origemMercadoria;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    @JsonIgnore
    public TributConfiguraOfGt getConfiguraOfGt() { return configuraOfGt; }
    public String getUfDestino() { return ufDestino; }
    public Integer getCfop() { return cfop; }
    public String getCst() { return cst; }
    public String getCsosn() { return csosn; }
    public BigDecimal getAliquota() { return aliquota; }
    public String getOrigemMercadoria() { return origemMercadoria; }

    public void setConfiguraOfGt(TributConfiguraOfGt configuraOfGt) { this.configuraOfGt = configuraOfGt; }
    public void setUfDestino(String ufDestino) { this.ufDestino = ufDestino; }
    public void setCfop(Integer cfop) { this.cfop = cfop; }
    public void setCst(String cst) { this.cst = cst; }
    public void setCsosn(String csosn) { this.csosn = csosn; }
    public void setAliquota(BigDecimal aliquota) { this.aliquota = aliquota; }
    public void setOrigemMercadoria(String origemMercadoria) { this.origemMercadoria = origemMercadoria; }
}
