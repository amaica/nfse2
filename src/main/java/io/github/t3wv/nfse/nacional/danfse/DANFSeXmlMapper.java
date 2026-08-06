package io.github.t3wv.nfse.nacional.danfse;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Converte XML da NFS-e nacional nos parameters do {@code danfse.jrxml}.
 */
final class DANFSeXmlMapper {

    private static final String CONSULTA_URL = "https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave=";

    private DANFSeXmlMapper() {}

    static Map<String, Object> fromXml(final String xml) throws Exception {
        final Dados d = parse(xml);
        final Map<String, Object> p = new HashMap<>();
        p.put("CHAVE", DanfseDom.nullToEmpty(d.chave));
        p.put("CHAVE_FORMATADA", formatChave(d.chave));
        p.put("TP_AMB", DanfseDom.nullToEmpty(d.tpAmb));
        p.put("AMBIENTE_LABEL", ambienteLabel(d));
        p.put("X_LOC_EMI", DanfseDom.nullToEmpty(d.xLocEmi));
        p.put("N_NFSE", DanfseDom.nullToEmpty(d.nNFSe));
        p.put("D_COMPET", DanfseDom.nullToEmpty(d.dCompet));
        p.put("DH_PROC", DanfseDom.nullToEmpty(d.dhProc));
        p.put("SITUACAO", situacao(d.cStat));
        p.put("N_DPS", DanfseDom.nullToEmpty(d.nDPS));
        p.put("SERIE_DPS", DanfseDom.nullToEmpty(d.serie));
        p.put("DH_EMI_DPS", DanfseDom.nullToEmpty(d.dhEmi));
        p.put("FINALIDADE", "Normal");
        p.put("QR_CODE", qrImage(CONSULTA_URL + d.chave));
        p.put("QR_URL", CONSULTA_URL + d.chave);

        p.put("PREST_CNPJ", formatDoc(d.prestCnpj));
        p.put("PREST_NOME", truncate(DanfseDom.nullToEmpty(d.prestNome), 55));
        p.put("PREST_ENDERECO", truncate(DanfseDom.nullToEmpty(d.prestEndereco), 90));
        p.put("PREST_MUN_UF", DanfseDom.nullToEmpty(d.prestMun) + "/" + DanfseDom.nullToEmpty(d.prestUf));
        p.put("PREST_CEP", formatCep(d.prestCep));
        p.put("PREST_EMAIL", truncate(DanfseDom.nullToEmpty(d.prestEmail), 35));
        p.put("PREST_FONE", DanfseDom.nullToEmpty(d.prestFone));

        final boolean tomaOk = !DanfseDom.blank(d.tomaCnpj) || !DanfseDom.blank(d.tomaNome);
        p.put("TOMA_IDENTIFICADO", tomaOk);
        p.put("TOMA_CNPJ", formatDoc(d.tomaCnpj));
        p.put("TOMA_NOME", truncate(DanfseDom.nullToEmpty(d.tomaNome), 55));
        p.put("TOMA_ENDERECO", truncate(DanfseDom.nullToEmpty(d.tomaEndereco), 90));
        p.put("TOMA_MUN_IBGE", DanfseDom.nullToEmpty(d.tomaMun) + " IBGE " + DanfseDom.nullToEmpty(d.tomaIbge));
        p.put("TOMA_CEP", formatCep(d.tomaCep));
        p.put("TOMA_FONE", DanfseDom.nullToEmpty(d.tomaFone));

        p.put("C_TRIB_NAC", DanfseDom.nullToEmpty(d.cTribNac));
        p.put("C_NBS", DanfseDom.nullToEmpty(d.cNBS));
        p.put("X_LOC_PRESTACAO", DanfseDom.nullToEmpty(d.xLocPrestacao));
        p.put("X_TRIB_NAC", truncate(DanfseDom.nullToEmpty(d.xTribNac), 95));
        p.put("X_DESC_SERV", DanfseDom.nullToEmpty(d.xDescServ));

        p.put("X_LOC_INCID", truncate(DanfseDom.nullToEmpty(d.xLocIncid), 28));
        p.put("V_BC", DanfseFormats.money(d.vBC));
        p.put("P_ALIQ", DanfseFormats.pct(d.pAliqAplic));
        p.put("V_ISSQN", DanfseFormats.money(d.vISSQN));
        p.put("RETENCAO", retenIss(d.tpRetISSQN));
        p.put("TRIB_ISSQN", tribIss(d.tribISSQN));
        p.put("REG_ESP_TRIB", DanfseDom.nullToDash(d.regEspTrib));
        p.put("SIMPLES_NACIONAL", snLabel(d.opSimpNac));

        p.put("V_SERV", DanfseFormats.money(d.vServ));
        p.put("V_DESC", DanfseFormats.money("0.00"));
        p.put("V_LIQ", DanfseFormats.money(d.vLiq));

        d.ibscbs.putInto(p);

        String info = DanfseDom.nullToEmpty(d.xInfComp);
        if (!DanfseDom.blank(d.vTotTribFed) || !DanfseDom.blank(d.vTotTribEst) || !DanfseDom.blank(d.vTotTribMun)) {
            info = (info.isEmpty() ? "" : info + "\n")
                    + "Totais aproximados de tributos (Lei 12.741/2012): Fed "
                    + DanfseFormats.money(d.vTotTribFed)
                    + " | Est " + DanfseFormats.money(d.vTotTribEst)
                    + " | Mun " + DanfseFormats.money(d.vTotTribMun);
        }
        p.put("X_INF_COMP", info.isEmpty() ? "-" : info);
        p.put("RODAPE", "Documento gerado localmente conforme NT 008/2026 (API ADN suspensa). Chave: " + d.chave);
        return p;
    }

