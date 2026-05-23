package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static Usuario create(Long empresaId, String nome, String email, String senhaHash) {
        var u = new Usuario();
        u.empresaId = empresaId;
        u.nome = nome;
        u.email = email;
        u.senha = senhaHash;
        return u;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public boolean isAtivo() { return ativo; }
}
