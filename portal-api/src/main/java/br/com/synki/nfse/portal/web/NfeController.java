package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.AssinaturaService;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.ContabilidadeService;
import br.com.synki.nfse.portal.service.EmissaoNfeService;
import br.com.synki.nfse.portal.service.NfeContextoService;
import br.com.synki.nfse.portal.service.NfeDanfeService;
import br.com.synki.nfse.portal.service.NfeDistribuicaoDFeService;
import br.com.synki.nfse.portal.service.NfeEmailService;
import br.com.synki.nfse.portal.service.NfeEntradaService;
import br.com.synki.nfse.portal.service.NfeOperacoesService;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.web.dto.EnviarDanfeEmailRequest;
import br.com.synki.nfse.portal.web.dto.nfe.*;
import com.fincatto.documentofiscal.DFModelo;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nfe")
public class NfeController {

    private final NfeContextoService contextoService;
    private final EmissaoNfeService emissaoService;
    private final NfeOperacoesService operacoesService;
    private final NfeDanfeService danfeService;
    private final NfeEmailService nfeEmailService;
    private final AuditLogService auditLogService;
    private final PortalAuthorization authz;
    private final AssinaturaService assinaturaService;
    private final ContabilidadeService contabilidadeService;
    private final NfeEmissaoRepository nfeEmissaoRepository;
    private final NfeDistribuicaoDFeService distribuicaoDFeService;
    private final NfeEntradaService nfeEntradaService;

    public NfeController(
            NfeContextoService contextoService,
            EmissaoNfeService emissaoService,
            NfeOperacoesService operacoesService,
            NfeDanfeService danfeService,
            NfeEmailService nfeEmailService,
            AuditLogService auditLogService,
            PortalAuthorization authz,
            AssinaturaService assinaturaService,
            ContabilidadeService contabilidadeService,
            NfeEmissaoRepository nfeEmissaoRepository,
            NfeDistribuicaoDFeService distribuicaoDFeService,
            NfeEntradaService nfeEntradaService) {
        this.contextoService = contextoService;
        this.emissaoService = emissaoService;
        this.operacoesService = operacoesService;
        this.danfeService = danfeService;
        this.nfeEmailService = nfeEmailService;
        this.auditLogService = auditLogService;
        this.authz = authz;
        this.assinaturaService = assinaturaService;
        this.contabilidadeService = contabilidadeService;
        this.nfeEmissaoRepository = nfeEmissaoRepository;
        this.distribuicaoDFeService = distribuicaoDFeService;
        this.nfeEntradaService = nfeEntradaService;
    }

