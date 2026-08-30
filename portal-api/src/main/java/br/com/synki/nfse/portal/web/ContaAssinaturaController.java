package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.AssinaturaService;
import br.com.synki.nfse.portal.service.MembershipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conta")
public class ContaAssinaturaController {

    private final AssinaturaService assinaturaService;
    private final MembershipService membershipService;

    public ContaAssinaturaController(AssinaturaService assinaturaService, MembershipService membershipService) {
        this.assinaturaService = assinaturaService;
        this.membershipService = membershipService;
    }

    public record CheckoutRequest(int pacotes) {}

    @GetMapping("/assinatura")
    public Map<String, Object> status(@AuthenticationPrincipal EmbedSession session) {
        var contaId = membershipService.contaIdDaEmpresa(session.empresaId());
        return assinaturaService.statusConta(contaId, session.usuarioId());
    }

    @PostMapping("/billing/checkout")
    public Map<String, String> checkout(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody CheckoutRequest body) throws Exception {
        var url = assinaturaService.criarCheckout(
                session.usuarioId(), session.empresaId(), body != null ? body.pacotes() : 1);
        return Map.of("url", url);
    }

    @PostMapping("/billing/portal")
    public Map<String, String> portal(@AuthenticationPrincipal EmbedSession session) throws Exception {
        var url = assinaturaService.abrirPortal(session.usuarioId(), session.empresaId());
        return Map.of("url", url);
    }
}
