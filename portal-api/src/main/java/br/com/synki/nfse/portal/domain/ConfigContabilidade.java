package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "config_contabilidade")
public class ConfigContabilidade {

    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "email_contabilidade")
    private String emailContabilidade;

    @Column(name = "envio_automatico", nullable = false)
    private boolean envioAutomatico = false;

    @Column(name = "enviar_nfse", nullable = false)
    private boolean enviarNfse = true;

    @Column(name = "enviar_nfe", nullable = false)
    private boolean enviarNfe = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public static ConfigContabilidade padrao(Long empresaId) {
        var c = new ConfigContabilidade();
        c.empresaId = empresaId;
        return c;
    }

    public Long getEmpresaId() { return empresaId; }
    public String getEmailContabilidade() { return emailContabilidade; }
    public boolean isEnvioAutomatico() { return envioAutomatico; }
    public boolean isEnviarNfse() { return enviarNfse; }
    public boolean isEnviarNfe() { return enviarNfe; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setEmailContabilidade(String emailContabilidade) { this.emailContabilidade = emailContabilidade; }
    public void setEnvioAutomatico(boolean envioAutomatico) { this.envioAutomatico = envioAutomatico; }
    public void setEnviarNfse(boolean enviarNfse) { this.enviarNfse = enviarNfse; }
    public void setEnviarNfe(boolean enviarNfe) { this.enviarNfe = enviarNfe; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
