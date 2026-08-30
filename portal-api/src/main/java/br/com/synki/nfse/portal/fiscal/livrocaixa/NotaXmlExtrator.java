package br.com.synki.nfse.portal.fiscal.livrocaixa;

import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaProcessada;
import com.fincatto.documentofiscal.utils.DFPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NotaXmlExtrator {

    private static final Logger log = LoggerFactory.getLogger(NotaXmlExtrator.class);
    private static final Pattern TAG = Pattern.compile("<([\\w:.-]+)[^>]*>([^<]*)</\\1>");

    private NotaXmlExtrator() {}

    public static Optional<LancamentoLivroCaixa> extrairNfe(String xml, String chaveFallback) {
        if (xml == null || xml.isBlank()) {
            return Optional.empty();
        }
        try {
            var proc = new DFPersister().read(NFNotaProcessada.class, xml);
            var nota = proc.getNota();
            var info = nota.getInfo();
            var ide = info.getIdentificacao();
            var totalStr = info.getTotal().getIcmsTotal().getValorTotalNFe();
            var total = parseDecimal(totalStr).orElse(BigDecimal.ZERO);
            var dest = info.getDestinatario();

            LocalDate data = ide.getDataHoraEmissao() != null
                    ? ide.getDataHoraEmissao().toLocalDate()
                    : LocalDate.now();

            String chave = info.getChaveAcesso() != null ? info.getChaveAcesso() : chaveFallback;
            String numero = ide.getNumeroNota() != null ? String.valueOf(ide.getNumeroNota()) : "";
            String nomeDest = dest != null && dest.getRazaoSocial() != null ? dest.getRazaoSocial() : "";
            String docDest = "";
            if (dest != null) {
                if (dest.getCpf() != null) {
                    docDest = dest.getCpf();
                } else if (dest.getCnpj() != null) {
                    docDest = dest.getCnpj();
                }
            }

            return Optional.of(new LancamentoLivroCaixa(
                    data,
                    "NF-e",
                    numero,
                    chave,
                    "Receita — NF-e nº " + numero + (nomeDest.isBlank() ? "" : " — " + nomeDest),
                    nomeDest,
                    docDest,
                    total.setScale(2, RoundingMode.HALF_UP),
                    LancamentoLivroCaixa.TipoMovimento.RECEITA));
        } catch (Exception ex) {
            log.debug("NF-e via fincatto falhou, tentando DOM: {}", ex.getMessage());
            return extrairNfeDom(xml, chaveFallback);
        }
    }

    private static Optional<LancamentoLivroCaixa> extrairNfeDom(String xml, String chaveFallback) {
        try {
            var doc = parse(xml);
            var data = parseDataNfe(doc);
            var valor = parseDecimal(primeiroTexto(doc, "vNF")).orElse(BigDecimal.ZERO);
            if (valor.signum() <= 0) {
                return Optional.empty();
            }
            var numero = primeiroTexto(doc, "nNF");
            var chave = extrairChaveNfe(xml).orElse(chaveFallback);
            var nomeDest = primeiroTexto(doc, "xNome");
            var docDest = coalesce(primeiroTexto(doc, "CNPJ"), primeiroTexto(doc, "CPF"));
            return Optional.of(new LancamentoLivroCaixa(
                    data,
                    "NF-e",
                    numero,
                    chave,
                    "Receita — NF-e nº " + numero,
                    nomeDest,
                    docDest,
                    valor.setScale(2, RoundingMode.HALF_UP),
                    LancamentoLivroCaixa.TipoMovimento.RECEITA));
        } catch (Exception ex) {
            log.debug("NF-e DOM falhou: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<LancamentoLivroCaixa> extrairNfse(String xml, String chaveFallback) {
        if (xml == null || xml.isBlank()) {
            return Optional.empty();
        }
        try {
            var doc = parse(xml);
            var valor = parseDecimal(coalesce(
                    primeiroTexto(doc, "vLiq"),
                    primeiroTexto(doc, "vServ"),
                    primeiroTexto(doc, "vBC"),
                    regex(xml, "<v(?:Liq|Serv|BC)[^>]*>([0-9.,]+)</"))).orElse(BigDecimal.ZERO);
            if (valor.signum() <= 0) {
                return Optional.empty();
            }
            var dataStr = coalesce(
                    primeiroTexto(doc, "dhProc"),
                    primeiroTexto(doc, "dhEmi"),
                    primeiroTexto(doc, "dCompet"),
                    regex(xml, "<d(?:hProc|hEmi|Compet)[^>]*>([^<]+)</"));
            var data = parseDataFlex(dataStr);
            var numero = coalesce(
                    primeiroTexto(doc, "nNFSe"),
                    primeiroTexto(doc, "nDPS"),
                    regex(xml, "<n(?:NFSe|DPS)[^>]*>([^<]+)</"));
            var chave = coalesce(
                    regex(xml, "(\\d{50})"),
                    chaveFallback);
            var tomador = coalesce(
                    regex(xml, "<xNomeTomador[^>]*>([^<]+)</"),
                    regex(xml, "<xNome[^>]*>([^<]+)</"));
            var docTomador = coalesce(
                    regex(xml, "<CNPJTomador[^>]*>([^<]+)</"),
                    regex(xml, "<CPFTomador[^>]*>([^<]+)</"),
                    regex(xml, "<CNPJ[^>]*>([^<]+)</"),
                    regex(xml, "<CPF[^>]*>([^<]+)</"));
            return Optional.of(new LancamentoLivroCaixa(
                    data,
                    "NFS-e",
                    numero,
                    chave,
                    "Receita — NFS-e" + (numero.isBlank() ? "" : " nº " + numero),
                    tomador,
                    docTomador,
                    valor.setScale(2, RoundingMode.HALF_UP),
                    LancamentoLivroCaixa.TipoMovimento.RECEITA));
        } catch (Exception ex) {
            log.debug("NFS-e parse falhou ({}): {}", chaveFallback, ex.getMessage());
            return Optional.empty();
        }
    }

    private static Document parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String primeiroTexto(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            var text = nodes.item(i).getTextContent();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        nodes = doc.getElementsByTagNameNS("*", tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            var text = nodes.item(i).getTextContent();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return "";
    }

    private static Optional<BigDecimal> parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(raw.replace(",", ".")).setScale(2, RoundingMode.HALF_UP));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static LocalDate parseDataNfe(Document doc) {
        var dh = coalesce(primeiroTexto(doc, "dhEmi"), primeiroTexto(doc, "dEmi"));
        return parseDataFlex(dh);
    }

    private static LocalDate parseDataFlex(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        var t = raw.trim();
        try {
            if (t.length() >= 10 && t.charAt(4) == '-') {
                return LocalDate.parse(t.substring(0, 10));
            }
            if (t.length() >= 10 && t.charAt(2) == '/') {
                return LocalDate.parse(t.substring(0, 10), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            if (t.length() == 7 && t.charAt(4) == '-') {
                return LocalDate.parse(t + "-01");
            }
            return OffsetDateTime.parse(t).toLocalDate();
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    public static Optional<LancamentoLivroCaixa> extrairNfeDespesa(String xml, String chaveFallback) {
        var meta = metadadosNfeEntrada(xml, chaveFallback);
        if (meta.valor() == null || meta.valor().signum() <= 0) {
            return Optional.empty();
        }
        String numero = meta.numero() != null ? meta.numero() : "";
        String nome = meta.nomeEmitente() != null ? meta.nomeEmitente() : "";
        return Optional.of(new LancamentoLivroCaixa(
                meta.data(),
                "NF-e entrada",
                numero,
                meta.chave(),
                "Despesa — NF-e nº " + numero + (nome.isBlank() ? "" : " — " + nome),
                nome,
                meta.cnpjEmitente() != null ? meta.cnpjEmitente() : "",
                meta.valor().setScale(2, RoundingMode.HALF_UP),
                LancamentoLivroCaixa.TipoMovimento.DESPESA));
    }

    public record MetadadosNfeEntrada(
            LocalDate data,
            String chave,
            String numero,
            String serie,
            String nomeEmitente,
            String cnpjEmitente,
            String natureza,
            java.math.BigDecimal valor
    ) {}

    public static MetadadosNfeEntrada metadadosNfeEntrada(String xml, String chaveFallback) {
        String chave = extrairChaveNfe(xml).orElse(chaveFallback);
        try {
            var proc = new DFPersister().read(NFNotaProcessada.class, xml);
            var info = proc.getNota().getInfo();
            var ide = info.getIdentificacao();
            var emit = info.getEmitente();
            var totalStr = info.getTotal().getIcmsTotal().getValorTotalNFe();
            var valor = parseDecimal(totalStr).orElse(BigDecimal.ZERO);
            LocalDate data = ide.getDataHoraEmissao() != null
                    ? ide.getDataHoraEmissao().toLocalDate()
                    : LocalDate.now();
            String docEmit = "";
            String nome = "";
            if (emit != null) {
                nome = emit.getRazaoSocial() != null ? emit.getRazaoSocial() : "";
                if (emit.getCnpj() != null) {
                    docEmit = emit.getCnpj();
                } else if (emit.getCpf() != null) {
                    docEmit = emit.getCpf();
                }
            }
            String numero = ide.getNumeroNota() != null ? String.valueOf(ide.getNumeroNota()) : "";
            String serie = ide.getSerie() != null ? String.valueOf(ide.getSerie()) : "";
            String nat = ide.getNaturezaOperacao() != null ? ide.getNaturezaOperacao() : "";
            return new MetadadosNfeEntrada(data, chave, numero, serie, nome, docEmit, nat, valor);
        } catch (Exception ex) {
            log.debug("metadados NF-e entrada via fincatto falhou: {}", ex.getMessage());
            return metadadosNfeEntradaDom(xml, chave);
        }
    }

    private static MetadadosNfeEntrada metadadosNfeEntradaDom(String xml, String chave) {
        try {
            var doc = parse(xml);
            var data = parseDataNfe(doc);
            var valor = parseDecimal(primeiroTexto(doc, "vNF")).orElse(BigDecimal.ZERO);
            var numero = primeiroTexto(doc, "nNF");
            var serie = primeiroTexto(doc, "serie");
            var nat = primeiroTexto(doc, "natOp");
            var nome = regex(xml, "<emit>[\\s\\S]*?<xNome>([^<]+)</xNome>");
            var cnpj = coalesce(
                    regex(xml, "<emit>[\\s\\S]*?<CNPJ>([^<]+)</CNPJ>"),
                    regex(xml, "<emit>[\\s\\S]*?<CPF>([^<]+)</CPF>"));
            return new MetadadosNfeEntrada(data, chave, numero, serie, nome, cnpj, nat, valor);
        } catch (Exception ex) {
            return new MetadadosNfeEntrada(LocalDate.now(), chave, "", "", "", "", "", BigDecimal.ZERO);
        }
    }

    public static Optional<String> extrairChaveNfe(String xml) {
        if (xml == null || xml.isBlank()) {
            return Optional.empty();
        }
        Matcher m = Pattern.compile("Id=\"NFe(\\d{44})\"").matcher(xml);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        m = Pattern.compile("<chNFe>(\\d{44})</chNFe>").matcher(xml);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static String regex(String xml, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(xml);
        return m.find() ? m.group(1).trim() : "";
    }

    private static String coalesce(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }
}
