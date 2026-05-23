package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EmissaoContextoService {

    private final EmpresaRepository empresaRepository;
    private final NfseLibService nfseLibService;
    private final CertificadoLeituraService certificadoLeituraService;

    public EmissaoContextoService(
            EmpresaRepository empresaRepository,
            NfseLibService nfseLibService,
            CertificadoLeituraService certificadoLeituraService) {
        this.empresaRepository = empresaRepository;
        this.nfseLibService = nfseLibService;
        this.certificadoLeituraService = certificadoLeituraService;
    }

    public Map<String, Object> contexto(Long empresaId) throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        var cfg = nfseLibService.configOrThrow(empresaId);
        var certificadoOk = nfseLibService.temCertificado(empresaId);
        var meta = certificadoLeituraService.lerMetadados(empresaId);

        String prestadorDocumento = meta.map(CertificadoLeituraService.Metadados::documento).orElse(empresa.getCnpj());
        boolean prestadorPf = meta.map(CertificadoLeituraService.Metadados::pessoaFisica).orElse(false);
        String prestadorNome = meta.map(CertificadoLeituraService.Metadados::titular).orElse(empresa.getNome());

        var body = new LinkedHashMap<String, Object>();
        body.put("empresaNome", empresa.getNome());
        body.put("prestadorNome", prestadorNome);
        body.put("prestadorDocumento", prestadorDocumento);
        body.put("prestadorPessoaFisica", prestadorPf);
        body.put("prefeitura", cfg.getPrefeitura());
        body.put("codigoMunicipioIbge", cfg.getCodigoMunicipioIbge());
        body.put("ambiente", cfg.getAmbiente());
        body.put("certificadoCadastrado", certificadoOk);
        body.put("podeEmitir", certificadoOk && !prestadorPf && prestadorDocumento != null && prestadorDocumento.length() == 14);
        body.put("codigoServicoPadrao", "01.07.01.000");
        body.put("descricaoServicoPadrao", "Analise e desenvolvimento de sistemas");
        if (certificadoOk) {
            try {
                var aliq = nfseLibService.consultaAliquota(empresaId, cfg.getCodigoMunicipioIbge(), "01.07.01.000");
                body.put("aliquotaPadraoPercentual", aliq);
            } catch (Exception ignored) {
                body.put("aliquotaPadraoPercentual", null);
            }
        }
        if (!certificadoOk) {
            body.put("aviso", "Cadastre o certificado A1 (.pfx) antes de emitir.");
        } else if (prestadorPf) {
            body.put("aviso", "Certificado de pessoa fisica: emissao como CNPJ exige certificado e-CNPJ A1.");
        }
        return body;
    }
}
