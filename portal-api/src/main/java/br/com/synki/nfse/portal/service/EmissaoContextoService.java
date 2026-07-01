package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.service.fiscal.TributacaoService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmissaoContextoService {

    private final EmpresaRepository empresaRepository;
    private final NfseLibService nfseLibService;
    private final CertificadoLeituraService certificadoLeituraService;
    private final EmpresaCnaeService empresaCnaeService;
    private final ServicosLc116Service servicosLc116Service;
    private final TributacaoService tributacaoService;

    public EmissaoContextoService(
            EmpresaRepository empresaRepository,
            NfseLibService nfseLibService,
            CertificadoLeituraService certificadoLeituraService,
            EmpresaCnaeService empresaCnaeService,
            ServicosLc116Service servicosLc116Service,
            TributacaoService tributacaoService) {
        this.empresaRepository = empresaRepository;
        this.nfseLibService = nfseLibService;
        this.certificadoLeituraService = certificadoLeituraService;
        this.empresaCnaeService = empresaCnaeService;
        this.servicosLc116Service = servicosLc116Service;
        this.tributacaoService = tributacaoService;
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

        var codigoPadrao = "01.07.01.000";
        var descricaoPadrao = "Analise e desenvolvimento de sistemas";
        List<Map<String, Object>> cnaesEmpresa = List.of();
        List<ServicosLc116Service.ServicoLc116> servicosSugeridos = List.of();

        if (prestadorDocumento != null && prestadorDocumento.length() == 14) {
            var cnaes = empresaCnaeService.obterCnaes(prestadorDocumento);
            cnaesEmpresa = cnaes.stream()
                    .map(c -> Map.<String, Object>of(
                            "codigo", c.codigo(),
                            "descricao", c.descricao(),
                            "principal", c.principal()))
                    .toList();
            body.put("empresaCnaes", cnaesEmpresa);

            if (!cnaes.isEmpty()) {
                var codigos = cnaes.stream().map(EmpresaCnaeService.CnaeEmpresa::codigo).toList();
                servicosSugeridos = servicosLc116Service.sugerirPorCnaes(codigos, 25);
                body.put("servicosSugeridos", servicosSugeridos);
                if (!servicosSugeridos.isEmpty()) {
                    codigoPadrao = servicosSugeridos.getFirst().codigo();
                    descricaoPadrao = servicosSugeridos.getFirst().descricao();
                }
            }
        } else {
            body.put("empresaCnaes", cnaesEmpresa);
            body.put("servicosSugeridos", servicosSugeridos);
        }

        body.put("codigoServicoPadrao", codigoPadrao);
        body.put("descricaoServicoPadrao", descricaoPadrao);

        var operacoesNfse = tributacaoService.listarNfseServicos(empresaId, true).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "descricao", s.getDescricao(),
                        "itemListaServico", s.getItemListaServico(),
                        "nbs", s.getNbs() != null ? s.getNbs() : "",
                        "aliquotaIss", s.getAliquotaIss() != null ? s.getAliquotaIss() : 0,
                        "principal", s.isPrincipal()))
                .toList();
        body.put("operacoesNfse", operacoesNfse);

        if (certificadoOk) {
            try {
                var aliq = nfseLibService.consultaAliquota(empresaId, cfg.getCodigoMunicipioIbge(), codigoPadrao);
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
