package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmbedTokenService tokenService;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EmbedTokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        var user = usuarioRepository.findByEmailAndAtivoTrue(req.email())
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenha()))
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
        String token = tokenService.createToken(user.getEmpresaId(), user.getId());
        return Map.of(
                "token", token,
                "empresaId", user.getEmpresaId(),
                "usuarioId", user.getId(),
                "nome", user.getNome());
    }

    @GetMapping("/embed/validate")
    public Map<String, Object> validateEmbed(@RequestParam String t) {
        var session = tokenService.validate(t);
        return Map.of("empresaId", session.empresaId(), "usuarioId", session.usuarioId(), "valido", true);
    }
}
