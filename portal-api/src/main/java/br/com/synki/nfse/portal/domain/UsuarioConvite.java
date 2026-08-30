package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario_convite")
public class UsuarioConvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 32)
    private String papel = UsuarioEmpresa.PAPEL_OPERADOR;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "criado_por_usuario_id", nullable = false)
    private Long criadoPorUsuarioId;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "aceito_em")
    private Instant aceitoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static UsuarioConvite criar(
            Long contaId,
            Long empresaId,
            String email,
            String papel,
            String token,
            Long criadoPorUsuarioId,
            Instant expiraEm) {
        var c = new UsuarioConvite();
        c.contaId = contaId;
        c.empresaId = empresaId;
        c.email = email.trim().toLowerCase();
        c.papel = papel != null ? papel : UsuarioEmpresa.PAPEL_OPERADOR;
        c.token = token;
        c.criadoPorUsuarioId = criadoPorUsuarioId;
        c.expiraEm = expiraEm;
        return c;
    }

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public Long getEmpresaId() { return empresaId; }
    public String getEmail() { return email; }
    public String getPapel() { return papel; }
    public String getToken() { return token; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getAceitoEm() { return aceitoEm; }

    public boolean isPendente() {
        return aceitoEm == null && Instant.now().isBefore(expiraEm);
    }

    public void marcarAceito() {
        this.aceitoEm = Instant.now();
    }
}
