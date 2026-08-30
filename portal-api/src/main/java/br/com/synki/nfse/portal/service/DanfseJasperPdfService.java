package br.com.synki.nfse.portal.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Gera DANFSe via JasperReports ({@code /danfse/danfse.jrxml}),
 * layout alinhado ao PDF local NT 008/2026.
 */
@Service
public class DanfseJasperPdfService {

    private static final Logger log = LoggerFactory.getLogger(DanfseJasperPdfService.class);
    private static final String JRXML = "danfse/danfse.jrxml";
    private static final String CONSULTA_URL = "https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave=";

    static {
        JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance())
                .setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
    }

    private volatile JasperReport compiled;

    public byte[] gerarDeXml(String xml) throws Exception {
        Dados d = parse(xml);
        Map<String, Object> params = toParams(d);
        JasperReport report = compiledReport();
        JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource(1));
        return JasperExportManager.exportReportToPdf(print);
    }

    private JasperReport compiledReport() throws Exception {
        JasperReport cached = compiled;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (compiled == null) {
                try (InputStream in = new ClassPathResource(JRXML).getInputStream()) {
                    compiled = JasperCompileManager.compileReport(in);
                    log.info("DANFSe Jasper compilado: {}", JRXML);
                }
            }
            return compiled;
        }
    }

    private Map<String, Object> toParams(Dados d) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("CHAVE", nullToEmpty(d.chave));
        p.put("CHAVE_FORMATADA", formatChave(d.chave));
        p.put("TP_AMB", nullToEmpty(d.tpAmb));
        p.put("AMBIENTE_LABEL", ambienteLabel(d));
        p.put("X_LOC_EMI", nullToEmpty(d.xLocEmi));
        p.put("N_NFSE", nullToEmpty(d.nNFSe));
        p.put("D_COMPET", nullToEmpty(d.dCompet));
        p.put("DH_PROC", nullToEmpty(d.dhProc));
        p.put("SITUACAO", situacao(d.cStat));
        p.put("N_DPS", nullToEmpty(d.nDPS));
        p.put("SERIE_DPS", nullToEmpty(d.serie));
        p.put("DH_EMI_DPS", nullToEmpty(d.dhEmi));
        p.put("FINALIDADE", "Normal");
        p.put("QR_CODE", qrImage(CONSULTA_URL + d.chave));
        p.put("QR_URL", CONSULTA_URL + d.chave);

        p.put("PREST_CNPJ", formatDoc(d.prestCnpj));
        p.put("PREST_NOME", truncate(nullToEmpty(d.prestNome), 55));
        p.put("PREST_ENDERECO", truncate(nullToEmpty(d.prestEndereco), 90));
        p.put("PREST_MUN_UF", nullToEmpty(d.prestMun) + "/" + nullToEmpty(d.prestUf));
        p.put("PREST_CEP", formatCep(d.prestCep));
        p.put("PREST_EMAIL", truncate(nullToEmpty(d.prestEmail), 35));
        p.put("PREST_FONE", nullToEmpty(d.prestFone));

        boolean tomaOk = !blank(d.tomaCnpj) || !blank(d.tomaNome);
        p.put("TOMA_IDENTIFICADO", tomaOk);
        p.put("TOMA_CNPJ", formatDoc(d.tomaCnpj));
        p.put("TOMA_NOME", truncate(nullToEmpty(d.tomaNome), 55));
        p.put("TOMA_ENDERECO", truncate(nullToEmpty(d.tomaEndereco), 90));
        p.put("TOMA_MUN_IBGE", nullToEmpty(d.tomaMun) + " IBGE " + nullToEmpty(d.tomaIbge));
        p.put("TOMA_CEP", formatCep(d.tomaCep));
        p.put("TOMA_FONE", nullToEmpty(d.tomaFone));

        p.put("C_TRIB_NAC", nullToEmpty(d.cTribNac));
        p.put("C_NBS", nullToEmpty(d.cNBS));
        p.put("X_LOC_PRESTACAO", nullToEmpty(d.xLocPrestacao));
        p.put("X_TRIB_NAC", truncate(nullToEmpty(d.xTribNac), 95));
        p.put("X_DESC_SERV", nullToEmpty(d.xDescServ));

        p.put("X_LOC_INCID", truncate(nullToEmpty(d.xLocIncid), 28));
        p.put("V_BC", money(d.vBC));
        p.put("P_ALIQ", pct(d.pAliqAplic));
        p.put("V_ISSQN", money(d.vISSQN));
        p.put("RETENCAO", retenIss(d.tpRetISSQN));
        p.put("TRIB_ISSQN", tribIss(d.tribISSQN));
        p.put("REG_ESP_TRIB", nullToDash(d.regEspTrib));
        p.put("SIMPLES_NACIONAL", snLabel(d.opSimpNac));

        p.put("V_SERV", money(d.vServ));
        p.put("V_DESC", money("0.00"));
        p.put("V_LIQ", money(d.vLiq));

        String info = nullToEmpty(d.xInfComp);
        if (!blank(d.vTotTribFed) || !blank(d.vTotTribEst) || !blank(d.vTotTribMun)) {
            info = (info.isEmpty() ? "" : info + "\n")
                    + "Totais aproximados de tributos (Lei 12.741/2012): Fed "
                    + money(d.vTotTribFed) + " | Est " + money(d.vTotTribEst) + " | Mun " + money(d.vTotTribMun);
        }
        p.put("X_INF_COMP", info.isEmpty() ? "-" : info);
        p.put("RODAPE", "Documento gerado localmente conforme NT 008/2026 (API ADN suspensa). Chave: " + d.chave);
        return p;
    }

    private BufferedImage qrImage(String content) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        var matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    // ---- parse XML (mesmo contrato do gerador PDFBox) ----

    private Dados parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element root = doc.getDocumentElement();

        Dados d = new Dados();
        Element inf = first(root, "infNFSe");
        if (inf != null) {
            String id = inf.getAttribute("Id");
            if (id != null && id.startsWith("NFS")) {
                d.chave = id.substring(3);
            }
            d.xLocEmi = text(inf, "xLocEmi");
            d.xLocPrestacao = text(inf, "xLocPrestacao");
            d.nNFSe = text(inf, "nNFSe");
            d.xLocIncid = text(inf, "xLocIncid");
            d.xTribNac = text(inf, "xTribNac");
            d.cStat = text(inf, "cStat");
            d.dhProc = text(inf, "dhProc");
            d.vBC = text(first(inf, "valores"), "vBC");
            d.pAliqAplic = text(first(inf, "valores"), "pAliqAplic");
            d.vISSQN = text(first(inf, "valores"), "vISSQN");
            d.vLiq = text(first(inf, "valores"), "vLiq");

            Element emit = first(inf, "emit");
            if (emit != null) {
                d.prestCnpj = firstText(emit, "CNPJ", "CPF");
                d.prestNome = text(emit, "xNome");
                d.prestEmail = text(emit, "email");
                d.prestFone = text(emit, "fone");
                Element end = first(emit, "enderNac");
                if (end != null) {
                    d.prestEndereco = join(" ", text(end, "xLgr"), text(end, "nro"), text(end, "xBairro"));
                    d.prestMun = d.xLocEmi;
                    d.prestUf = text(end, "UF");
                    d.prestCep = text(end, "CEP");
                }
            }
        }

        Element dps = first(inf != null ? inf : root, "DPS");
        Element infDps = dps != null ? first(dps, "infDPS") : first(root, "infDPS");
        if (infDps != null) {
            d.tpAmb = text(infDps, "tpAmb");
            d.dhEmi = text(infDps, "dhEmi");
            d.serie = text(infDps, "serie");
            d.nDPS = text(infDps, "nDPS");
            d.dCompet = text(infDps, "dCompet");
            Element prest = first(infDps, "prest");
            if (prest != null) {
                Element reg = first(prest, "regTrib");
                if (reg != null) {
                    d.opSimpNac = text(reg, "opSimpNac");
                    d.regEspTrib = text(reg, "regEspTrib");
                }
            }
            Element toma = first(infDps, "toma");
            if (toma != null) {
                d.tomaCnpj = firstText(toma, "CNPJ", "CPF");
                d.tomaNome = text(toma, "xNome");
                d.tomaFone = text(toma, "fone");
                Element end = first(toma, "end");
                Element endNac = end != null ? first(end, "endNac") : null;
                if (end != null) {
                    d.tomaEndereco = join(" ", text(end, "xLgr"), text(end, "nro"), text(end, "xBairro"));
                    if (endNac != null) {
                        d.tomaIbge = text(endNac, "cMun");
                        d.tomaCep = text(endNac, "CEP");
                    }
                }
            }
            Element serv = first(infDps, "serv");
            if (serv != null) {
                Element cServ = first(serv, "cServ");
                if (cServ != null) {
                    d.cTribNac = text(cServ, "cTribNac");
                    d.xDescServ = text(cServ, "xDescServ");
                    d.cNBS = text(cServ, "cNBS");
                }
                Element info = first(serv, "infoCompl");
                if (info != null) d.xInfComp = text(info, "xInfComp");
            }
            Element valores = first(infDps, "valores");
            if (valores != null) {
                Element vServPrest = first(valores, "vServPrest");
                if (vServPrest != null) d.vServ = text(vServPrest, "vServ");
                Element trib = first(valores, "trib");
                if (trib != null) {
                    Element tribMun = first(trib, "tribMun");
                    if (tribMun != null) {
                        d.tribISSQN = text(tribMun, "tribISSQN");
                        d.tpRetISSQN = text(tribMun, "tpRetISSQN");
                    }
                    Element tot = first(trib, "totTrib");
                    Element vTot = tot != null ? first(tot, "vTotTrib") : null;
                    if (vTot != null) {
                        d.vTotTribFed = text(vTot, "vTotTribFed");
                        d.vTotTribEst = text(vTot, "vTotTribEst");
                        d.vTotTribMun = text(vTot, "vTotTribMun");
                    }
                }
            }
        }
        if (blank(d.chave)) {
            throw new IllegalArgumentException("XML NFS-e sem chave de acesso");
        }
        if (blank(d.vServ)) d.vServ = d.vLiq;
        return d;
    }

    private static Element first(Element parent, String local) {
        if (parent == null) return null;
        NodeList list = parent.getElementsByTagNameNS("*", local);
        if (list.getLength() == 0) list = parent.getElementsByTagName(local);
        if (list.getLength() == 0) return null;
        Node n = list.item(0);
        return n instanceof Element e ? e : null;
    }

    private static String text(Element parent, String local) {
        Element e = first(parent, local);
        if (e == null) return null;
        String t = e.getTextContent();
        return t == null ? null : t.trim();
    }

    private static String firstText(Element parent, String... locals) {
        for (String l : locals) {
            String t = text(parent, l);
            if (!blank(t)) return t;
        }
        return null;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String nullToDash(String s) {
        return blank(s) ? "-" : s;
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (blank(p)) continue;
            if (!sb.isEmpty()) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String formatChave(String chave) {
        if (chave == null || chave.length() != 50) return nullToEmpty(chave);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i += 10) {
            if (i > 0) sb.append(' ');
            sb.append(chave, i, Math.min(i + 10, 50));
        }
        return sb.toString();
    }

    private static String formatDoc(String doc) {
        if (blank(doc)) return "";
        String d = doc.replaceAll("\\D", "");
        if (d.length() == 14) {
            return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
                    + "/" + d.substring(8, 12) + "-" + d.substring(12);
        }
        if (d.length() == 11) {
            return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
        }
        return doc;
    }

    private static String formatCep(String cep) {
        if (blank(cep)) return "";
        String d = cep.replaceAll("\\D", "");
        if (d.length() == 8) return d.substring(0, 5) + "-" + d.substring(5);
        return cep;
    }

    private static String money(String v) {
        if (blank(v)) return "0,00";
        try {
            return String.format("%,.2f", Double.parseDouble(v)).replace(',', 'X').replace('.', ',').replace('X', '.');
        } catch (Exception e) {
            return v;
        }
    }

    private static String pct(String v) {
        if (blank(v)) return "-";
        return v.replace('.', ',') + "%";
    }

    private static String ambienteLabel(Dados d) {
        if ("2".equals(d.tpAmb)) return "Homologacao";
        return "Producao";
    }

    private static String situacao(String cStat) {
        if ("100".equals(cStat)) return "Autorizada";
        if ("101".equals(cStat)) return "Cancelada";
        return nullToDash(cStat);
    }

    private static String retenIss(String tp) {
        if ("1".equals(tp)) return "Nao retido";
        if ("2".equals(tp)) return "Retido tomador";
        if ("3".equals(tp)) return "Retido intermediario";
        return nullToDash(tp);
    }

    private static String tribIss(String t) {
        if ("1".equals(t)) return "Operacao tributavel";
        if ("2".equals(t)) return "Imunidade";
        if ("3".equals(t)) return "Exportacao";
        if ("4".equals(t)) return "Nao incidencia";
        return nullToDash(t);
    }

    private static String snLabel(String op) {
        if ("1".equals(op)) return "Nao optante";
        if ("2".equals(op)) return "Optante MEI";
        if ("3".equals(op)) return "Optante ME/EPP";
        return nullToDash(op);
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
    }
}
