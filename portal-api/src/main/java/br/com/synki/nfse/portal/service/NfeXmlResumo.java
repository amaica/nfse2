package br.com.synki.nfse.portal.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Resumo da NF-e extraído do XML para listagem (sem carregar o XML na grade). */
public record NfeXmlResumo(
        BigDecimal valorTotal,
        String destinatarioNome,
        String cfop,
        String tipoOperacao,
        String produtosResumo) {

    public static NfeXmlResumo extrair(String xml) {
        if (xml == null || xml.isBlank()) {
            return vazio();
        }
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            var valor = parseDecimal(texto(doc, "vNF"));
            var dest = primeiroFilho(doc, "dest");
            var nome = dest != null ? textoLocal(dest, "xNome") : null;
            var tpNf = texto(doc, "tpNF");
            var tipo = "0".equals(tpNf == null ? "" : tpNf.trim()) ? "Entrada" : "Saída";
            var cfops = new ArrayList<String>();
            var produtos = new ArrayList<String>();
            NodeList dets = doc.getElementsByTagNameNS("*", "det");
            if (dets.getLength() == 0) {
                dets = doc.getElementsByTagName("det");
            }
            for (int i = 0; i < dets.getLength(); i++) {
                if (!(dets.item(i) instanceof Element det)) {
                    continue;
                }
                var cfop = textoLocal(det, "CFOP");
                if (cfop != null && !cfop.isBlank() && !cfops.contains(cfop)) {
                    cfops.add(cfop.trim());
                }
                var xProd = textoLocal(det, "xProd");
                if (xProd != null && !xProd.isBlank()) {
                    produtos.add(xProd.trim());
                }
            }
            return new NfeXmlResumo(
                    valor,
                    cortar(nome, 255),
                    cortar(String.join(", ", cfops), 60),
                    tipo,
                    String.join("\n", produtos));
        } catch (Exception ignored) {
            return vazio();
        }
    }

    public static NfeXmlResumo vazio() {
        return new NfeXmlResumo(null, null, null, null, null);
    }

    private static Element primeiroFilho(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagNameNS("*", tag);
        if (nodes.getLength() == 0) {
            nodes = doc.getElementsByTagName(tag);
        }
        return nodes.getLength() > 0 && nodes.item(0) instanceof Element el ? el : null;
    }

    private static String texto(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagNameNS("*", tag);
        if (nodes.getLength() == 0) {
            nodes = doc.getElementsByTagName(tag);
        }
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private static String textoLocal(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagNameNS("*", tag);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(tag);
        }
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(",", ".")).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String cortar(String s, int max) {
        if (s == null) {
            return null;
        }
        var t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    public List<String> produtos() {
        if (produtosResumo == null || produtosResumo.isBlank()) {
            return List.of();
        }
        return List.of(produtosResumo.split("\\n"));
    }
}