    @GetMapping("/notas")
    public Object listarNotas(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) String serie,
            @RequestParam(required = false) String status) {
        var numeroFiltro = numero != null ? numero : EmissaoNfeService.parseNumeroFiltro(q);
        return emissaoService.listarNotasFiltradas(
                session.empresaId(),
                DFModelo.NFE,
                page,
                size,
                parseData(de),
                parseData(ate),
                q,
                numeroFiltro,
                serie,
                status);
    }

    @GetMapping("/notas-entrada")
    public Object listarEntradasAutocomplete(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) String q) {
        return emissaoService.listarEntradas(session.empresaId(), q);
    }

    @GetMapping("/entradas")
    public Object listarEntradasDfe(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate,
            @RequestParam(required = false) String q) {
        authz.requireOperador(session);
        return nfeEntradaService.listar(session.empresaId(), parseData(de), parseData(ate), q, page, size);
    }

    @GetMapping("/entradas/export.zip")
    public ResponseEntity<byte[]> exportarEntradasZip(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Long> ids) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EXPORT_XML_NFE_ENTRADA",
                ids != null ? ids.size() + " ids" : "filtro");
        byte[] zip = nfeEntradaService.exportarZip(session.empresaId(), parseData(de), parseData(ate), q, ids);
        String filename = "nfe-entrada-" + session.empresaId() + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    @GetMapping(value = "/entradas/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> xmlEntrada(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id) {
        authz.requireOperador(session);
        String xml = nfeEntradaService.xmlPorId(session.empresaId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nfe-entrada-" + id + ".xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/notas/export.zip")
    public ResponseEntity<byte[]> exportarXmlZip(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) String serie,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> chaves) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "EXPORT_XML_NFE",
                "ZIP " + (chaves != null ? chaves.size() + " chaves" : "filtro"));
        var numeroFiltro = numero != null ? numero : EmissaoNfeService.parseNumeroFiltro(q);
        byte[] zip = emissaoService.exportarXmlZip(
                session.empresaId(),
                DFModelo.NFE,
                parseData(de),
                parseData(ate),
                q,
                numeroFiltro,
                serie,
                status,
                chaves);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nfe-xmls.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    private static java.time.LocalDate parseData(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        return java.time.LocalDate.parse(iso.trim());
    }

    @GetMapping("/emissao/contexto")
    public Object contexto(@AuthenticationPrincipal EmbedSession session) throws Exception {
        return contextoService.contexto(session.empresaId(), DFModelo.NFE);
    }

    @GetMapping("/status-servico")
    public Object statusServico(@AuthenticationPrincipal EmbedSession session) throws Exception {
        return operacoesService.statusServico(session.empresaId(), DFModelo.NFE);
    }

    @PostMapping("/lotes/enviar")
    public Object enviarLote(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody(required = false) NfeEmitirLoteRequest body) throws Exception {
        authz.requireOperador(session);
        assinaturaService.requireEmissaoNfe(session.empresaId());
        var resultado = emissaoService.enviarLote(session.empresaId(), DFModelo.NFE, body);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EMISSAO_NFE",
                "NF-e " + resultado.getOrDefault("chaveAcesso", ""));
        assinaturaService.registrarNfeEmitida(session.empresaId());
        var chave = resultado.get("chaveAcesso");
        if (chave != null && !chave.toString().isBlank()) {
            contabilidadeService.enviarNfeAposEmissao(session.empresaId(), chave.toString());
        }
        return resultado;
    }

    @GetMapping("/lotes/{recibo}")
    public Object consultarLote(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String recibo) throws Exception {
        return operacoesService.consultarLote(session.empresaId(), recibo, DFModelo.NFE);
    }

    @GetMapping("/notas/consultar/{chave}")
    public Object consultarNota(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "CONSULTA_NFE", chave);
        return operacoesService.consultarNota(session.empresaId(), chave, DFModelo.NFE);
    }

    @GetMapping("/notas/{chave}/danfe")
    public ResponseEntity<byte[]> danfe(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "DANFE_NFE", chave);
        byte[] pdf = danfeService.gerarPdf(session.empresaId(), chave.replace("NFe", ""));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"danfe-" + chave + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/notas/{chave}/danfe/email")
    public Object enviarDanfePorEmail(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave,
            @Valid @RequestBody EnviarDanfeEmailRequest body) throws Exception {
        authz.requireOperador(session);
        nfeEmailService.enviarDanfe(session.empresaId(), chave, body.destinatario(), body.mensagem());
        auditLogService.log(session.empresaId(), session.usuarioId(), "EMAIL_DANFE_NFE", chave);
        return Map.of("ok", true, "destinatario", body.destinatario().trim().toLowerCase());
    }

    @GetMapping("/notas/{chave}/xml")
    public ResponseEntity<String> xml(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave) {
        var chaveNorm = chave.replace("NFe", "");
        var xml = nfeEmissaoRepository
                .findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(session.empresaId(), chaveNorm)
                .map(e -> e.getXmlProc())
                .filter(s -> s != null && !s.isBlank())
                .orElseThrow(() -> new IllegalStateException("XML da NF-e nao encontrado para a chave informada"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + chaveNorm + "-proc.xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @PostMapping("/notas/cancelar")
    public Object cancelar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeCancelarRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "CANCELAMENTO_NFE", body.chave());
        return operacoesService.cancelar(session.empresaId(), DFModelo.NFE, body);
    }

    @PostMapping("/notas/inutilizar")
    public Object inutilizar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeInutilizarRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "INUTILIZACAO_NFE",
                body.serie() + " " + body.numeroInicial() + "-" + body.numeroFinal());
        return operacoesService.inutilizar(session.empresaId(), DFModelo.NFE, body);
    }

    @PostMapping("/notas/carta-correcao")
    public Object cartaCorrecao(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeCartaCorrecaoRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "CCE_NFE", body.chave());
        return operacoesService.cartaCorrecao(session.empresaId(), DFModelo.NFE, body);
    }

    @PostMapping("/contingencia/epec")
    public Object contingenciaEpec(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody(required = false) NfeContingenciaEpecRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EPEC_NFE", "contingencia");
        return operacoesService.enviarEpec(session.empresaId(), DFModelo.NFE, body != null ? body : new NfeContingenciaEpecRequest(null, null, null, null, null));
    }

    @PostMapping("/distribuicao/baixar")
    public Object baixarXmlsDestinatario(@AuthenticationPrincipal EmbedSession session) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "BAIXAR_XML_DFE", "distribuicao destinatario");
        return distribuicaoDFeService.baixarEmpresa(session.empresaId());
    }
}
