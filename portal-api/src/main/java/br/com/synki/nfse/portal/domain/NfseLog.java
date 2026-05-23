package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nfse_log")
public class NfseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 80)
    private String acao;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static NfseLog of(Long empresaId, Long usuarioId, String acao, String descricao) {
        var log = new NfseLog();
        log.empresaId = empresaId;
        log.usuarioId = usuarioId;
        log.acao = acao;
        log.descricao = descricao;
        return log;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAcao() { return acao; }
    public String getDescricao() { return descricao; }
    public Instant getCreatedAt() { return createdAt; }
}
