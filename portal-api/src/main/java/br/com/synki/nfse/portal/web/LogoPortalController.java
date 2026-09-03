package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.EmpresaLogoService;
import br.com.synki.nfse.portal.service.MembershipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/empresas/{empresaId}/logo")
public class LogoPortalController {

    private final EmpresaLogoService empresaLogoService;
    private final MembershipService membershipService;

    public LogoPortalController(EmpresaLogoService empresaLogoService, MembershipService membershipService) {
        this.empresaLogoService = empresaLogoService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public Object status(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long empresaId) {
        membershipService.requireAccess(session.usuarioId(), empresaId);
        return Map.of("cadastrado", empresaLogoService.existe(empresaId));
    }

    @PostMapping(consumes = "multipart/form-data")
    public Object upload(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long empresaId,
            @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        membershipService.requireOperador(session.usuarioId(), empresaId);
        empresaLogoService.salvar(empresaId, arquivo);
        return Map.of("ok", true, "empresaId", empresaId);
    }

    @DeleteMapping
    public Object remover(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long empresaId) throws Exception {
        membershipService.requireGestao(session.usuarioId(), empresaId);
        empresaLogoService.excluir(empresaId);
        return Map.of("ok", true);
    }
}