    private static BufferedImage qrImage(final String content) throws Exception {
        final Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        final var matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private static Dados parse(final String xml) throws Exception {
        final var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        final Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        final Element root = doc.getDocumentElement();

        final Dados d = new Dados();
        final Element inf = DanfseDom.first(root, "infNFSe");
        if (inf != null) {
            final String id = inf.getAttribute("Id");
            if (id != null && id.startsWith("NFS")) {
                d.chave = id.substring(3);
            }
            d.xLocEmi = DanfseDom.text(inf, "xLocEmi");
            d.xLocPrestacao = DanfseDom.text(inf, "xLocPrestacao");
            d.nNFSe = DanfseDom.text(inf, "nNFSe");
            d.xLocIncid = DanfseDom.text(inf, "xLocIncid");
            d.xTribNac = DanfseDom.text(inf, "xTribNac");
            d.cStat = DanfseDom.text(inf, "cStat");
            d.dhProc = DanfseDom.text(inf, "dhProc");
            final Element valoresNfse = DanfseDom.first(inf, "valores");
            d.vBC = DanfseDom.text(valoresNfse, "vBC");
            d.pAliqAplic = DanfseDom.text(valoresNfse, "pAliqAplic");
            d.vISSQN = DanfseDom.text(valoresNfse, "vISSQN");
            d.vLiq = DanfseDom.text(valoresNfse, "vLiq");

            final Element emit = DanfseDom.first(inf, "emit");
            if (emit != null) {
                d.prestCnpj = DanfseDom.firstText(emit, "CNPJ", "CPF");
                d.prestNome = DanfseDom.text(emit, "xNome");
                d.prestEmail = DanfseDom.text(emit, "email");
                d.prestFone = DanfseDom.text(emit, "fone");
                final Element end = DanfseDom.first(emit, "enderNac");
                if (end != null) {
                    d.prestEndereco = DanfseDom.join(" ",
                            DanfseDom.text(end, "xLgr"),
                            DanfseDom.text(end, "nro"),
                            DanfseDom.text(end, "xBairro"));
                    d.prestMun = d.xLocEmi;
                    d.prestUf = DanfseDom.text(end, "UF");
                    d.prestCep = DanfseDom.text(end, "CEP");
                }
            }
        }

        final Element dps = DanfseDom.first(inf != null ? inf : root, "DPS");
        final Element infDps = dps != null ? DanfseDom.first(dps, "infDPS") : DanfseDom.first(root, "infDPS");
        if (infDps != null) {
            d.tpAmb = DanfseDom.text(infDps, "tpAmb");
            d.dhEmi = DanfseDom.text(infDps, "dhEmi");
            d.serie = DanfseDom.text(infDps, "serie");
            d.nDPS = DanfseDom.text(infDps, "nDPS");
            d.dCompet = DanfseDom.text(infDps, "dCompet");
            final Element prest = DanfseDom.first(infDps, "prest");
            if (prest != null) {
                final Element reg = DanfseDom.first(prest, "regTrib");
                if (reg != null) {
                    d.opSimpNac = DanfseDom.text(reg, "opSimpNac");
                    d.regEspTrib = DanfseDom.text(reg, "regEspTrib");
                }
            }
            final Element toma = DanfseDom.first(infDps, "toma");
            if (toma != null) {
                d.tomaCnpj = DanfseDom.firstText(toma, "CNPJ", "CPF");
                d.tomaNome = DanfseDom.text(toma, "xNome");
                d.tomaFone = DanfseDom.text(toma, "fone");
                final Element end = DanfseDom.first(toma, "end");
                final Element endNac = end != null ? DanfseDom.first(end, "endNac") : null;
                if (end != null) {
                    d.tomaEndereco = DanfseDom.join(" ",
                            DanfseDom.text(end, "xLgr"),
                            DanfseDom.text(end, "nro"),
                            DanfseDom.text(end, "xBairro"));
                    if (endNac != null) {
                        d.tomaIbge = DanfseDom.text(endNac, "cMun");
                        d.tomaCep = DanfseDom.text(endNac, "CEP");
                    }
                }
            }
            final Element serv = DanfseDom.first(infDps, "serv");
            if (serv != null) {
                final Element cServ = DanfseDom.first(serv, "cServ");
                if (cServ != null) {
                    d.cTribNac = DanfseDom.text(cServ, "cTribNac");
                    d.xDescServ = DanfseDom.text(cServ, "xDescServ");
                    d.cNBS = DanfseDom.text(cServ, "cNBS");
                }
                final Element info = DanfseDom.first(serv, "infoCompl");
                if (info != null) {
                    d.xInfComp = DanfseDom.text(info, "xInfComp");
                }
            }
            final Element valores = DanfseDom.first(infDps, "valores");
            if (valores != null) {
                final Element vServPrest = DanfseDom.first(valores, "vServPrest");
                if (vServPrest != null) {
                    d.vServ = DanfseDom.text(vServPrest, "vServ");
                }
                final Element trib = DanfseDom.first(valores, "trib");
                if (trib != null) {
                    final Element tribMun = DanfseDom.first(trib, "tribMun");
                    if (tribMun != null) {
                        d.tribISSQN = DanfseDom.text(tribMun, "tribISSQN");
                        d.tpRetISSQN = DanfseDom.text(tribMun, "tpRetISSQN");
                    }
                    final Element tot = DanfseDom.first(trib, "totTrib");
                    final Element vTot = tot != null ? DanfseDom.first(tot, "vTotTrib") : null;
                    if (vTot != null) {
                        d.vTotTribFed = DanfseDom.text(vTot, "vTotTribFed");
                        d.vTotTribEst = DanfseDom.text(vTot, "vTotTribEst");
                        d.vTotTribMun = DanfseDom.text(vTot, "vTotTribMun");
                    }
                }
            }
        }

        if (DanfseDom.blank(d.chave)) {
            throw new IllegalArgumentException("XML NFS-e sem chave de acesso");
        }
        if (DanfseDom.blank(d.vServ)) {
            d.vServ = d.vLiq;
        }
        d.ibscbs = DanfseIbscbs.from(inf, infDps, d.prestUf);
        return d;
    }

    private static String truncate(String s, final int max) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String formatChave(final String chave) {
        if (chave == null || chave.length() != 50) {
            return DanfseDom.nullToEmpty(chave);
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i += 10) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(chave, i, Math.min(i + 10, 50));
        }
        return sb.toString();
    }

