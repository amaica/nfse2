package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario_empresa")
public class UsuarioEmpresa {

    public static final String PAPEL_OWNER = "OWNER";
    public static final String PAPEL_ADMIN = "ADMIN";
    public static final String PAPEL_OPERADOR = "OPERADOR";
    public static final String PAPEL_VISUALIZADOR = "VISUALIZADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(nullable = false, length = 32)
    private String papel = PAPEL_OPERADOR;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static UsuarioEmpresa vincular(Long usuarioId, Long empresaId, Long contaId, String papel) {
        var ue = new UsuarioEmpresa();
        ue.usuarioId = usuarioId;
        ue.empresaId = empresaId;
        ue.contaId = contaId;
        ue.papel = papel != null ? papel : PAPEL_OPERADOR;
        return ue;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getContaId() { return contaId; }
    public String getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }

    public void setPapel(String papel) { this.papel = papel; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
