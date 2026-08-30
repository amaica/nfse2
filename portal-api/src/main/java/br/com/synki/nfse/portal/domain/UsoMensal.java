package br.com.synki.nfse.portal.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "uso_mensal")
public class UsoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_id", nullable = false)
    private Long contaId;

    @Column(name = "ano_mes", nullable = false, length = 7)
    private String anoMes;

    @Column(name = "nfse_count", nullable = false)
    private int nfseCount = 0;

    @Column(name = "nfe_count", nullable = false)
    private int nfeCount = 0;

    public static UsoMensal zerado(Long contaId, String anoMes) {
        var u = new UsoMensal();
        u.contaId = contaId;
        u.anoMes = anoMes;
        return u;
    }

    public Long getId() { return id; }
    public Long getContaId() { return contaId; }
    public String getAnoMes() { return anoMes; }
    public int getNfseCount() { return nfseCount; }
    public int getNfeCount() { return nfeCount; }

    public void incrementNfse() { this.nfseCount++; }
    public void incrementNfe() { this.nfeCount++; }
}
