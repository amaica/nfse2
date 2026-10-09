package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "nfe_evento")
public class NfeEvento {

    public static final String CANCELAMENTO = "CANCELAMENTO";
    public static final String CCE = "CCE";
    public static final String INUTILIZACAO = "INUTILIZACAO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 3)
    private String modelo;

    @Column(length = 44)
    private String chave;

    @Column(length = 10)
    private String serie;

    @Column(name = "numero_inicial")
    private Long numeroInicial;

    @Column(name = "numero_final")
    private Long numeroFinal;

    private Integer sequencial;

    @Column(length = 20)
    private String protocolo;

    @Column(length = 8)
    private String status;

    @Column(length = 500)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getTipo() { return tipo; }
    public String getModelo() { return modelo; }
    public String getChave() { return chave; }
    public String getSerie() { return serie; }
    public Long getNumeroInicial() { return numeroInicial; }
    public Long getNumeroFinal() { return numeroFinal; }
    public Integer getSequencial() { return sequencial; }
    public String getProtocolo() { return protocolo; }
    public String getStatus() { return status; }
    public String getMotivo() { return motivo; }
    public String getTexto() { return texto; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setChave(String chave) { this.chave = chave; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setNumeroInicial(Long numeroInicial) { this.numeroInicial = numeroInicial; }
    public void setNumeroFinal(Long numeroFinal) { this.numeroFinal = numeroFinal; }
    public void setSequencial(Integer sequencial) { this.sequencial = sequencial; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public void setStatus(String status) { this.status = status; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public void setTexto(String texto) { this.texto = texto; }
}
