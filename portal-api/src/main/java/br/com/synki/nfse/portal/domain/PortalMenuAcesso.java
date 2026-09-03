package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "portal_menu_acesso")
public class PortalMenuAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static PortalMenuAcesso of(Long usuarioId, Long empresaId, Long menuId) {
        var row = new PortalMenuAcesso();
        row.usuarioId = usuarioId;
        row.empresaId = empresaId;
        row.menuId = menuId;
        return row;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getMenuId() { return menuId; }
}
