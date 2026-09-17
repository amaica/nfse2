package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.web.dto.nfe.NfeIbsCbsItemRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeIsItemRequest;
import com.fincatto.documentofiscal.nfe400.classes.NFCredito;
import com.fincatto.documentofiscal.nfe400.classes.NFDebito;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoImpostoTributacaoIBSCBS;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoImpostoTributacaoIS;
import com.fincatto.documentofiscal.nfe400.classes.nota.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Montagem IBS/CBS — Reforma Tributária (NT 2023.001+).
 * A partir de 03/08/2026 a SEFAZ exige preenchimento correto dos campos IBS e CBS.
 * Alíquotas no XML são percentuais (como ICMS): 0,90 = 0,90% → v = BC × p / 100.
 * Padrão de teste: IBS 0,9% UF + 0,1% Mun; CBS 1%.
 */
public final class NfeReformaMontador {

    public static final LocalDate OBRIGATORIEDADE_IBS_CBS = LocalDate.of(2026, 8, 3);

    /** Percentual vigente 2026 (LC 214/25): IBS UF 0,10%; IBS Mun 0%; CBS 0,90%. */
    private static final BigDecimal ALIQ_IBS_UF_TESTE = new BigDecimal("0.10");
    private static final BigDecimal ALIQ_IBS_MUN_TESTE = new BigDecimal("0.00");
    private static final BigDecimal ALIQ_CBS_TESTE = new BigDecimal("0.90");
    private static final String CST_PADRAO = "000";
    private static final String CLASS_TRIB_PADRAO = "000001";
    private static final BigDecimal CEM = new BigDecimal("100");

    private NfeReformaMontador() {}

    public static boolean deveIncluirIbsCbs(TributOperacaoFiscal operacao) {
        return operacao == null || operacao.isHabilitarIbsCbs();
    }

    public static void aplicarIdentificacaoReforma(
            NFNotaInfoIdentificacao id,
            TributOperacaoFiscal operacao,
            String codigoMunicipioEmitente) {
        if (operacao == null) {
            return;
        }
        if (operacao.getCMunFGIBS() != null && !operacao.getCMunFGIBS().isBlank()) {
            id.setCMunFGIBS(operacao.getCMunFGIBS());
        } else if (codigoMunicipioEmitente != null) {
            id.setCMunFGIBS(codigoMunicipioEmitente);
        }
        if ("1".equals(operacao.getIndIntermed())) {
            id.setIndIntermed(NFIndicadorIntermediador.OPERACAO_COM_INTERMEDIADOR);
        }
        if (operacao.getTpNFDebito() != null && !operacao.getTpNFDebito().isBlank()) {
            var deb = NFDebito.valueOfCodigo(operacao.getTpNFDebito().trim());
            if (deb != null) {
                id.setTpNFDebito(deb);
            }
        }
        if (operacao.getTpNFCredito() != null && !operacao.getTpNFCredito().isBlank()) {
            var cred = NFCredito.valueOfCodigo(operacao.getTpNFCredito().trim());
            if (cred != null) {
                id.setTpNFCredito(cred);
            }
        }
    }

    public static NFNotaInfoItemImpostoIS montarIsItem(
            BigDecimal baseCalculo,
            NfeIsItemRequest req,
            TributOperacaoFiscal operacao) {
        boolean habilitar = req != null && Boolean.TRUE.equals(req.habilitar())
                || (operacao != null && operacao.isHabilitarIs());
        if (!habilitar) {
            return null;
        }
        var cstCod = primeiroNaoVazio(
                req != null ? req.cst() : null,
                operacao != null ? operacao.getIsCst() : null,
                "01");
        var classTrib = primeiroNaoVazio(
                req != null ? req.classificacaoTributaria() : null,
                operacao != null ? operacao.getIsClassTrib() : null,
                "000001");
        var pIs = aliquota(req != null ? req.aliquota() : null,
                operacao != null ? operacao.getAliquotaIs() : null,
                BigDecimal.ZERO);
        pIs = normalizarAliquotaPercentual(pIs);
        var bc = moeda(baseCalculo);
        var vIs = moeda(bc.multiply(pIs).divide(CEM, 8, RoundingMode.HALF_UP));

        var is = new NFNotaInfoItemImpostoIS();
        var cst = NFNotaInfoImpostoTributacaoIS.valueOfCodigo(cstCod);
        if (cst == null) {
            cst = NFNotaInfoImpostoTributacaoIS.TRIBUTACAO_INTEGRAL;
        }
        is.setCstIS(cst);
        is.setCClassTribIS(classTrib);
        is.setVBCIS(bc);
        is.setPIS(pIs);
        is.setVIS(vIs);
        return is;
    }

