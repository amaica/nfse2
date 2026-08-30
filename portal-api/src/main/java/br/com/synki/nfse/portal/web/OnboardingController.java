package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.OnboardingService;
import br.com.synki.nfse.portal.web.dto.OnboardingEmpresaRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal EmbedSession session) {
        return onboardingService.status(session.usuarioId());
    }

    @PostMapping("/empresa")
    public Map<String, Object> criarEmpresa(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody OnboardingEmpresaRequest req) {
        return onboardingService.criarPrimeiraEmpresa(session.usuarioId(), req);
    }
}
