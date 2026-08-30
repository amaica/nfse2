package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.AdminTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminTokenService tokenService;

    public AdminAuthController(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    public record LoginRequest(String secret) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body) {
        if (!tokenService.secretValido(body != null ? body.secret() : null)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Chave de administracao invalida"));
        }
        return ResponseEntity.ok(Map.of("token", tokenService.createToken()));
    }
}