    public static BigDecimal acumularIs(BigDecimal acc, NFNotaInfoItemImpostoIS is) {
        if (is == null || is.getVIS() == null || is.getVIS().isBlank()) {
            return acc != null ? acc : BigDecimal.ZERO;
        }
        var v = new BigDecimal(is.getVIS());
        return (acc != null ? acc : BigDecimal.ZERO).add(v);
    }

    public static void aplicarTotaisIs(NFNotaInfoTotal total, BigDecimal vIs) {
        if (vIs == null || vIs.signum() <= 0) {
            return;
        }
        var isTot = new NFNotaInfoISTot();
        isTot.setVIS(vIs.setScale(2, RoundingMode.HALF_UP));
        total.setIsTot(isTot);
    }

    public static NFNotaInfoItemImpostoIBSCBS montarIbsCbsItem(
            BigDecimal baseCalculo,
            NfeIbsCbsItemRequest req,
            TributOperacaoFiscal operacao) {
        if (req != null && Boolean.FALSE.equals(req.habilitar())) {
            return null;
        }
        if (!deveIncluirIbsCbs(operacao) && (req == null || !Boolean.TRUE.equals(req.habilitar()))) {
            return null;
        }

        var cstCod = primeiroNaoVazio(
                req != null ? req.cst() : null,
                operacao != null ? operacao.getIbsCbsCst() : null,
                CST_PADRAO);
        var classTrib = primeiroNaoVazio(
                req != null ? req.classificacaoTributaria() : null,
                operacao != null ? operacao.getIbsCbsClassTrib() : null,
                CLASS_TRIB_PADRAO);

        var pIbsUf = aliquotaIbsUf(req != null ? req.aliquotaIbsUf() : null, operacao);
        var pIbsMun = aliquotaIbsMun(req != null ? req.aliquotaIbsMun() : null, operacao);
        var pCbs = aliquotaCbs(req != null ? req.aliquotaCbs() : null, operacao);

        var bc = moeda(baseCalculo);
        var vIbsUf = moeda(bc.multiply(pIbsUf).divide(CEM, 8, RoundingMode.HALF_UP));
        var vIbsMun = moeda(bc.multiply(pIbsMun).divide(CEM, 8, RoundingMode.HALF_UP));
        var vCbs = moeda(bc.multiply(pCbs).divide(CEM, 8, RoundingMode.HALF_UP));
        var vIbs = moeda(vIbsUf.add(vIbsMun));

        var ibsCbs = new NFNotaInfoItemImpostoIBSCBS();
        ibsCbs.setCst(NFNotaInfoImpostoTributacaoIBSCBS.valueOfCodigo(cstCod));
        ibsCbs.setcClassTrib(classTrib);

        var gIbsCbs = new NFNotaInfoItemImpostoIBSCBSTIBS();
        gIbsCbs.setVBC(bc);

        var gIbsUf = new NFNotaInfoItemImpostoIBSCBSTIBS.GIBSUF();
        gIbsUf.setPIBSUF(pIbsUf);
        gIbsUf.setVIBSUF(vIbsUf);
        gIbsCbs.setGIBSUF(gIbsUf);

        var gIbsMun = new NFNotaInfoItemImpostoIBSCBSTIBS.GIBSMun();
        gIbsMun.setPIBSMun(pIbsMun);
        gIbsMun.setVIBSMun(vIbsMun);
        gIbsCbs.setGIBSMun(gIbsMun);

        var gCbs = new NFNotaInfoItemImpostoIBSCBSTIBS.GCBS();
        gCbs.setPCBS(pCbs);
        gCbs.setVCBS(vCbs);
        gIbsCbs.setGCBS(gCbs);

        gIbsCbs.setVIBS(vIbs);
        ibsCbs.setGIBSCBS(gIbsCbs);
        return ibsCbs;
    }

    public static record TotaisIbsCbs(BigDecimal vBc, BigDecimal vIbsUf, BigDecimal vIbsMun, BigDecimal vIbs, BigDecimal vCbs) {}

    public static TotaisIbsCbs acumular(TotaisIbsCbs acc, NFNotaInfoItemImpostoIBSCBS ibsCbs) {
        if (ibsCbs == null || ibsCbs.getGIBSCBS() == null) {
            return acc;
        }
        var g = ibsCbs.getGIBSCBS();
        var bc = parse(g.getVBC());
        var vUf = g.getGIBSUF() != null ? parse(g.getGIBSUF().getVIBSUF()) : BigDecimal.ZERO;
        var vMun = g.getGIBSMun() != null ? parse(g.getGIBSMun().getVIBSMun()) : BigDecimal.ZERO;
        var vIbs = parse(g.getVIBS());
        var vCbs = g.getGCBS() != null ? parse(g.getGCBS().getVCBS()) : BigDecimal.ZERO;
        if (acc == null) {
            return new TotaisIbsCbs(bc, vUf, vMun, vIbs, vCbs);
        }
        return new TotaisIbsCbs(
                acc.vBc().add(bc),
                acc.vIbsUf().add(vUf),
                acc.vIbsMun().add(vMun),
                acc.vIbs().add(vIbs),
                acc.vCbs().add(vCbs));
    }

