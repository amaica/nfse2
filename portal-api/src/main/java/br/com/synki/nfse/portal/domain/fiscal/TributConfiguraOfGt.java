package br.com.synki.nfse.portal.domain.fiscal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tribut_configura_of_gt")
public class TributConfiguraOfGt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tribut_operacao_fiscal_id", nullable = false)
    private Long tributOperacaoFiscalId;

    @Column(name = "tribut_grupo_tributario_id", nullable = false)
    private Long tributGrupoTributarioId;

    @OneToMany(mappedBy = "configuraOfGt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TributIcmsUf> listaIcmsUf = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getTributOperacaoFiscalId() { return tributOperacaoFiscalId; }
    public Long getTributGrupoTributarioId() { return tributGrupoTributarioId; }
    public List<TributIcmsUf> getListaIcmsUf() { return listaIcmsUf; }

    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public void setTributOperacaoFiscalId(Long tributOperacaoFiscalId) { this.tributOperacaoFiscalId = tributOperacaoFiscalId; }
    public void setTributGrupoTributarioId(Long tributGrupoTributarioId) { this.tributGrupoTributarioId = tributGrupoTributarioId; }
    public void setListaIcmsUf(List<TributIcmsUf> listaIcmsUf) { this.listaIcmsUf = listaIcmsUf; }
}
