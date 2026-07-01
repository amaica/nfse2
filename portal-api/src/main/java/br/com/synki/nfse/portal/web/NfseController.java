package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.EmissaoContextoService;
import br.com.synki.nfse.portal.service.EmissaoDpsService;
import br.com.synki.nfse.portal.service.NfseEmailService;
import br.com.synki.nfse.portal.service.NfseLibService;
import br.com.synki.nfse.portal.service.CnaeService;
import br.com.synki.nfse.portal.service.NbsService;
import br.com.synki.nfse.portal.service.ServicosLc116Service;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import br.com.synki.nfse.portal.web.dto.EmissaoCompletaRequest;
import br.com.synki.nfse.portal.web.dto.EnviarDanfeEmailRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nfse")
public class NfseController {

    private final NfseLibService nfseLibService;
    private final EmissaoDpsService emissaoDpsService;
    private final EmissaoContextoService emissaoContextoService;
    private final AuditLogService auditLogService;
    private final NfseLogRepository logRepository;
    private final ServicosLc116Service servicosLc116Service;
    private final NbsService nbsService;
    private final CnaeService cnaeService;
    private final NfseEmailService nfseEmailService;

    public NfseController(
            NfseLibService nfseLibService,
            EmissaoDpsService emissaoDpsService,
            EmissaoContextoService emissaoContextoService,
            AuditLogService auditLogService,
            NfseLogRepository logRepository,
            ServicosLc116Service servicosLc116Service,
            NbsService nbsService,
            CnaeService cnaeService,
            NfseEmailService nfseEmailService) {
        this.nfseLibService = nfseLibService;
        this.emissaoDpsService = emissaoDpsService;
        this.emissaoContextoService = emissaoContextoService;
        this.auditLogService = auditLogService;
        this.logRepository = logRepository;
        this.servicosLc116Service = servicosLc116Service;
        this.nbsService = nbsService;
        this.cnaeService = cnaeService;
        this.nfseEmailService = nfseEmailService;
    }

    @GetMapping("/emissao/contexto")
    public Object emissaoContexto(@AuthenticationPrincipal EmbedSession session) throws Exception {
        return emissaoContextoService.contexto(session.empresaId());
    }

    @GetMapping("/convenio/{codigoMunicipio}")
    public Object convenio(@AuthenticationPrincipal EmbedSession session, @PathVariable String codigoMunicipio) throws Exception {
        return nfseLibService.consultaConvenio(session.empresaId(), codigoMunicipio);
    }

    @GetMapping("/servicos")
    public Object servicos(
            @RequestParam(required = false) String termo,
            @RequestParam(defaultValue = "400") int limite,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String cnaes) {
        var listaCnaes = cnaes == null || cnaes.isBlank()
                ? List.<String>of()
                : List.of(cnaes.split(","));
        var itens = servicosLc116Service.buscar(termo, limite, grupo, listaCnaes);
        return Map.of(
                "total", servicosLc116Service.total(),
                "totalAgro", servicosLc116Service.totalAgro(),
                "totalMecanico", servicosLc116Service.totalMecanico(),
                "exibidos", itens.size(),
                "grupo", grupo != null ? grupo : "todos",
                "filtradoPorCnae", !listaCnaes.isEmpty() && (termo == null || termo.isBlank()),
                "itens", itens);
    }

    @GetMapping("/cnae")
    public Object cnae(
            @RequestParam(required = false) String termo,
            @RequestParam(defaultValue = "40") int limite) {
        var itens = cnaeService.buscar(termo, limite);
        return Map.of("total", cnaeService.total(), "exibidos", itens.size(), "itens", itens);
    }

    @GetMapping("/nbs")
    public Object nbs(
            @RequestParam(required = false) String termo,
            @RequestParam(defaultValue = "40") int limite,
            @RequestParam(required = false) String lc116) {
        var itens = nbsService.buscar(termo, limite, lc116);
        return Map.of(
                "total", nbsService.total(),
                "exibidos", itens.size(),
                "itens", itens);
    }

    @GetMapping("/aliquota")
    public Object aliquota(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam String codigoMunicipio,
            @RequestParam String codigoServico) throws Exception {
        return Map.of("aliquota", nfseLibService.consultaAliquota(session.empresaId(), codigoMunicipio, codigoServico));
    }

    @GetMapping("/consulta/{chave}")
    public Object consulta(@AuthenticationPrincipal EmbedSession session, @PathVariable String chave) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "CONSULTA", "Chave " + chave);
        return nfseLibService.buscarPorChave(session.empresaId(), chave);
    }

    @GetMapping("/pdf/{chave}")
    public ResponseEntity<byte[]> pdf(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave,
            @RequestParam(defaultValue = "false") boolean inline) throws Exception {
        byte[] pdf = nfseLibService.downloadPdf(session.empresaId(), chave);
        var disposition = inline ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=nfse.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/pdf/{chave}/email")
    public Object enviarPdfPorEmail(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave,
            @Valid @RequestBody EnviarDanfeEmailRequest body) throws Exception {
        var resultado = nfseEmailService.enviarDanfe(session.empresaId(), chave, body.destinatario(), body.mensagem());
        auditLogService.log(session.empresaId(), session.usuarioId(), "EMAIL_DANFE",
                "DANFSe " + chave + " -> " + body.destinatario()
                        + (resultado.anexoXml() ? " (XML; PDF agendado)" : ""));
        return Map.of(
                "ok", true,
                "destinatario", body.destinatario(),
                "anexoXml", resultado.anexoXml(),
                "pdfRetryAgendado", resultado.pdfRetryAgendado());
    }

    @GetMapping("/xml/{chave}")
    public ResponseEntity<String> xml(@AuthenticationPrincipal EmbedSession session, @PathVariable String chave) throws Exception {
        return ResponseEntity.ok(nfseLibService.downloadXml(session.empresaId(), chave));
    }

    @GetMapping("/historico")
    public Object historico(@AuthenticationPrincipal EmbedSession session) {
        return logRepository.findTop50ByEmpresaIdOrderByCreatedAtDesc(session.empresaId());
    }

    @GetMapping("/config")
    public Object config(@AuthenticationPrincipal EmbedSession session) {
        var cfg = nfseLibService.configOrThrow(session.empresaId());
        return Map.of(
                "prefeitura", cfg.getPrefeitura(),
                "codigoMunicipioIbge", cfg.getCodigoMunicipioIbge(),
                "ambiente", cfg.getAmbiente(),
                "certificadoCadastrado", nfseLibService.temCertificado(session.empresaId()));
    }

    @PostMapping("/emitir")
    public Object emitir(@AuthenticationPrincipal EmbedSession session, @Valid @RequestBody EmissaoCompletaRequest body) throws Exception {
        var sucesso = emissaoDpsService.emitir(session.empresaId(), body);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EMISSAO", "NFSe " + sucesso.getChaveAcesso());
        return Map.of(
                "sucesso", true,
                "chaveAcesso", sucesso.getChaveAcesso(),
                "idDps", sucesso.getIdDps() != null ? sucesso.getIdDps() : "",
                "processadoEm", sucesso.getDataHoraProcessamento() != null ? sucesso.getDataHoraProcessamento().toString() : "");
    }
}
