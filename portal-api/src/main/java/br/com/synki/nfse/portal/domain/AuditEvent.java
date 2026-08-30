package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 80)
    private String acao;

    @Column(length = 120)
    private String recurso;

    @Column(columnDefinition = "TEXT")
    private String detalhe;

    @Column(length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static AuditEvent of(
            Long contaId,
            Long empresaId,
            Long usuarioId,
            String acao,
            String recurso,
            String detalhe,
            String ip) {
        var e = new AuditEvent();
        e.contaId = contaId;
        e.empresaId = empresaId;
        e.usuarioId = usuarioId;
        e.acao = acao;
        e.recurso = recurso;
        e.detalhe = detalhe;
        e.ip = ip;
        return e;
    }

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAcao() { return acao; }
    public String getRecurso() { return recurso; }
    public String getDetalhe() { return detalhe; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }
}
