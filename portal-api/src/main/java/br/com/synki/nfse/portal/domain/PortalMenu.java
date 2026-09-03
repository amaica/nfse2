package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portal_menu")
public class PortalMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(length = 80)
    private String icon;

    @Column(length = 500)
    private String outcome;

    @Column(name = "ordem_menu", nullable = false)
    private Integer ordemMenu = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "operador_tem_acesso", nullable = false, length = 3)
    private String operadorTemAcesso = "SIM";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PortalMenu parent;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortalSubMenu> submenus = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getOutcome() { return outcome; }
    public Integer getOrdemMenu() { return ordemMenu; }
    public boolean isAtivo() { return ativo; }
    public String getOperadorTemAcesso() { return operadorTemAcesso; }
    public PortalMenu getParent() { return parent; }
    public List<PortalSubMenu> getSubmenus() { return submenus; }
    public Instant getCreatedAt() { return createdAt; }

    public void setLabel(String label) { this.label = label; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public void setOrdemMenu(Integer ordemMenu) { this.ordemMenu = ordemMenu; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setOperadorTemAcesso(String operadorTemAcesso) { this.operadorTemAcesso = operadorTemAcesso; }
    public void setParent(PortalMenu parent) { this.parent = parent; }

    public void replaceSubmenus(List<PortalSubMenu> next) {
        submenus.clear();
        if (next == null) return;
        for (PortalSubMenu item : next) {
            item.setMenu(this);
            submenus.add(item);
        }
    }
}
