package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.web.dto.nfe.NfeIbsCbsItemRequest;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoImpostoTributacaoIBSCBS;
import com.fincatto.documentofiscal.nfe400.classes.nota.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Montagem IBS/CBS — Reforma Tributária (NT 2023.001+).
 * A partir de 03/08/2026 a SEFAZ exige preenchimento correto dos campos IBS e CBS.
 * Alíquota teste padrão: 1% (IBS 0,9% UF + 0,1% Mun; CBS 1%).
 */
public final class NfeReformaMontador {

    public static final LocalDate OBRIGATORIEDADE_IBS_CBS = LocalDate.of(2026, 8, 3);

    private static final BigDecimal ALIQ_IBS_UF_TESTE = new BigDecimal("0.0090");
    private static final BigDecimal ALIQ_IBS_MUN_TESTE = new BigDecimal("0.0010");
    private static final BigDecimal ALIQ_CBS_TESTE = new BigDecimal("0.0100");
    private static final String CST_PADRAO = "000";
    private static final String CLASS_TRIB_PADRAO = "000001";

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

        var pIbsUf = aliquota(req != null ? req.aliquotaIbsUf() : null,
                operacao != null ? operacao.getAliquotaIbsUf() : null, ALIQ_IBS_UF_TESTE);
        var pIbsMun = aliquota(req != null ? req.aliquotaIbsMun() : null,
                operacao != null ? operacao.getAliquotaIbsMun() : null, ALIQ_IBS_MUN_TESTE);
        var pCbs = aliquota(req != null ? req.aliquotaCbs() : null,
                operacao != null ? operacao.getAliquotaCbs() : null, ALIQ_CBS_TESTE);

        var bc = moeda(baseCalculo);
        var vIbsUf = moeda(bc.multiply(pIbsUf));
        var vIbsMun = moeda(bc.multiply(pIbsMun));
        var vCbs = moeda(bc.multiply(pCbs));
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
        ibsCbsTot.setGCBS(gCbs);

        total.setIbscbsTot(ibsCbsTot);

        var nfeTotal = total.getIcmsTotal().getValorTotalNFe();
        if (nfeTotal != null) {
            var novoTotal = parse(nfeTotal).add(totais.vIbs()).add(totais.vCbs());
            total.getIcmsTotal().setValorTotalNFe(moeda(novoTotal));
        }
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

    private static String primeiroNaoVazio(String... vals) {
        for (var v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return CST_PADRAO;
    }
}
