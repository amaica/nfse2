package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "portal_submenu")
public class PortalSubMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private PortalMenu menu;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(length = 80)
    private String icon;

    @Column(length = 500)
    private String outcome;

    public Long getId() { return id; }
    public PortalMenu getMenu() { return menu; }
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getOutcome() { return outcome; }

    public void setMenu(PortalMenu menu) { this.menu = menu; }
    public void setLabel(String label) { this.label = label; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
