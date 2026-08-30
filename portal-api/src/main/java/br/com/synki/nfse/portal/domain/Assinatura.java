package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assinatura")
public class Assinatura {

    public static final String STATUS_TRIAL = "trial";
    public static final String STATUS_PENDENTE = "pendente";
    public static final String STATUS_ATIVA = "ativa";
    public static final String STATUS_VENCIDA = "vencida";
    public static final String STATUS_CANCELADA = "cancelada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_id", nullable = false, unique = true)
    private Long contaId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(nullable = false, length = 32)
    private String status = STATUS_TRIAL;

    @Column(name = "plano_codigo", nullable = false, length = 32)
    private String planoCodigo = "starter";

    @Column(nullable = false)
    private int pacotes = 1;

    @Column(name = "empresas_quota", nullable = false)
    private int empresasQuota = 1;

    @Column(name = "usuarios_quota", nullable = false)
    private int usuariosQuota = 5;

    @Column(name = "nfse_mes_quota", nullable = false)
    private int nfseMesQuota = 100;

    @Column(name = "nfe_mes_quota", nullable = false)
    private int nfeMesQuota = 50;

    @Column(name = "periodo_fim")
    private Instant periodoFim;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public static Assinatura trial(Long contaId) {
        var a = new Assinatura();
        a.contaId = contaId;
        a.status = STATUS_TRIAL;
        a.pacotes = 1;
        a.empresasQuota = 1;
        a.usuariosQuota = 3;
        a.nfseMesQuota = 50;
        a.nfeMesQuota = 25;
        a.periodoFim = Instant.now().plusSeconds(14 * 86400L);
        return a;
    }

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getStatus() { return status; }
    public String getPlanoCodigo() { return planoCodigo; }
    public int getPacotes() { return pacotes; }
    public int getEmpresasQuota() { return empresasQuota; }
    public int getUsuariosQuota() { return usuariosQuota; }
    public int getNfseMesQuota() { return nfseMesQuota; }
    public int getNfeMesQuota() { return nfeMesQuota; }
    public Instant getPeriodoFim() { return periodoFim; }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public void setStatus(String status) { this.status = status; }
    public void setPacotes(int pacotes) { this.pacotes = pacotes; }
    public void setEmpresasQuota(int empresasQuota) { this.empresasQuota = empresasQuota; }
    public void setUsuariosQuota(int usuariosQuota) { this.usuariosQuota = usuariosQuota; }
    public void setNfseMesQuota(int nfseMesQuota) { this.nfseMesQuota = nfseMesQuota; }
    public void setNfeMesQuota(int nfeMesQuota) { this.nfeMesQuota = nfeMesQuota; }
    public void setPeriodoFim(Instant periodoFim) { this.periodoFim = periodoFim; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void aplicarPacotes(int pacotes) {
        this.pacotes = Math.max(1, pacotes);
        this.empresasQuota = this.pacotes;
        this.usuariosQuota = this.pacotes * 5;
        this.nfseMesQuota = this.pacotes * 100;
        this.nfeMesQuota = this.pacotes * 50;
    }
}
