package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "configuracao_nfse")
public class ConfiguracaoNfse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, unique = true)
    private Long empresaId;

    private String prefeitura;

    @Column(name = "codigo_municipio_ibge", nullable = false, length = 7)
    private String codigoMunicipioIbge;

    @Column(nullable = false, length = 20)
    private String ambiente = "producao";

    @Column(name = "token_integracao", length = 512)
    private String tokenIntegracao;

    @Column(name = "serie_rps", nullable = false, length = 10)
    private String serieRps = "1";

    @Column(name = "ultimo_numero_nfse", nullable = false)
    private long ultimoNumeroNfse = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getPrefeitura() { return prefeitura; }
    public String getCodigoMunicipioIbge() { return codigoMunicipioIbge; }
    public String getAmbiente() { return ambiente; }
    public String getSerieRps() { return serieRps; }
    public long getUltimoNumeroNfse() { return ultimoNumeroNfse; }
    public boolean isProducao() { return "producao".equalsIgnoreCase(ambiente); }

    public void setPrefeitura(String prefeitura) { this.prefeitura = prefeitura; }
    public void setCodigoMunicipioIbge(String codigoMunicipioIbge) { this.codigoMunicipioIbge = codigoMunicipioIbge; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public void setSerieRps(String serieRps) { this.serieRps = serieRps; }
    public void setUltimoNumeroNfse(long ultimoNumeroNfse) { this.ultimoNumeroNfse = ultimoNumeroNfse; }

    public static ConfiguracaoNfse criar(Long empresaId, String prefeitura, String codigoMunicipioIbge, String ambiente) {
        var c = new ConfiguracaoNfse();
        c.empresaId = empresaId;
        c.prefeitura = prefeitura;
        c.codigoMunicipioIbge = codigoMunicipioIbge;
        c.ambiente = ambiente != null && !ambiente.isBlank() ? ambiente : "homologacao";
        return c;
    }
}
