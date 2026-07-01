package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.CertificadoLeituraService;
import br.com.synki.nfse.portal.service.CertificadoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/empresas/{empresaId}/certificado")
public class CertificadoAdminController {

    private final CertificadoService certificadoService;
    private final CertificadoLeituraService certificadoLeituraService;

    public CertificadoAdminController(
            CertificadoService certificadoService,
            CertificadoLeituraService certificadoLeituraService) {
        this.certificadoService = certificadoService;
        this.certificadoLeituraService = certificadoLeituraService;
    }

    @GetMapping
    public Object status(@PathVariable Long empresaId) {
        var body = new LinkedHashMap<String, Object>();
        body.put("cadastrado", certificadoService.possuiCertificado(empresaId));
        certificadoLeituraService.lerMetadados(empresaId).ifPresent(meta -> {
            body.put("documento", meta.documento());
            body.put("titular", meta.titular());
            body.put("pessoaFisica", meta.pessoaFisica());
            body.put("podeEmitir", !meta.pessoaFisica());
        });
        return body;
    }

    @PostMapping(consumes = "multipart/form-data")
    public Object upload(
            @PathVariable Long empresaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("senha") String senha) throws Exception {
        return certificadoService.salvar(empresaId, arquivo, senha);
    }
}
