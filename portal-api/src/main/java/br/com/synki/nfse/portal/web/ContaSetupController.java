package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.SetupProgressService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/conta")
public class ContaSetupController {

    private final SetupProgressService setupProgressService;

    public ContaSetupController(SetupProgressService setupProgressService) {
        this.setupProgressService = setupProgressService;
    }

    @GetMapping("/setup")
    public Map<String, Object> setup(@AuthenticationPrincipal EmbedSession session) {
        return setupProgressService.progresso(session.usuarioId(), session.empresaId());
    }
}
