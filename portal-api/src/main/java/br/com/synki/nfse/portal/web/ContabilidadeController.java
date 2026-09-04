package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.fiscal.livrocaixa.LivroCaixaService;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.ContabilidadeService;
import br.com.synki.nfse.portal.web.dto.SalvarContabilidadeRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/conta/contabilidade")
public class ContabilidadeController {

    private final ContabilidadeService contabilidadeService;
    private final LivroCaixaService livroCaixaService;
    private final PortalAuthorization authz;
    private final AuditLogService auditLogService;

    public ContabilidadeController(
            ContabilidadeService contabilidadeService,
            LivroCaixaService livroCaixaService,
            PortalAuthorization authz,
            AuditLogService auditLogService) {
        this.contabilidadeService = contabilidadeService;
        this.livroCaixaService = livroCaixaService;
        this.authz = authz;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/config")
    public Map<String, Object> config(@AuthenticationPrincipal EmbedSession session) {
        authz.requireGestao(session);
        return contabilidadeService.obterConfig(session.empresaId());
    }

    @PutMapping("/config")
    public Map<String, Object> salvarConfig(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody SalvarContabilidadeRequest body) {
        authz.requireGestao(session);
        var resultado = contabilidadeService.salvarConfig(
                session.empresaId(),
                body.emailContabilidade(),
                body.envioAutomatico(),
                body.enviarNfse(),
                body.enviarNfe(),
                body.enviarNfeEntrada() != null && body.enviarNfeEntrada());
        auditLogService.log(session.empresaId(), session.usuarioId(), "CONTABILIDADE_CONFIG",
                "Envio automatico=" + body.envioAutomatico() + " email=" + body.emailContabilidade());
        return resultado;
    }

    @GetMapping("/export.zip")
    public ResponseEntity<byte[]> exportarZip(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "true") boolean nfse,
            @RequestParam(defaultValue = "true") boolean nfe) throws Exception {
        authz.requireGestao(session);
        byte[] zip = contabilidadeService.exportarZip(session.empresaId(), de, ate, nfse, nfe);
        auditLogService.log(session.empresaId(), session.usuarioId(), "CONTABILIDADE_ZIP",
                "Periodo " + de + " a " + ate + " nfse=" + nfse + " nfe=" + nfe);
        var filename = "syncnota-xml-" + de + "_" + ate + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    @GetMapping("/livro-caixa")
    public Map<String, Object> livroCaixa(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "true") boolean nfse,
            @RequestParam(defaultValue = "true") boolean nfe) throws Exception {
        authz.requireGestao(session);
        return livroCaixaService.resumo(session.empresaId(), de, ate, nfse, nfe);
    }

    @GetMapping("/livro-caixa.csv")
    public ResponseEntity<byte[]> livroCaixaCsv(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "true") boolean nfse,
            @RequestParam(defaultValue = "true") boolean nfe) throws Exception {
        authz.requireGestao(session);
        byte[] csv = livroCaixaService.gerarCsv(session.empresaId(), de, ate, nfse, nfe);
        auditLogService.log(session.empresaId(), session.usuarioId(), "LIVRO_CAIXA_CSV",
                "Periodo " + de + " a " + ate);
        var filename = "syncnota-livro-caixa-" + de + "_" + ate + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/lcdpr.txt")
    public ResponseEntity<byte[]> lcdpr(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "true") boolean nfse,
            @RequestParam(defaultValue = "true") boolean nfe) throws Exception {
        authz.requireGestao(session);
        byte[] txt = livroCaixaService.gerarLcdpr(session.empresaId(), de, ate, nfse, nfe);
        auditLogService.log(session.empresaId(), session.usuarioId(), "LCDPR_GERADO",
                "Periodo " + de + " a " + ate);
        var filename = "LCDPR-" + de.getYear() + "-" + apenasDigitos(session) + ".txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(txt);
    }

    private static String apenasDigitos(EmbedSession session) {
        return String.valueOf(session.empresaId());
    }
}
