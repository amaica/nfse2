package br.com.synki.nfse.portal.service;

import br.com.swconsultoria.impressao.model.Impressao;
import br.com.swconsultoria.impressao.service.ImpressaoService;
import br.com.swconsultoria.impressao.util.ImpressaoUtil;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import org.springframework.stereotype.Service;

@Service
public class NfeDanfeService {

    private final NfeEmissaoRepository emissaoRepository;
    private final EmpresaLogoService empresaLogoService;

    public NfeDanfeService(NfeEmissaoRepository emissaoRepository, EmpresaLogoService empresaLogoService) {
        this.emissaoRepository = emissaoRepository;
        this.empresaLogoService = empresaLogoService;
    }

    public byte[] gerarPdf(Long empresaId, String chave) throws Exception {
        var emissao = emissaoRepository.findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(empresaId, chave)
                .orElseThrow(() -> new IllegalArgumentException("NF-e nao encontrada para gerar DANFE"));
        if (emissao.getXmlProc() == null || emissao.getXmlProc().isBlank()) {
            throw new IllegalStateException("NF-e sem XML autorizado armazenado");
        }
        if (!"100".equals(emissao.getStatusProtocolo())) {
            throw new IllegalStateException("NF-e nao autorizada (status " + emissao.getStatusProtocolo() + ")");
        }
        return gerarPdfDeXml(emissao.getXmlProc(), empresaId);
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
}
