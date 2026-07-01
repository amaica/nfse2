package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.EmissaoNfeService;
import br.com.synki.nfse.portal.service.NfeContextoService;
import br.com.synki.nfse.portal.service.NfeDanfeService;
import br.com.synki.nfse.portal.service.NfeOperacoesService;
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
    private final AuditLogService auditLogService;

    public NfeController(
            NfeContextoService contextoService,
            EmissaoNfeService emissaoService,
            NfeOperacoesService operacoesService,
            NfeDanfeService danfeService,
            AuditLogService auditLogService) {
        this.contextoService = contextoService;
        this.emissaoService = emissaoService;
        this.operacoesService = operacoesService;
        this.danfeService = danfeService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/notas")
    public Object listarNotas(@AuthenticationPrincipal EmbedSession session) {
        return Map.of("itens", emissaoService.listarNotas(session.empresaId()));
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
        var resultado = emissaoService.enviarLote(session.empresaId(), DFModelo.NFE, body);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EMISSAO_NFE",
                "NF-e " + resultado.getOrDefault("chaveAcesso", ""));
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

    @PostMapping("/notas/cancelar")
    public Object cancelar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeCancelarRequest body) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "CANCELAMENTO_NFE", body.chave());
        return operacoesService.cancelar(session.empresaId(), DFModelo.NFE, body);
    }

    @PostMapping("/notas/inutilizar")
    public Object inutilizar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeInutilizarRequest body) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "INUTILIZACAO_NFE",
                body.serie() + " " + body.numeroInicial() + "-" + body.numeroFinal());
        return operacoesService.inutilizar(session.empresaId(), DFModelo.NFE, body);
    }

    @PostMapping("/notas/carta-correcao")
    public Object cartaCorrecao(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeCartaCorrecaoRequest body) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "CCE_NFE", body.chave());
        return operacoesService.cartaCorrecao(session.empresaId(), DFModelo.NFE, body);
    }

    @PostMapping("/contingencia/epec")
    public Object contingenciaEpec(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody(required = false) NfeContingenciaEpecRequest body) throws Exception {
        auditLogService.log(session.empresaId(), session.usuarioId(), "EPEC_NFE", "contingencia");
        return operacoesService.enviarEpec(session.empresaId(), DFModelo.NFE, body != null ? body : new NfeContingenciaEpecRequest(null, null, null, null, null));
    }
}
