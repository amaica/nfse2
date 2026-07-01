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

    @Column(length = 14)
    private String gtin;

    @Column(name = "codigo_ncm", length = 8)
    private String codigoNcm;

    @Column(length = 6, nullable = false)
    private String unidade = "UN";

    @Column(name = "valor_unitario", precision = 15, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "grupo_tributario_id")
    private Long grupoTributarioId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getGtin() { return gtin; }
    public String getCodigoNcm() { return codigoNcm; }
    public String getUnidade() { return unidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public Long getGrupoTributarioId() { return grupoTributarioId; }
    public boolean isAtivo() { return ativo; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNome(String nome) { this.nome = nome; }
    public void setGtin(String gtin) { this.gtin = gtin; }
    public void setCodigoNcm(String codigoNcm) { this.codigoNcm = codigoNcm; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public void setGrupoTributarioId(Long grupoTributarioId) { this.grupoTributarioId = grupoTributarioId; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
