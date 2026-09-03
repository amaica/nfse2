package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.CertificadoLeituraService;
import br.com.synki.nfse.portal.service.CertificadoService;
import br.com.synki.nfse.portal.service.MembershipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/empresas/{empresaId}/certificado")
public class CertificadoPortalController {

    private final CertificadoService certificadoService;
    private final CertificadoLeituraService certificadoLeituraService;
    private final MembershipService membershipService;

    public CertificadoPortalController(
            CertificadoService certificadoService,
            CertificadoLeituraService certificadoLeituraService,
            MembershipService membershipService) {
        this.certificadoService = certificadoService;
        this.certificadoLeituraService = certificadoLeituraService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public Object status(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long empresaId) {
        membershipService.requireAccess(session.usuarioId(), empresaId);
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
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long empresaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("senha") String senha) throws Exception {
        membershipService.requireOperador(session.usuarioId(), empresaId);
        return certificadoService.salvar(empresaId, arquivo, senha);
    }
}
