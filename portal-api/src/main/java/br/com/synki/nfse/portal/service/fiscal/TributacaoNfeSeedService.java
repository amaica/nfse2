package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributConfiguraOfGt;
import br.com.synki.nfse.portal.domain.fiscal.TributGrupoTributario;
import br.com.synki.nfse.portal.domain.fiscal.TributIcmsUf;
import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributConfiguraOfGtRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributGrupoTributarioRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributOperacaoFiscalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Pacote tributário NF-e mínimo para todo emitente: grupo nacional, operações
 * (venda, compra, devoluções, depósito) com IBS/CBS da reforma e ICMS por UF.
 */
@Service
public class TributacaoNfeSeedService {

    private static final Logger log = LoggerFactory.getLogger(TributacaoNfeSeedService.class);

    private static final String GRUPO_NACIONAL = "Mercadoria nacional";
    private static final BigDecimal IBS_UF = new BigDecimal("0.0090");
    private static final BigDecimal IBS_MUN = new BigDecimal("0.0010");
    private static final BigDecimal CBS = new BigDecimal("0.0100");
    private static final BigDecimal ALIQ_INTRA = new BigDecimal("0.1700");
    private static final BigDecimal ALIQ_INTER = new BigDecimal("0.1200");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoNfseRepository configuracaoNfseRepository;
    private final TributGrupoTributarioRepository grupoRepository;
    private final TributOperacaoFiscalRepository operacaoRepository;
    private final TributConfiguraOfGtRepository configuraRepository;

    public TributacaoNfeSeedService(
            EmpresaRepository empresaRepository,
            ConfiguracaoNfseRepository configuracaoNfseRepository,
            TributGrupoTributarioRepository grupoRepository,
            TributOperacaoFiscalRepository operacaoRepository,
            TributConfiguraOfGtRepository configuraRepository) {
        this.empresaRepository = empresaRepository;
        this.configuracaoNfseRepository = configuracaoNfseRepository;
        this.grupoRepository = grupoRepository;
        this.operacaoRepository = operacaoRepository;
        this.configuraRepository = configuraRepository;
    }

    public int garantirTodasEmpresas() {
        int total = 0;
        for (var empresa : empresaRepository.findAll()) {
            if (empresa.isAtivo()) {
                total += garantirCadastros(empresa.getId());
            }
        }
        return total;
    }

