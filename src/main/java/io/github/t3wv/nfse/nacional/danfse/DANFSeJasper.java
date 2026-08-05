package io.github.t3wv.nfse.nacional.danfse;

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

import java.io.InputStream;
import java.util.Map;

/**
 * Gera o DANFSe (PDF) localmente via JasperReports — NT 008/2026.
 * Template: {@code /danfse/danfse.jrxml}.
 */
public final class DANFSeJasper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DANFSeJasper.class);
    private static final String JRXML = "/danfse/danfse.jrxml";
    private static volatile JasperReport compiled;

    static {
        JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance())
                .setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
    }

    private DANFSeJasper() {}

    /**
     * Gera o PDF a partir do XML autorizado da NFS-e nacional.
     */
    public static byte[] gerarPdfDeXml(final String xmlNfse) throws Exception {
        if (xmlNfse == null || xmlNfse.isBlank()) {
            throw new IllegalArgumentException("XML da NFS-e e obrigatorio para gerar DANFSe");
        }
        return gerarPdf(DANFSeXmlMapper.fromXml(xmlNfse));
    }

    /**
     * Gera o PDF a partir dos parameters ja montados (uso avancado / testes).
     */
    public static byte[] gerarPdf(final Map<String, Object> parameters) throws Exception {
        if (parameters == null || parameters.isEmpty()) {
            throw new IllegalArgumentException("Parameters do DANFSe sao obrigatorios");
        }
        final JasperReport report = compiledReport();
        final JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource(1));
        final byte[] pdf = JasperExportManager.exportReportToPdf(print);
        if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P') {
            throw new IllegalStateException("Falha ao exportar DANFSe: PDF invalido");
        }
        return pdf;
    }

    private static JasperReport compiledReport() throws Exception {
        JasperReport cached = compiled;
        if (cached != null) {
            return cached;
        }
        synchronized (DANFSeJasper.class) {
            if (compiled == null) {
                try (InputStream in = DANFSeJasper.class.getResourceAsStream(JRXML)) {
                    if (in == null) {
                        throw new IllegalStateException("Template nao encontrado no classpath: " + JRXML);
                    }
                    compiled = JasperCompileManager.compileReport(in);
                    LOGGER.info("Template DANFSe Jasper compilado: {}", JRXML);
                }
            }
            return compiled;
        }
    }
}
