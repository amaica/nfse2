package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean revogado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static RefreshToken of(Long usuarioId, String tokenHash, Instant expiraEm) {
        var t = new RefreshToken();
        t.usuarioId = usuarioId;
        t.tokenHash = tokenHash;
        t.expiraEm = expiraEm;
        return t;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isRevogado() { return revogado; }

    public boolean isValido() {
        return !revogado && Instant.now().isBefore(expiraEm);
    }

    public void revogar() {
        this.revogado = true;
    }
}
