package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dunning_aviso")
public class DunningAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(nullable = false, length = 32)
    private String tipo;

    @Column(nullable = false, length = 64)
    private String referencia;

    @Column(name = "enviado_em", nullable = false, updatable = false)
    private Instant enviadoEm = Instant.now();

    public static DunningAviso of(Long contaId, String tipo, String referencia) {
        var d = new DunningAviso();
        d.contaId = contaId;
        d.tipo = tipo;
        d.referencia = referencia;
        return d;
    }

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public String getTipo() { return tipo; }
    public String getReferencia() { return referencia; }
    public Instant getEnviadoEm() { return enviadoEm; }
}
