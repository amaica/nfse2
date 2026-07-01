package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmbedTokenService tokenService;

    public AuthController(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            EmbedTokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public record LoginRequest(String email, String cnpj, @NotBlank String senha) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        if (req.senha() == null || req.senha().isBlank()) {
            throw new IllegalArgumentException("Senha obrigatoria");
        }
        var user = (req.cnpj() != null && !req.cnpj().isBlank())
                ? autenticarPorCnpj(req.cnpj(), req.senha())
                : autenticarPorEmail(req.email(), req.senha());
        return tokenResponse(user);
    }

    private br.com.synki.nfse.portal.domain.Usuario autenticarPorEmail(String email, String senha) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Informe email ou cnpj");
        }
        return usuarioRepository.findByEmailAndAtivoTrue(email.trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(senha, u.getSenha()))
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
    }

    private br.com.synki.nfse.portal.domain.Usuario autenticarPorCnpj(String cnpj, String senha) {
        var doc = cnpj.replaceAll("\\D", "");
        var empresa = empresaRepository.findByCnpjAndAtivoTrue(doc)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada ou inativa"));
        var user = usuarioRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(empresa.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario de integracao nao configurado"));
        if (!passwordEncoder.matches(senha, user.getSenha())) {
            throw new IllegalArgumentException("Credenciais invalidas");
        }
        return user;
    }

    private Map<String, Object> tokenResponse(br.com.synki.nfse.portal.domain.Usuario user) {
        var empresa = empresaRepository.findById(user.getEmpresaId()).orElseThrow();
        String token = tokenService.createToken(user.getEmpresaId(), user.getId());
        return Map.of(
                "token", token,
                "empresaId", user.getEmpresaId(),
                "empresaNome", empresa.getNome(),
                "empresaCnpj", empresa.getCnpj(),
                "usuarioId", user.getId(),
                "nome", user.getNome(),
                "email", user.getEmail());
    }

    @GetMapping("/embed/validate")
    public Map<String, Object> validateEmbed(@RequestParam String t) {
        var session = tokenService.validate(t);
        return Map.of("empresaId", session.empresaId(), "usuarioId", session.usuarioId(), "valido", true);
    }
}
