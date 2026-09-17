package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.PainelHomeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelHomeService painelHomeService;
    private final PortalAuthorization authz;

    public PainelController(PainelHomeService painelHomeService, PortalAuthorization authz) {
        this.painelHomeService = painelHomeService;
        this.authz = authz;
    }

    @GetMapping("/home")
    public Map<String, Object> home(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) Integer ano) {
        authz.requireOperador(session);
        return painelHomeService.home(session.empresaId(), ano);
    }
}
