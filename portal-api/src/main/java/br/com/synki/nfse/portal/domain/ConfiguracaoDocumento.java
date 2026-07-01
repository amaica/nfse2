package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "configuracao_documento")
public class ConfiguracaoDocumento {

    public static final String TIPO_NFE = "NFE";
    public static final String TIPO_NFCE = "NFCE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, length = 10)
    private String serie = "1";

    @Column(name = "ultimo_numero", nullable = false)
    private long ultimoNumero = 0;

    @Column(nullable = false)
    private boolean habilitado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static ConfiguracaoDocumento criar(Long empresaId, String tipo, boolean habilitado) {
        var c = new ConfiguracaoDocumento();
        c.empresaId = empresaId;
        c.tipo = tipo;
        c.habilitado = habilitado;
        return c;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getTipo() { return tipo; }
    public String getSerie() { return serie; }
    public long getUltimoNumero() { return ultimoNumero; }
    public boolean isHabilitado() { return habilitado; }

    public void setSerie(String serie) { this.serie = serie; }
    public void setUltimoNumero(long ultimoNumero) { this.ultimoNumero = ultimoNumero; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
}
