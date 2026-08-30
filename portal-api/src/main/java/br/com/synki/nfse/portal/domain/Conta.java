package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conta")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "owner_usuario_id")
    private Long ownerUsuarioId;

    @Column(nullable = false, length = 32)
    private String status = "ativa";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

    public static Conta criar(String nome, Long ownerUsuarioId) {
        var c = new Conta();
        c.nome = nome;
        c.ownerUsuarioId = ownerUsuarioId;
        return c;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Long getOwnerUsuarioId() { return ownerUsuarioId; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public String getStripeCustomerId() { return stripeCustomerId; }

    public void setOwnerUsuarioId(Long ownerUsuarioId) { this.ownerUsuarioId = ownerUsuarioId; }
    public void setNome(String nome) { this.nome = nome; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
}
