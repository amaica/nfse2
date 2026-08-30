package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import br.com.synki.nfse.portal.service.*;
import br.com.synki.nfse.portal.web.dto.RegistrarContaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmbedTokenService tokenService;
    private final AuthEmpresaService authEmpresaService;
    private final MembershipService membershipService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioContaService usuarioContaService;
    private final OnboardingService onboardingService;

    public AuthController(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            EmbedTokenService tokenService,
            AuthEmpresaService authEmpresaService,
            MembershipService membershipService,
            RefreshTokenService refreshTokenService,
            UsuarioContaService usuarioContaService,
            OnboardingService onboardingService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authEmpresaService = authEmpresaService;
        this.membershipService = membershipService;
        this.refreshTokenService = refreshTokenService;
        this.usuarioContaService = usuarioContaService;
        this.onboardingService = onboardingService;
    }

    public record LoginRequest(String email, String cnpj, @NotBlank String senha) {}

    public record TrocarEmpresaRequest(@NotNull Long empresaId) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AceitarConviteRequest(@NotBlank String token, String nome, @NotBlank String senha) {}

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegistrarContaRequest req) {
        return onboardingService.registrar(req);
    }

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

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody RefreshRequest req) {
        var usuarioId = refreshTokenService.validarERevogar(req.refreshToken());
        var user = usuarioRepository.findById(usuarioId)
                .filter(Usuario -> Usuario.isAtivo())
                .orElseThrow(() -> new IllegalArgumentException("Usuario inativo ou nao encontrado"));
        return tokenResponse(user);
    }

    @GetMapping("/convite")
    public Map<String, Object> consultarConvite(@RequestParam String token) {
        return usuarioContaService.consultarConvite(token);
    }

    @PostMapping("/convite/aceitar")
    public Map<String, Object> aceitarConvite(@RequestBody AceitarConviteRequest req) {
        return usuarioContaService.aceitarConvite(req.token(), req.nome(), req.senha());
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
        if (membershipService.precisaOnboarding(user.getId())) {
            return onboardingService.sessaoOnboarding(user);
        }
        var empresaId = membershipService.resolverEmpresaInicial(user.getId(), user.getEmpresaId());
        if (empresaId == null || !membershipService.hasAccess(user.getId(), empresaId)) {
            throw new IllegalArgumentException("Usuario sem empresa vinculada — conclua o onboarding");
        }
        var contaId = membershipService.contaIdDoUsuario(user.getId(), empresaId);
        var papel = membershipService.papelAtivo(user.getId(), empresaId);
        if (!empresaId.equals(user.getEmpresaId())) {
            user.setEmpresaId(empresaId);
            usuarioRepository.save(user);
        }
        return AuthSessionBuilder.of(user, empresaId, tokenService, refreshTokenService, empresaRepository)
                .papel(papel)
                .contaId(contaId)
                .build();
    }

    @GetMapping("/embed/validate")
    public Map<String, Object> validateEmbed(@RequestParam String t) {
        var session = tokenService.validate(t);
        return Map.of("empresaId", session.empresaId(), "usuarioId", session.usuarioId(), "valido", true);
    }

    @GetMapping("/me")
    public Map<String, Object> sessaoAtual(@AuthenticationPrincipal EmbedSession session) {
        var empresa = empresaRepository.findById(session.empresaId()).orElseThrow();
        var user = usuarioRepository.findById(session.usuarioId()).orElseThrow();
        var body = new LinkedHashMap<String, Object>();
        body.put("empresaId", session.empresaId());
        body.put("empresaNome", empresa.getNome());
        body.put("empresaCnpj", empresa.getCnpj());
        body.put("usuarioId", session.usuarioId());
        body.put("nome", user.getNome());
        body.put("email", user.getEmail());
        body.put("papel", membershipService.papelAtivo(session.usuarioId(), session.empresaId()));
        body.put("contaId", membershipService.contaIdDoUsuario(session.usuarioId(), session.empresaId()));
        body.put("empresas", membershipService.listarTodasEmpresasPermitidas(session.usuarioId()).stream()
                .map(e -> Map.of("id", e.getId(), "nome", e.getNome(), "cnpj", e.getCnpj()))
                .toList());
        return body;
    }

    @GetMapping("/empresas")
    public Map<String, Object> listarEmpresas(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "40") int limite) {
        return Map.of(
                "empresaAtualId", session.empresaId(),
                "itens", authEmpresaService.listarEmpresas(session.usuarioId(), session.empresaId(), q, limite));
    }

    @PostMapping("/trocar-empresa")
    public Map<String, Object> trocarEmpresa(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody TrocarEmpresaRequest body) {
        if (body.empresaId().equals(session.empresaId())) {
            return authEmpresaService.trocarEmpresa(session.usuarioId(), body.empresaId());
        }
        return authEmpresaService.trocarEmpresa(session.usuarioId(), body.empresaId());
    }
}
