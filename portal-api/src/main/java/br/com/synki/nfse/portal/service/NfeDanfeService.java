package br.com.synki.nfse.portal.service;

import br.com.swconsultoria.impressao.model.Impressao;
import br.com.swconsultoria.impressao.service.ImpressaoService;
import br.com.swconsultoria.impressao.util.ImpressaoUtil;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NfeDanfeService {

    private static final Pattern CSTAT = Pattern.compile(
            "<(?:\\w+:)?cStat>\\s*(\\d{3})\\s*</(?:\\w+:)?cStat>",
            Pattern.CASE_INSENSITIVE);

    private final NfeEmissaoRepository emissaoRepository;
    private final EmpresaLogoService empresaLogoService;

    public NfeDanfeService(NfeEmissaoRepository emissaoRepository, EmpresaLogoService empresaLogoService) {
        this.emissaoRepository = emissaoRepository;
        this.empresaLogoService = empresaLogoService;
    }

    public byte[] gerarPdf(Long empresaId, String chave) throws Exception {
        var emissao = emissaoRepository.findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(empresaId, chave)
                .orElseThrow(() -> new IllegalArgumentException("NF-e nao encontrada para gerar DANFE"));
        String xml = emissao.getXmlProc();
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("NF-e sem XML autorizado armazenado — nao e possivel gerar o DANFE.");
        }

        String statusDb = emissao.getStatusProtocolo() != null ? emissao.getStatusProtocolo().trim() : "";
        String statusXml = extrairCStat(xml);
        String efetivo = !statusDb.isEmpty() ? statusDb : statusXml;

        if ("101".equals(efetivo) || "101".equals(statusXml)) {
            throw new IllegalStateException(
                    "DANFE indisponivel: NF-e cancelada (status 101). Baixe o XML se precisar do documento.");
        }
        if ("110".equals(efetivo) || "110".equals(statusXml)) {
            throw new IllegalStateException("DANFE indisponivel: NF-e denegada (status 110).");
        }

        // Autorizada no banco OU no XML (procNFe) — cobre status DB desatualizado com XML correto
        boolean autorizada = "100".equals(statusDb) || "100".equals(statusXml);
        if (!autorizada) {
            throw new IllegalStateException(
                    "DANFE so e gerado para NF-e autorizada (cStat 100). Status atual: "
                            + (efetivo.isEmpty() ? "desconhecido" : efetivo)
                            + ". Se a nota foi autorizada, consulte novamente na SEFAZ para atualizar o status.");
        }

        return gerarPdfDeXml(xml, empresaId);
    }

    public byte[] gerarPdfDeXml(String xmlProc) throws Exception {
        return gerarPdfDeXml(xmlProc, null);
    }

    public byte[] gerarPdfDeXml(String xmlProc, Long empresaId) throws Exception {
        Impressao impressao = ImpressaoUtil.impressaoPadraoNFe(xmlProc);
        if (empresaId != null) {
            empresaLogoService.abrir(empresaId).ifPresent(logo -> impressao.getParametros().put("Logo", logo));
        }
        return ImpressaoService.impressaoPdfByte(impressao);
    }

    /** Preferência pelo cStat do protocolo (protNFe), senão o primeiro cStat do XML. */
    static String extrairCStat(String xml) {
        if (xml == null || xml.isBlank()) return "";
        int prot = xml.toLowerCase().indexOf("protnfe");
        String escopo = prot >= 0 ? xml.substring(prot) : xml;
        Matcher m = CSTAT.matcher(escopo);
        if (m.find()) return m.group(1);
        m = CSTAT.matcher(xml);
        return m.find() ? m.group(1) : "";
    }
}
