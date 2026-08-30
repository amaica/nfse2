package br.com.synki.nfse.portal.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Gera DANFSe localmente a partir do XML da NFS-e nacional (NT 008/2026 v1.02).
 * A API ADN ({@code adn.nfse.gov.br/danfse}) foi suspensa em 03/08/2026.
 */
@Service
public class DanfseLocalPdfService {

    private static final float MARGIN = 14f;
    private static final Color GRAY_HEADER = new Color(240, 240, 240);
    private static final String CONSULTA_URL = "https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave=";

    public byte[] gerarDeXml(String xml) throws Exception {
        var dados = parse(xml);
        try (var doc = new PDDocument(); var baos = new ByteArrayOutputStream()) {
            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float contentW = pageW - 2 * MARGIN;
            float y = pageH - MARGIN;

            try (var cs = new PDPageContentStream(doc, page)) {
                // borda externa 1pt
                cs.setStrokingColor(Color.BLACK);
                cs.setLineWidth(1f);
                cs.addRect(MARGIN - 2, MARGIN - 2, contentW + 4, pageH - 2 * MARGIN + 4);
                cs.stroke();

                y = drawHeader(doc, cs, dados, MARGIN, y, contentW);
                y -= 4;
                y = drawIdentificacao(cs, dados, MARGIN, y, contentW);
                y -= 4;
                y = drawPrestador(cs, dados, MARGIN, y, contentW);
                y -= 4;
                y = drawTomador(cs, dados, MARGIN, y, contentW);
                y -= 4;
                y = drawServico(cs, dados, MARGIN, y, contentW);
                y -= 4;
                y = drawIssqn(cs, dados, MARGIN, y, contentW);
                y -= 4;
                y = drawValores(cs, dados, MARGIN, y, contentW);
                y -= 4;
                drawComplementares(cs, dados, MARGIN, y, contentW, MARGIN + 20);
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private float drawHeader(PDDocument doc, PDPageContentStream cs, Dados d,
                             float x, float y, float w) throws Exception {
        float h = 72f;
        fillRect(cs, x, y - h, w, h, GRAY_HEADER);
        strokeRect(cs, x, y - h, w, h);

        // Título
        text(cs, PDType1Font.HELVETICA_BOLD, 11, x + 8, y - 16, "DANFSe v2.0");
        text(cs, PDType1Font.HELVETICA_BOLD, 9, x + 8, y - 28, "Documento Auxiliar da NFS-e");
        // NT 008: aviso só em homologação (tpAmb=2). ambGer NÃO é ambiente fiscal.
        if ("2".equals(d.tpAmb)) {
            cs.setNonStrokingColor(Color.RED);
            text(cs, PDType1Font.HELVETICA_BOLD, 9, x + 8, y - 40, "NFS-e SEM VALIDADE JURIDICA");
            cs.setNonStrokingColor(Color.BLACK);
        }
        text(cs, PDType1Font.HELVETICA, 8, x + 8, y - 52,
                truncate(nullToEmpty(d.xLocEmi), 40));
        text(cs, PDType1Font.HELVETICA, 6, x + 8, y - 62,
                "Ambiente: " + ambienteLabel(d) + "  |  Emitente: Prestador");

        // QR Code à direita
        float qrSize = 48f; // ~1.7cm
        float qrX = x + w - qrSize - 10;
        float qrY = y - h + 10;
        var qrImg = qrImage(CONSULTA_URL + d.chave);
        PDImageXObject pdImg = LosslessFactory.createFromImage(doc, qrImg);
        cs.drawImage(pdImg, qrX, qrY, qrSize, qrSize);
        text(cs, PDType1Font.HELVETICA, 5, x + w - 120, y - h + 4,
                "Consulte a autenticidade no portal nacional");

        return y - h;
    }

    private float drawIdentificacao(PDPageContentStream cs, Dados d, float x, float y, float w) throws Exception {
        float h = 54f;
        y = blockTitle(cs, x, y, w, "IDENTIFICACAO DA NFS-e");
        strokeRect(cs, x, y - h, w, h);
        float col = w / 4f;
        labelValue(cs, x + 4, y - 12, "CHAVE DE ACESSO", formatChave(d.chave), 7);
        labelValue(cs, x + 4, y - 28, "NUMERO", nullToEmpty(d.nNFSe), 8);
        labelValue(cs, x + col, y - 28, "COMPETENCIA", nullToEmpty(d.dCompet), 8);
        labelValue(cs, x + 2 * col, y - 28, "DH EMISSAO NFS-e", nullToEmpty(d.dhProc), 7);
        labelValue(cs, x + 3 * col, y - 28, "SITUACAO", situacao(d.cStat), 8);
        labelValue(cs, x + 4, y - 44, "NUMERO DPS", nullToEmpty(d.nDPS), 8);
        labelValue(cs, x + col, y - 44, "SERIE DPS", nullToEmpty(d.serie), 8);
        labelValue(cs, x + 2 * col, y - 44, "DH EMISSAO DPS", nullToEmpty(d.dhEmi), 7);
        labelValue(cs, x + 3 * col, y - 44, "FINALIDADE", "Normal", 8);
        return y - h;
    }

    private float drawPrestador(PDPageContentStream cs, Dados d, float x, float y, float w) throws Exception {
        float h = 58f;
        y = blockTitle(cs, x, y, w, "PRESTADOR / FORNECEDOR");
        strokeRect(cs, x, y - h, w, h);
        labelValue(cs, x + 4, y - 12, "CNPJ/CPF", formatDoc(d.prestCnpj), 8);
        labelValue(cs, x + w * 0.35f, y - 12, "NOME", truncate(nullToEmpty(d.prestNome), 55), 8);
        labelValue(cs, x + 4, y - 28, "ENDERECO", truncate(d.prestEndereco, 90), 7);
        labelValue(cs, x + 4, y - 44, "MUNICIPIO/UF", nullToEmpty(d.prestMun) + "/" + nullToEmpty(d.prestUf), 8);
        labelValue(cs, x + w * 0.35f, y - 44, "CEP", formatCep(d.prestCep), 8);
        labelValue(cs, x + w * 0.55f, y - 44, "EMAIL", truncate(nullToEmpty(d.prestEmail), 35), 7);
        labelValue(cs, x + w * 0.82f, y - 44, "FONE", nullToEmpty(d.prestFone), 7);
        return y - h;
    }

    private float drawTomador(PDPageContentStream cs, Dados d, float x, float y, float w) throws Exception {
        float h = 58f;
        y = blockTitle(cs, x, y, w, "TOMADOR / ADQUIRENTE");
        strokeRect(cs, x, y - h, w, h);
        if (blank(d.tomaCnpj) && blank(d.tomaNome)) {
            text(cs, PDType1Font.HELVETICA_BOLD, 8, x + 4, y - 28,
                    "TOMADOR/ADQUIRENTE DA OPERACAO NAO IDENTIFICADO NA NFS-e");
            return y - h;
        }
        labelValue(cs, x + 4, y - 12, "CNPJ/CPF", formatDoc(d.tomaCnpj), 8);
        labelValue(cs, x + w * 0.35f, y - 12, "NOME", truncate(nullToEmpty(d.tomaNome), 55), 8);
        labelValue(cs, x + 4, y - 28, "ENDERECO", truncate(d.tomaEndereco, 90), 7);
        labelValue(cs, x + 4, y - 44, "MUNICIPIO/UF", nullToEmpty(d.tomaMun) + " IBGE " + nullToEmpty(d.tomaIbge), 7);
        labelValue(cs, x + w * 0.45f, y - 44, "CEP", formatCep(d.tomaCep), 8);
        labelValue(cs, x + w * 0.65f, y - 44, "FONE", nullToEmpty(d.tomaFone), 7);
        return y - h;
    }

    private float drawServico(PDPageContentStream cs, Dados d, float x, float y, float w) throws Exception {
        float h = 78f;
        y = blockTitle(cs, x, y, w, "SERVICO PRESTADO");
        strokeRect(cs, x, y - h, w, h);
        labelValue(cs, x + 4, y - 12, "Cod. Tributacao Nacional", nullToEmpty(d.cTribNac), 8);
        labelValue(cs, x + w * 0.35f, y - 12, "NBS", nullToEmpty(d.cNBS), 8);
        labelValue(cs, x + w * 0.55f, y - 12, "Local Prestacao", nullToEmpty(d.xLocPrestacao), 8);
        labelValue(cs, x + 4, y - 28, "Descricao do Codigo", truncate(nullToEmpty(d.xTribNac), 95), 7);
        text(cs, PDType1Font.HELVETICA_BOLD, 6, x + 4, y - 42, "Descricao do Servico");
        drawWrapped(cs, PDType1Font.HELVETICA, 7, x + 4, y - 52, w - 8, nullToEmpty(d.xDescServ), 3);
        return y - h;
    }

    private float drawIssqn(PDPageContentStream cs, Dados d, float x, float y, float w) throws Exception {
        float h = 42f;
        y = blockTitle(cs, x, y, w, "TRIBUTACAO MUNICIPAL (ISSQN)");
        strokeRect(cs, x, y - h, w, h);
        float col = w / 5f;
        labelValue(cs, x + 4, y - 12, "Municipio Incidencia", truncate(nullToEmpty(d.xLocIncid), 28), 7);
        labelValue(cs, x + col, y - 12, "BC ISSQN", money(d.vBC), 8);
        labelValue(cs, x + 2 * col, y - 12, "Aliquota", pct(d.pAliqAplic), 8);
        labelValue(cs, x + 3 * col, y - 12, "ISSQN", money(d.vISSQN), 8);
        labelValue(cs, x + 4 * col, y - 12, "Retencao", retenIss(d.tpRetISSQN), 7);
        labelValue(cs, x + 4, y - 28, "Tributacao ISSQN", tribIss(d.tribISSQN), 7);
        labelValue(cs, x + 2 * col, y - 28, "Regime Especial", nullToDash(d.regEspTrib), 7);
        labelValue(cs, x + 3.5f * col, y - 28, "Simples Nacional", snLabel(d.opSimpNac), 7);
        return y - h;
    }

    private float drawValores(PDPageContentStream cs, Dados d, float x, float y, float w) throws Exception {
        float h = 36f;
        y = blockTitle(cs, x, y, w, "VALOR TOTAL DA NFS-e");
        fillRect(cs, x, y - h, w, h, GRAY_HEADER);
        strokeRect(cs, x, y - h, w, h);
        float col = w / 4f;
        labelValue(cs, x + 4, y - 14, "Valor do Servico", money(d.vServ), 9);
        labelValue(cs, x + col, y - 14, "Descontos", money("0.00"), 9);
        labelValue(cs, x + 2 * col, y - 14, "Valor Liquido", money(d.vLiq), 9);
        text(cs, PDType1Font.HELVETICA_BOLD, 9, x + 3 * col, y - 18, "Liquido NFS-e: " + money(d.vLiq));
        return y - h;
    }

    private void drawComplementares(PDPageContentStream cs, Dados d, float x, float y, float w, float minY) throws Exception {
        float h = Math.max(40f, y - minY);
        y = blockTitle(cs, x, y, w, "INFORMACOES COMPLEMENTARES");
        strokeRect(cs, x, y - h, w, h);
        String info = nullToEmpty(d.xInfComp);
        if (!blank(d.vTotTribFed) || !blank(d.vTotTribEst) || !blank(d.vTotTribMun)) {
            info = (info.isEmpty() ? "" : info + "\n")
                    + "Totais aproximados de tributos (Lei 12.741/2012): Fed "
                    + money(d.vTotTribFed) + " | Est " + money(d.vTotTribEst) + " | Mun " + money(d.vTotTribMun);
        }
        drawWrapped(cs, PDType1Font.HELVETICA, 7, x + 4, y - 14, w - 8, info.isEmpty() ? "-" : info, 6);
        text(cs, PDType1Font.HELVETICA, 5, x + 4, y - h + 6,
                "Documento gerado localmente conforme NT 008/2026 (API ADN suspensa). Chave: " + d.chave);
    }

    // ---- drawing helpers ----

    private float blockTitle(PDPageContentStream cs, float x, float y, float w, String title) throws Exception {
        float th = 14f;
        fillRect(cs, x, y - th, w, th, GRAY_HEADER);
        strokeRect(cs, x, y - th, w, th);
        text(cs, PDType1Font.HELVETICA_BOLD, 7, x + 4, y - 10, title);
        return y - th;
    }

    private void labelValue(PDPageContentStream cs, float x, float y, String label, String value, float size) throws Exception {
        text(cs, PDType1Font.HELVETICA_BOLD, 5.5f, x, y, label);
        text(cs, PDType1Font.HELVETICA, size, x, y - 10, value == null ? "" : value);
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String s) throws Exception {
        if (s == null) s = "";
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(s));
        cs.endText();
    }

    private void drawWrapped(PDPageContentStream cs, PDType1Font font, float size,
                             float x, float y, float maxW, String text, int maxLines) throws Exception {
        List<String> lines = wrap(font, size, text, maxW);
        int n = Math.min(maxLines, lines.size());
        for (int i = 0; i < n; i++) {
            text(cs, font, size, x, y - i * (size + 2), lines.get(i));
        }
        if (lines.size() > maxLines) {
            text(cs, font, size, x, y - maxLines * (size + 2), "...");
        }
    }

    private List<String> wrap(PDType1Font font, float size, String text, float maxW) throws Exception {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            out.add("");
            return out;
        }
        String[] words = sanitize(text).replace('\n', ' ').split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String trial = line.isEmpty() ? word : line + " " + word;
            float tw = font.getStringWidth(trial) / 1000f * size;
            if (tw > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws Exception {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
    }

    private void strokeRect(PDPageContentStream cs, float x, float y, float w, float h) throws Exception {
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private BufferedImage qrImage(String content) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        var matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    // ---- parse XML ----

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
            d.ambGer = text(inf, "ambGer");
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
        if (list.getLength() == 0) {
            list = parent.getElementsByTagName(local);
        }
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

    private static String sanitize(String s) {
        // WinAnsi não tem alguns chars — normaliza
        return s.replace('\u2014', '-')
                .replace('\u2013', '-')
                .replace('\u00A0', ' ')
                .replace("ç", "c").replace("Ç", "C")
                .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
                .replace("Á", "A").replace("À", "A").replace("Ã", "A").replace("Â", "A")
                .replace("é", "e").replace("ê", "e").replace("É", "E").replace("Ê", "E")
                .replace("í", "i").replace("Í", "I")
                .replace("ó", "o").replace("õ", "o").replace("ô", "o")
                .replace("Ó", "O").replace("Õ", "O").replace("Ô", "O")
                .replace("ú", "u").replace("Ú", "U")
                .replace("º", "o").replace("ª", "a")
                .replaceAll("[^\\x20-\\x7E]", "?");
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
        String xLocEmi, xLocPrestacao, nNFSe, xLocIncid, xTribNac, ambGer, cStat, dhProc;
        String tpAmb, dhEmi, serie, nDPS, dCompet;
        String prestCnpj, prestNome, prestEndereco, prestMun, prestUf, prestCep, prestEmail, prestFone;
        String tomaCnpj, tomaNome, tomaEndereco, tomaMun, tomaIbge, tomaCep, tomaFone;
        String cTribNac, cNBS, xDescServ, xInfComp;
        String vBC, pAliqAplic, vISSQN, vLiq, vServ;
        String tribISSQN, tpRetISSQN, opSimpNac, regEspTrib;
        String vTotTribFed, vTotTribEst, vTotTribMun;
    }
}
