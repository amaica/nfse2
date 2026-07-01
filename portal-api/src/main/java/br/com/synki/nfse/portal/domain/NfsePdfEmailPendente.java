package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nfse_pdf_email_pendente")
public class NfsePdfEmailPendente {

    public enum Status {
        PENDENTE,
        ENVIADO,
        EXPIRADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "chave_acesso", nullable = false, length = 50)
    private String chaveAcesso;

    @Column(nullable = false)
    private String destinatario;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public static NfsePdfEmailPendente criar(Long empresaId, String chaveAcesso, String destinatario, String mensagem) {
        var item = new NfsePdfEmailPendente();
        item.empresaId = empresaId;
        item.chaveAcesso = chaveAcesso;
        item.destinatario = destinatario;
        item.mensagem = mensagem;
        return item;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getChaveAcesso() { return chaveAcesso; }
    public String getDestinatario() { return destinatario; }
    public String getMensagem() { return mensagem; }
    public int getTentativas() { return tentativas; }
    public Instant getProximaTentativaEm() { return proximaTentativaEm; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void registrarTentativa(Instant proximaTentativa) {
        this.tentativas++;
        this.proximaTentativaEm = proximaTentativa;
        this.updatedAt = Instant.now();
    }

    public void marcarEnviado() {
        this.status = Status.ENVIADO;
        this.updatedAt = Instant.now();
    }

    public void marcarExpirado() {
        this.status = Status.EXPIRADO;
        this.updatedAt = Instant.now();
    }
}
