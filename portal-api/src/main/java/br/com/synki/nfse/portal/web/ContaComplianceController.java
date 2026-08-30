package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.ContaMetricasService;
import br.com.synki.nfse.portal.service.LgpdExportService;
import br.com.synki.nfse.portal.service.MembershipService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/conta")
public class ContaComplianceController {

    private final MembershipService membershipService;
    private final PortalAuthorization authz;
    private final ContaMetricasService metricasService;
    private final AuditLogService auditLogService;
    private final LgpdExportService lgpdExportService;
    private final ObjectMapper objectMapper;

    public ContaComplianceController(
            MembershipService membershipService,
            PortalAuthorization authz,
            ContaMetricasService metricasService,
            AuditLogService auditLogService,
            LgpdExportService lgpdExportService,
            ObjectMapper objectMapper) {
        this.membershipService = membershipService;
        this.authz = authz;
        this.metricasService = metricasService;
        this.auditLogService = auditLogService;
        this.lgpdExportService = lgpdExportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/metricas")
    public Map<String, Object> metricas(@AuthenticationPrincipal EmbedSession session) {
        authz.requireGestao(session);
        var contaId = membershipService.contaIdDaEmpresa(session.empresaId());
        return metricasService.painel(contaId);
    }

    @GetMapping("/auditoria")
    public Map<String, Object> auditoria(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int limite) {
        authz.requireGestao(session);
        var contaId = membershipService.contaIdDaEmpresa(session.empresaId());
        return auditLogService.listarConta(contaId, pagina, limite);
    }

    @GetMapping("/lgpd/export")
    public ResponseEntity<byte[]> exportarLgpd(@AuthenticationPrincipal EmbedSession session) throws Exception {
        authz.requireGestao(session);
        var contaId = membershipService.contaIdDaEmpresa(session.empresaId());
        var dados = lgpdExportService.exportarConta(contaId);
        auditLogService.logConta(
                contaId,
                session.empresaId(),
                session.usuarioId(),
                "LGPD_EXPORT",
                "conta",
                "Exportacao solicitada pelo usuario " + session.usuarioId());

        var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dados);
        var bytes = json.getBytes(StandardCharsets.UTF_8);
        var filename = "syncnota-lgpd-conta-" + contaId + "-" + Instant.now().getEpochSecond() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }
}
