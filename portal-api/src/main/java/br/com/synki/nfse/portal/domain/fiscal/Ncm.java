package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ncm")
public class Ncm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String codigo;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public String getObservacao() { return observacao; }

    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