    public static void aplicarTotais(NFNotaInfoTotal total, TotaisIbsCbs totais) {
        if (totais == null || totais.vBc().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        var ibsCbsTot = new NFNotaInfoIBSCBSTot();
        ibsCbsTot.setVBCIBSCBS(totais.vBc());

        var gIbs = new NFNotaInfoIBSCBSTot.GIBS();
        gIbs.setVCredPres(BigDecimal.ZERO);
        gIbs.setVCredPresCondSus(BigDecimal.ZERO);
        var gIbsUf = new NFNotaInfoIBSCBSTot.GIBS.GIBSUF();
        gIbsUf.setVDif(BigDecimal.ZERO);
        gIbsUf.setVDevTrib(BigDecimal.ZERO);
        gIbsUf.setVIBSUF(totais.vIbsUf());
        gIbs.setGIBSUF(gIbsUf);

        var gIbsMun = new NFNotaInfoIBSCBSTot.GIBS.GIBSMun();
        gIbsMun.setVDif(BigDecimal.ZERO);
        gIbsMun.setVDevTrib(BigDecimal.ZERO);
        gIbsMun.setVIBSMun(totais.vIbsMun());
        gIbs.setGIBSMun(gIbsMun);
        gIbs.setVIBS(totais.vIbs());
        ibsCbsTot.setGIBS(gIbs);

        var gCbs = new NFNotaInfoIBSCBSTot.GCBS();
        gCbs.setVDif(BigDecimal.ZERO);
        gCbs.setVDevTrib(BigDecimal.ZERO);
        gCbs.setVCBS(totais.vCbs());
        gCbs.setVCredPres(BigDecimal.ZERO);
        gCbs.setVCredPresCondSus(BigDecimal.ZERO);
        ibsCbsTot.setGCBS(gCbs);

        total.setIbscbsTot(ibsCbsTot);

        // vNF continua pelo somatório clássico (produtos + frete + IPI…);
        // IBS/CBS ficam só em IBSCBSTot — somá-los em vNF causa rejeição 610.
    }

    public static List<NFNotaInfoItem> extrairItensComIbs(List<NFNotaInfoItem> itens) {
        return itens != null ? itens : new ArrayList<>();
    }

    private static BigDecimal parse(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(valor);
    }

    private static BigDecimal moeda(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal aliquota(BigDecimal req, BigDecimal operacao, BigDecimal padrao) {
        if (req != null) return req;
        if (operacao != null) return operacao;
        return padrao;
    }

    /**
     * Cadastros legados gravaram fração (0,0090). XML exige percentual (0,90).
     * Em 2026 a legislação fixa pIBSMun=0 e pIBSUF=0,10 / pCBS=0,90 — se o cadastro
     * ainda tiver o pacote de teste antigo (0,90/0,10/1,00 ou frações), sobrescreve.
     */
    private static BigDecimal normalizarAliquotaPercentual(BigDecimal p) {
        if (p == null) {
            return BigDecimal.ZERO;
        }
        if (p.compareTo(new BigDecimal("0.1")) < 0 && p.signum() > 0) {
            return p.multiply(CEM).setScale(4, RoundingMode.HALF_UP);
        }
        return p;
    }

    private static BigDecimal aliquotaIbsUf(BigDecimal req, TributOperacaoFiscal operacao) {
        var raw = aliquota(req, operacao != null ? operacao.getAliquotaIbsUf() : null, ALIQ_IBS_UF_TESTE);
        var p = normalizarAliquotaPercentual(raw);
        // 2026: única alíquota UF válida = 0,10%
        if (LocalDate.now().getYear() <= 2026) {
            return ALIQ_IBS_UF_TESTE;
        }
        return p;
    }

    private static BigDecimal aliquotaIbsMun(BigDecimal req, TributOperacaoFiscal operacao) {
        // 2026: IBS municipal obrigatoriamente 0%
        if (LocalDate.now().getYear() <= 2026) {
            return ALIQ_IBS_MUN_TESTE;
        }
        var raw = aliquota(req, operacao != null ? operacao.getAliquotaIbsMun() : null, ALIQ_IBS_MUN_TESTE);
        return normalizarAliquotaPercentual(raw);
    }

    private static BigDecimal aliquotaCbs(BigDecimal req, TributOperacaoFiscal operacao) {
        var raw = aliquota(req, operacao != null ? operacao.getAliquotaCbs() : null, ALIQ_CBS_TESTE);
        var p = normalizarAliquotaPercentual(raw);
        if (LocalDate.now().getYear() <= 2026) {
            return ALIQ_CBS_TESTE;
        }
        return p;
    }

    private static String primeiroNaoVazio(String... vals) {
        for (var v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return CST_PADRAO;
    }
}