    @Transactional
    public int garantirCadastros(Long empresaId) {
        var empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            return 0;
        }
        int criados = 0;
        String uf = normalizarUf(empresa.getUf());
        String ufInter = "SP".equals(uf) ? "MG" : "SP";
        String munIbs = configuracaoNfseRepository.findByEmpresaId(empresaId)
                .map(c -> c.getCodigoMunicipioIbge())
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);
        boolean simples = empresa.isOptanteSimples();

        garantirGrupoNacional(empresaId);
        var grupoNacional = grupoRepository.findFirstByEmpresaIdAndDescricaoIgnoreCase(empresaId, GRUPO_NACIONAL)
                .orElseThrow();
        var grupos = List.of(grupoNacional);
        criados += aplicarReformaEmOperacoesExistentes(empresaId, munIbs);

        criados += garantirOperacaoComIcms(
                empresaId, grupos, munIbs, simples, uf, ufInter,
                "Venda", "S", "Venda de mercadoria", "1", 5102, 6102,
                "00", "102", ALIQ_INTRA, ALIQ_INTER);
        criados += garantirOperacaoComIcms(
                empresaId, grupos, munIbs, simples, uf, ufInter,
                "Compra", "E", "Compra de mercadoria", "1", 1102, 2102,
                "00", "102", ALIQ_INTRA, ALIQ_INTER);
        criados += garantirOperacaoComIcms(
                empresaId, grupos, munIbs, simples, uf, ufInter,
                "Devolução de venda", "E", "Devolução de venda", "4", 1202, 2202,
                "41", "400", ZERO, ZERO);
        criados += garantirOperacaoComIcms(
                empresaId, grupos, munIbs, simples, uf, ufInter,
                "Devolução de compra", "S", "Devolução de compra", "4", 5202, 6202,
                "41", "400", ZERO, ZERO);
        criados += garantirOperacaoComIcms(
                empresaId, grupos, munIbs, simples, uf, ufInter,
                "Remessa para depósito", "S", "Remessa para depósito fechado", "1", 5905, 6905,
                "41", "400", ZERO, ZERO);
        criados += garantirOperacaoComIcms(
                empresaId, grupos, munIbs, simples, uf, ufInter,
                "Retorno de depósito", "E", "Retorno de depósito fechado", "1", 1906, 2906,
                "41", "400", ZERO, ZERO);

        if (criados > 0) {
            log.info("Seed tributacao NF-e empresa {}: {} ajuste(s)", empresaId, criados);
        }
        return criados;
    }

    private TributGrupoTributario garantirGrupoNacional(Long empresaId) {
        return grupoRepository.findFirstByEmpresaIdAndDescricaoIgnoreCase(empresaId, GRUPO_NACIONAL)
                .orElseGet(() -> {
                    var g = new TributGrupoTributario();
                    g.setEmpresaId(empresaId);
                    g.setDescricao(GRUPO_NACIONAL);
                    g.setOrigemMercadoria("0");
                    g.setObservacao("Pacote padrão — origem nacional (ICMS orig 0)");
                    return grupoRepository.save(g);
                });
    }

    private int aplicarReformaEmOperacoesExistentes(Long empresaId, String munIbs) {
        int n = 0;
        for (var op : operacaoRepository.findByEmpresaIdOrderByDescricaoAsc(empresaId)) {
            if (preencherReforma(op, munIbs)) {
                operacaoRepository.save(op);
                n++;
            }
        }
        return n;
    }

    private int garantirOperacaoComIcms(
            Long empresaId,
            List<TributGrupoTributario> grupos,
            String munIbs,
            boolean simples,
            String ufIntra,
            String ufInter,
            String descricao,
            String tipo,
            String descricaoNaNf,
            String finalidadeOperacao,
            int cfopIntra,
            int cfopInter,
            String cst,
            String csosn,
            BigDecimal aliqIntra,
            BigDecimal aliqInter) {
        int n = 0;
        var existente = operacaoRepository.findFirstByEmpresaIdAndDescricaoIgnoreCase(empresaId, descricao);
        TributOperacaoFiscal op;
        if (existente.isPresent()) {
            op = existente.get();
            boolean mudou = preencherReforma(op, munIbs);
            if (op.getCfop() == null || op.getCfop() < 1000) {
                op.setCfop(cfopIntra);
                mudou = true;
            }
            if (isBlank(op.getTipoOperacao())) {
                op.setTipoOperacao(tipo);
                mudou = true;
            }
            if (isBlank(op.getFinalidadeOperacao())) {
                op.setFinalidadeOperacao(finalidadeOperacao);
                mudou = true;
            }
            if (mudou) {
                operacaoRepository.save(op);
                n++;
            }
        } else {
            op = new TributOperacaoFiscal();
            op.setEmpresaId(empresaId);
            op.setDescricao(descricao);
            op.setTipoOperacao(tipo);
            op.setDescricaoNaNf(descricaoNaNf);
            op.setCfop(cfopIntra);
            op.setFinalidadeOperacao(finalidadeOperacao);
            op.setGeraFinanceiro("S");
            op.setMovimentaEstoque("S");
            op.setPrincipal("Venda".equals(descricao) ? "S" : "N");
            preencherReforma(op, munIbs);
            op.setHabilitarIbsCbs(true);
            op = operacaoRepository.save(op);
            n++;
        }
        for (var grupo : grupos) {
            n += garantirIcms(empresaId, op, grupo, simples, ufIntra, ufInter, cfopIntra, cfopInter, cst, csosn, aliqIntra, aliqInter);
        }
        return n;
    }

    private int garantirIcms(
            Long empresaId,
            TributOperacaoFiscal op,
            TributGrupoTributario grupo,
            boolean simples,
            String ufIntra,
            String ufInter,
            int cfopIntra,
            int cfopInter,
            String cst,
            String csosn,
            BigDecimal aliqIntra,
            BigDecimal aliqInter) {
        String origem = isBlank(grupo.getOrigemMercadoria()) ? "0" : grupo.getOrigemMercadoria();
        var cfg = configuraRepository
                .findByEmpresaIdAndTributOperacaoFiscalIdAndTributGrupoTributarioId(
                        empresaId, op.getId(), grupo.getId())
                .orElseGet(() -> {
                    var c = new TributConfiguraOfGt();
                    c.setEmpresaId(empresaId);
                    c.setTributOperacaoFiscalId(op.getId());
                    c.setTributGrupoTributarioId(grupo.getId());
                    return c;
                });
        boolean novo = cfg.getId() == null;
        int added = 0;
        added += garantirLinhaIcms(cfg, ufIntra, cfopIntra, cst, csosn, aliqIntra, simples, origem);
        if (!ufInter.equals(ufIntra)) {
            added += garantirLinhaIcms(cfg, ufInter, cfopInter, cst, csosn, aliqInter, simples, origem);
        }
        if (novo || added > 0) {
            configuraRepository.save(cfg);
            return novo ? added + 1 : added;
        }
        return 0;
    }

    private int garantirLinhaIcms(
            TributConfiguraOfGt cfg,
            String uf,
            int cfop,
            String cst,
            String csosn,
            BigDecimal aliquota,
            boolean simples,
            String origem) {
        boolean existe = cfg.getListaIcmsUf().stream().anyMatch(i -> uf.equalsIgnoreCase(i.getUfDestino()));
        if (existe) {
            return 0;
        }
        var icms = new TributIcmsUf();
        icms.setConfiguraOfGt(cfg);
        icms.setUfDestino(uf);
        icms.setCfop(cfop);
        icms.setOrigemMercadoria(origem);
        icms.setAliquota(aliquota);
        if (simples) {
            icms.setCsosn(csosn);
        } else {
            icms.setCst(cst);
        }
        cfg.getListaIcmsUf().add(icms);
        return 1;
    }

    /** Preenche só o que estiver vazio — não sobrescreve cadastro já ajustado. */
    private static boolean preencherReforma(TributOperacaoFiscal op, String munIbs) {
        boolean mudou = false;
        if (isBlank(op.getIbsCbsCst())) {
            op.setIbsCbsCst("000");
            op.setHabilitarIbsCbs(true);
            mudou = true;
        }
        if (isBlank(op.getIbsCbsClassTrib())) {
            op.setIbsCbsClassTrib("000001");
            mudou = true;
        }
        if (op.getAliquotaIbsUf() == null) {
            op.setAliquotaIbsUf(IBS_UF);
            mudou = true;
        }
        if (op.getAliquotaIbsMun() == null) {
            op.setAliquotaIbsMun(IBS_MUN);
            mudou = true;
        }
        if (op.getAliquotaCbs() == null) {
            op.setAliquotaCbs(CBS);
            mudou = true;
        }
        if (isBlank(op.getIndIntermed())) {
            op.setIndIntermed("0");
            mudou = true;
        }
        if (isBlank(op.getCMunFGIBS()) && munIbs != null) {
            op.setCMunFGIBS(munIbs);
            mudou = true;
        }
        return mudou;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String normalizarUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return "RS";
        }
        return uf.trim().toUpperCase(Locale.ROOT);
    }
}
