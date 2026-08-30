package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.domain.NfseOperacaoMensal;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.NfseOperacaoMensalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nfse/operacoes-mensais")
public class NfseOperacaoMensalController {

    private final NfseOperacaoMensalService service;
    private final PortalAuthorization authz;

    public NfseOperacaoMensalController(NfseOperacaoMensalService service, PortalAuthorization authz) {
        this.service = service;
        this.authz = authz;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@AuthenticationPrincipal EmbedSession session) {
        authz.requireOperador(session);
        return service.listar(session.empresaId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> obter(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        authz.requireOperador(session);
        return service.obter(session.empresaId(), id);
    }

    @PostMapping
    public Map<String, Object> salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody NfseOperacaoMensal body) {
        authz.requireGestao(session);
        return service.salvar(session.empresaId(), body);
    }

    public record EmitirRequest(String competencia) {}

    @PostMapping("/{id}/emitir")
    public Map<String, Object> emitir(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody(required = false) EmitirRequest body) throws Exception {
        authz.requireOperador(session);
        LocalDate comp = null;
        if (body != null && body.competencia() != null && !body.competencia().isBlank()) {
            comp = LocalDate.parse(body.competencia());
        }
        return service.emitir(session.empresaId(), session.usuarioId(), id, comp);
    }
}
