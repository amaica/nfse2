package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 7)
    private String placa;

    @Column(length = 100)
    private String modelo;

    @Column(length = 100)
    private String marca;

    @Column(length = 20)
    private String renavam;

    @Column(name = "tipo_rodado", length = 2)
    private String tipoRodado;

    @Column(name = "tipo_carroceria", length = 2)
    private String tipoCarroceria;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getPlaca() { return placa; }
    public String getModelo() { return modelo; }
    public String getMarca() { return marca; }
    public String getRenavam() { return renavam; }
    public String getTipoRodado() { return tipoRodado; }
    public String getTipoCarroceria() { return tipoCarroceria; }
    public boolean isAtivo() { return ativo; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setPlaca(String placa) { this.placa = placa; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setRenavam(String renavam) { this.renavam = renavam; }
    public void setTipoRodado(String tipoRodado) { this.tipoRodado = tipoRodado; }
    public void setTipoCarroceria(String tipoCarroceria) { this.tipoCarroceria = tipoCarroceria; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
