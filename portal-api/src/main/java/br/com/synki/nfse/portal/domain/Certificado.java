package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "certificado")
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Lob
    @Column(nullable = false)
    private byte[] arquivo;

    @Column(nullable = false)
    private String senha;

    private LocalDate validade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static Certificado of(Long empresaId, byte[] arquivo, String senha) {
        var c = new Certificado();
        c.empresaId = empresaId;
        c.arquivo = arquivo;
        c.senha = senha;
        return c;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public byte[] getArquivo() { return arquivo; }
    public String getSenha() { return senha; }
}
