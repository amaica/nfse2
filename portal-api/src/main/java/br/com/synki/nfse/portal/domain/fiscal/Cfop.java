package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cfop")
public class Cfop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false, length = 4)
    private String cfop;

    @Column(length = 500)
    private String aplicacao;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getCfop() { return cfop; }
    public String getAplicacao() { return aplicacao; }
    public String getDescricao() { return descricao; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setCfop(String cfop) { this.cfop = cfop; }
    public void setAplicacao(String aplicacao) { this.aplicacao = aplicacao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
