package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tribut_grupo_tributario")
public class TributGrupoTributario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "origem_mercadoria", length = 1)
    private String origemMercadoria;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getDescricao() { return descricao; }
    public String getOrigemMercadoria() { return origemMercadoria; }
    public String getObservacao() { return observacao; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setOrigemMercadoria(String origemMercadoria) { this.origemMercadoria = origemMercadoria; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
