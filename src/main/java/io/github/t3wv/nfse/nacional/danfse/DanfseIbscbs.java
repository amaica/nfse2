package io.github.t3wv.nfse.nacional.danfse;

import org.w3c.dom.Element;

import java.util.Map;

/**
 * Bloco "Tributação IBS/CBS" do DANFSe (NT 008/2026 §2.1.10 e §2.1.11).
 * Presente apenas quando {@code infNFSe/IBSCBS} existir no XML autorizado.
 */
final class DanfseIbscbs {

    private final boolean presente;
    private final String cstCClassTrib;
    private final String indOperLocalidade;
    private final String exclusoesBc;
    private final String vBc;
    private final String pRedAliqs;
    private final String pAliqIbsUfMun;
    private final String pAliqEfetMun;
    private final String vIbsMun;
    private final String pAliqEfetUf;
    private final String vIbsUf;
    private final String vIbsTot;
    private final String pCbs;
    private final String pAliqEfetCbs;
    private final String vCbs;
    private final String vTotIbsCbs;
    private final String vLiqComIbsCbs;

    private DanfseIbscbs(
            final boolean presente,
            final String cstCClassTrib,
            final String indOperLocalidade,
            final String exclusoesBc,
            final String vBc,
            final String pRedAliqs,
            final String pAliqIbsUfMun,
            final String pAliqEfetMun,
            final String vIbsMun,
            final String pAliqEfetUf,
            final String vIbsUf,
            final String vIbsTot,
            final String pCbs,
            final String pAliqEfetCbs,
            final String vCbs,
            final String vTotIbsCbs,
            final String vLiqComIbsCbs) {
        this.presente = presente;
        this.cstCClassTrib = cstCClassTrib;
        this.indOperLocalidade = indOperLocalidade;
        this.exclusoesBc = exclusoesBc;
        this.vBc = vBc;
        this.pRedAliqs = pRedAliqs;
        this.pAliqIbsUfMun = pAliqIbsUfMun;
        this.pAliqEfetMun = pAliqEfetMun;
        this.vIbsMun = vIbsMun;
        this.pAliqEfetUf = pAliqEfetUf;
        this.vIbsUf = vIbsUf;
        this.vIbsTot = vIbsTot;
        this.pCbs = pCbs;
        this.pAliqEfetCbs = pAliqEfetCbs;
        this.vCbs = vCbs;
        this.vTotIbsCbs = vTotIbsCbs;
        this.vLiqComIbsCbs = vLiqComIbsCbs;
    }

    static DanfseIbscbs ausente() {
        return new DanfseIbscbs(
                false, "-", "-", "0,00", "0,00", "-", "-", "-", "0,00",
                "-", "0,00", "0,00", "-", "-", "0,00", "0,00", "0,00");
    }

