package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "portal_perfil")
public class PortalPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "portal_perfil_menu", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "menu_id")
    private Set<Long> menuIds = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public boolean isAtivo() { return ativo; }
    public Set<Long> getMenuIds() { return menuIds; }

    public void setContaId(Long contaId) { this.contaId = contaId; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setMenuIds(Set<Long> menuIds) {
        this.menuIds.clear();
        if (menuIds != null) this.menuIds.addAll(menuIds);
    }
}
