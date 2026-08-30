package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.AssinaturaService;
import br.com.synki.nfse.portal.service.AuditLogService;
import br.com.synki.nfse.portal.service.EmissaoNfeService;
import br.com.synki.nfse.portal.service.NfeContextoService;
import br.com.synki.nfse.portal.service.NfeOperacoesService;
import br.com.synki.nfse.portal.web.dto.nfe.*;
import com.fincatto.documentofiscal.DFModelo;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nfce")
public class NfceController {

    private final NfeContextoService contextoService;
    private final EmissaoNfeService emissaoService;
    private final NfeOperacoesService operacoesService;
    private final AuditLogService auditLogService;
    private final PortalAuthorization authz;
    private final AssinaturaService assinaturaService;

    public NfceController(
            NfeContextoService contextoService,
            EmissaoNfeService emissaoService,
            NfeOperacoesService operacoesService,
            AuditLogService auditLogService,
            PortalAuthorization authz,
            AssinaturaService assinaturaService) {
        this.contextoService = contextoService;
        this.emissaoService = emissaoService;
        this.operacoesService = operacoesService;
        this.auditLogService = auditLogService;
        this.authz = authz;
        this.assinaturaService = assinaturaService;
    }

    @GetMapping("/notas")
    public Object listarNotas(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return emissaoService.listarNotas(session.empresaId(), DFModelo.NFCE, page, size);
    }

    @GetMapping("/emissao/contexto")
    public Object contexto(@AuthenticationPrincipal EmbedSession session) throws Exception {
        return contextoService.contexto(session.empresaId(), DFModelo.NFCE);
    }

    @GetMapping("/status-servico")
    public Object statusServico(@AuthenticationPrincipal EmbedSession session) throws Exception {
        return operacoesService.statusServico(session.empresaId(), DFModelo.NFCE);
    }

    @PostMapping("/lotes/enviar")
    public Object enviarLote(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody(required = false) NfeEmitirLoteRequest body) throws Exception {
        authz.requireOperador(session);
        assinaturaService.requireEmissaoNfe(session.empresaId());
        var resultado = emissaoService.enviarLote(session.empresaId(), DFModelo.NFCE, body);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EMISSAO_NFCE",
                "NFC-e " + resultado.getOrDefault("chaveAcesso", ""));
        assinaturaService.registrarNfeEmitida(session.empresaId());
        return resultado;
    }

    @GetMapping("/lotes/{recibo}")
    public Object consultarLote(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String recibo) throws Exception {
        return operacoesService.consultarLote(session.empresaId(), recibo, DFModelo.NFCE);
    }

    @GetMapping("/notas/consultar/{chave}")
    public Object consultarNota(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable String chave) throws Exception {
        return operacoesService.consultarNota(session.empresaId(), chave, DFModelo.NFCE);
    }

    @PostMapping("/notas/cancelar")
    public Object cancelar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeCancelarRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "CANCELAMENTO_NFCE", body.chave());
        return operacoesService.cancelar(session.empresaId(), DFModelo.NFCE, body);
    }

    @PostMapping("/notas/inutilizar")
    public Object inutilizar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody NfeInutilizarRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "INUTILIZACAO_NFCE",
                body.serie() + " " + body.numeroInicial() + "-" + body.numeroFinal());
        return operacoesService.inutilizar(session.empresaId(), DFModelo.NFCE, body);
    }

    @PostMapping("/contingencia/epec")
    public Object contingenciaEpec(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody(required = false) NfeContingenciaEpecRequest body) throws Exception {
        authz.requireOperador(session);
        auditLogService.log(session.empresaId(), session.usuarioId(), "EPEC_NFCE", "contingencia");
        return operacoesService.enviarEpec(session.empresaId(), DFModelo.NFCE, body != null ? body : new NfeContingenciaEpecRequest(null, null, null, null, null));
    }
}