    /**
     * Extrai IBS/CBS de {@code infNFSe} (valores apurados) e {@code infDPS} (CST / cIndOp).
     */
    static DanfseIbscbs from(final Element infNfse, final Element infDps, final String ufIncidencia) {
        final Element ibsNfse = DanfseDom.first(infNfse, "IBSCBS");
        if (ibsNfse == null) {
            return ausente();
        }

        final Element valores = DanfseDom.first(ibsNfse, "valores");
        final Element uf = DanfseDom.first(valores, "uf");
        final Element mun = DanfseDom.first(valores, "mun");
        final Element fed = DanfseDom.first(valores, "fed");
        final Element totCibs = DanfseDom.first(ibsNfse, "totCIBS");
        final Element gIbs = DanfseDom.first(totCibs, "gIBS");
        final Element gCbs = DanfseDom.first(totCibs, "gCBS");

        final Element ibsDps = DanfseDom.first(infDps, "IBSCBS");
        final Element gIbscbs = DanfseDom.first(
                DanfseDom.first(DanfseDom.first(ibsDps, "valores"), "trib"), "gIBSCBS");

        final String cst = DanfseDom.text(gIbscbs, "CST");
        final String cClassTrib = DanfseDom.text(gIbscbs, "cClassTrib");
        final String cIndOp = DanfseDom.text(ibsDps, "cIndOp");
        final String cLoc = DanfseDom.text(ibsNfse, "cLocalidadeIncid");
        final String xLoc = DanfseDom.text(ibsNfse, "xLocalidadeIncid");

        final String vBc = DanfseDom.text(valores, "vBC");
        final String vCalcRee = DanfseDom.text(valores, "vCalcReeRepRes");
        final String vIssqn = DanfseDom.text(DanfseDom.first(infNfse, "valores"), "vISSQN");
        final Element tribFed = DanfseDom.first(
                DanfseDom.first(DanfseDom.first(DanfseDom.first(infDps, "valores"), "trib"), "tribFed"),
                "piscofins");
        final String vPis = DanfseDom.firstText(tribFed, "vPis", "vPIS");
        final String vCofins = DanfseDom.firstText(tribFed, "vCofins", "vCOFINS");
        final String vDescIncond = DanfseDom.text(
                DanfseDom.first(DanfseDom.first(infDps, "valores"), "vDescCondIncond"), "vDescIncond");

        final String vIbsUf = DanfseDom.text(DanfseDom.first(gIbs, "gIBSUFTot"), "vIBSUF");
        final String vIbsMun = DanfseDom.text(DanfseDom.first(gIbs, "gIBSMunTot"), "vIBSMun");
        final String vIbsTot = DanfseDom.text(gIbs, "vIBSTot");
        final String vCbs = DanfseDom.text(gCbs, "vCBS");
        final String vTotNf = DanfseDom.text(totCibs, "vTotNF");
        final String vLiq = DanfseDom.text(DanfseDom.first(infNfse, "valores"), "vLiq");

        final String totIbsCbs = DanfseFormats.moneySum(vIbsTot, vCbs);
        final String liqComIbsCbs = DanfseDom.blank(vTotNf)
                ? DanfseFormats.moneySum(vLiq, vIbsTot, vCbs)
                : DanfseFormats.money(vTotNf);

        return new DanfseIbscbs(
                true,
                DanfseDom.join(" / ", DanfseDom.nullToDash(cst), DanfseDom.nullToDash(cClassTrib)),
                DanfseDom.join(" / ",
                        DanfseDom.nullToDash(cIndOp),
                        DanfseDom.nullToDash(cLoc),
                        DanfseDom.nullToDash(xLoc),
                        DanfseDom.nullToDash(ufIncidencia)),
                DanfseFormats.moneySum(vDescIncond, vCalcRee, vIssqn, vPis, vCofins),
                DanfseFormats.money(vBc),
                DanfseDom.join(" / ",
                        DanfseFormats.pctOrZero(DanfseDom.text(uf, "pRedAliqUF")),
                        DanfseFormats.pctOrZero(DanfseDom.text(mun, "pRedAliqMun")),
                        DanfseFormats.pctOrZero(DanfseDom.text(fed, "pRedAliqCBS"))),
                DanfseDom.join(" / ",
                        DanfseFormats.pct(DanfseDom.text(uf, "pIBSUF")),
                        DanfseFormats.pct(DanfseDom.text(mun, "pIBSMun"))),
                DanfseFormats.pct(DanfseDom.text(mun, "pAliqEfetMun")),
                DanfseFormats.money(vIbsMun),
                DanfseFormats.pct(DanfseDom.text(uf, "pAliqEfetUF")),
                DanfseFormats.money(vIbsUf),
                DanfseFormats.money(vIbsTot),
                DanfseFormats.pct(DanfseDom.text(fed, "pCBS")),
                DanfseFormats.pct(DanfseDom.text(fed, "pAliqEfetCBS")),
                DanfseFormats.money(vCbs),
                totIbsCbs,
                liqComIbsCbs);
    }

    void putInto(final Map<String, Object> parameters) {
        parameters.put("IBS_PRESENTE", presente);
        parameters.put("IBS_CST_CCLASS", cstCClassTrib);
        parameters.put("IBS_IND_OPER_LOC", indOperLocalidade);
        parameters.put("IBS_EXCL_BC", exclusoesBc);
        parameters.put("IBS_V_BC", vBc);
        parameters.put("IBS_P_RED_ALIQ", pRedAliqs);
        parameters.put("IBS_P_ALIQ_UF_MUN", pAliqIbsUfMun);
        parameters.put("IBS_P_ALIQ_EFET_MUN", pAliqEfetMun);
        parameters.put("IBS_V_IBS_MUN", vIbsMun);
        parameters.put("IBS_P_ALIQ_EFET_UF", pAliqEfetUf);
        parameters.put("IBS_V_IBS_UF", vIbsUf);
        parameters.put("IBS_V_IBS_TOT", vIbsTot);
        parameters.put("IBS_P_CBS", pCbs);
        parameters.put("IBS_P_ALIQ_EFET_CBS", pAliqEfetCbs);
        parameters.put("IBS_V_CBS", vCbs);
        parameters.put("IBS_V_TOT_IBSCBS", vTotIbsCbs);
        parameters.put("IBS_V_LIQ_COM_IBSCBS", vLiqComIbsCbs);
    }
}
