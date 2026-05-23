package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.CertificadoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/certificado")
public class CertificadoController {

    private final CertificadoService certificadoService;
    private final AuditLogService auditLogService;

    public CertificadoController(CertificadoService certificadoService, AuditLogService auditLogService) {
        this.certificadoService = certificadoService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal EmbedSession session) {
        return Map.of("cadastrado", certificadoService.possuiCertificado(session.empresaId()));
    }

    @PostMapping(consumes = "multipart/form-data")
    public Map<String, Object> upload(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("senha") String senha) throws Exception {
        var result = certificadoService.salvar(session.empresaId(), arquivo, senha);
        auditLogService.log(session.empresaId(), session.usuarioId(), "CERTIFICADO_UPLOAD", "PFX atualizado");
        return result;
    }
}
