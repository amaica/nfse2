package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 14, unique = true)
    private String cnpj;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public boolean isAtivo() { return ativo; }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }
}
