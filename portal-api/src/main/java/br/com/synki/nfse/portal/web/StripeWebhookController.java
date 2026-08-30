package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.AssinaturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing/stripe")
public class StripeWebhookController {

    private final AssinaturaService assinaturaService;

    public StripeWebhookController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        assinaturaService.processarWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