    private static String formatDoc(final String doc) {
        if (DanfseDom.blank(doc)) {
            return "";
        }
        final String d = doc.replaceAll("\\D", "");
        if (d.length() == 14) {
            return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
                    + "/" + d.substring(8, 12) + "-" + d.substring(12);
        }
        if (d.length() == 11) {
            return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
        }
        return doc;
    }

    private static String formatCep(final String cep) {
        if (DanfseDom.blank(cep)) {
            return "";
        }
        final String d = cep.replaceAll("\\D", "");
        if (d.length() == 8) {
            return d.substring(0, 5) + "-" + d.substring(5);
        }
        return cep;
    }

    private static String ambienteLabel(final Dados d) {
        if ("2".equals(d.tpAmb)) {
            return "Homologacao";
        }
        return "Producao";
    }

    private static String situacao(final String cStat) {
        if ("100".equals(cStat)) {
            return "Autorizada";
        }
        if ("101".equals(cStat)) {
            return "Cancelada";
        }
        return DanfseDom.nullToDash(cStat);
    }

    private static String retenIss(final String tp) {
        if ("1".equals(tp)) {
            return "Nao retido";
        }
        if ("2".equals(tp)) {
            return "Retido tomador";
        }
        if ("3".equals(tp)) {
            return "Retido intermediario";
        }
        return DanfseDom.nullToDash(tp);
    }

    private static String tribIss(final String t) {
        if ("1".equals(t)) {
            return "Operacao tributavel";
        }
        if ("2".equals(t)) {
            return "Imunidade";
        }
        if ("3".equals(t)) {
            return "Exportacao";
        }
        if ("4".equals(t)) {
            return "Nao incidencia";
        }
        return DanfseDom.nullToDash(t);
    }

    private static String snLabel(final String op) {
        if ("1".equals(op)) {
            return "Nao optante";
        }
        if ("2".equals(op)) {
            return "Optante MEI";
        }
        if ("3".equals(op)) {
            return "Optante ME/EPP";
        }
        return DanfseDom.nullToDash(op);
    }

    private static final class Dados {
        String chave;
        String xLocEmi, xLocPrestacao, nNFSe, xLocIncid, xTribNac, cStat, dhProc;
        String tpAmb, dhEmi, serie, nDPS, dCompet;
        String prestCnpj, prestNome, prestEndereco, prestMun, prestUf, prestCep, prestEmail, prestFone;
        String tomaCnpj, tomaNome, tomaEndereco, tomaMun, tomaIbge, tomaCep, tomaFone;
        String cTribNac, cNBS, xDescServ, xInfComp;
        String vBC, pAliqAplic, vISSQN, vLiq, vServ;
        String tribISSQN, tpRetISSQN, opSimpNac, regEspTrib;
        String vTotTribFed, vTotTribEst, vTotTribMun;
        DanfseIbscbs ibscbs = DanfseIbscbs.ausente();
    }
}
