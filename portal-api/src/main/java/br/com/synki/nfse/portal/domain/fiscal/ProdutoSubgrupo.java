package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "produto_subgrupo")
public class ProdutoSubgrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "produto_grupo_id", nullable = false)
    private Long produtoGrupoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getProdutoGrupoId() { return produtoGrupoId; }
    public String getNome() { return nome; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setProdutoGrupoId(Long produtoGrupoId) { this.produtoGrupoId = produtoGrupoId; }
    public void setNome(String nome) { this.nome = nome; }
}
