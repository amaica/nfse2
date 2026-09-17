package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.fiscal.livrocaixa.SimuladoIrService;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/conta/simulado-ir")
public class SimuladoIrController {

    private final SimuladoIrService simuladoIrService;
    private final PortalAuthorization authz;
    private final AuditLogService auditLogService;

    public SimuladoIrController(
            SimuladoIrService simuladoIrService,
            PortalAuthorization authz,
            AuditLogService auditLogService) {
        this.simuladoIrService = simuladoIrService;
        this.authz = authz;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Map<String, Object> simular(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "true") boolean nfse,
            @RequestParam(defaultValue = "true") boolean nfe) throws Exception {
        authz.requireOperador(session);
        var resultado = simuladoIrService.simular(session.empresaId(), de, ate, nfse, nfe);
        auditLogService.log(session.empresaId(), session.usuarioId(), "SIMULADO_IR",
                "Periodo " + de + " a " + ate + " nfse=" + nfse + " nfe=" + nfe);
        return resultado;
    }
}
