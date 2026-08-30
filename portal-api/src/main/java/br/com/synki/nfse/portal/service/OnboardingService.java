package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Assinatura;
import br.com.synki.nfse.portal.domain.Conta;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import br.com.synki.nfse.portal.web.dto.CriarEmpresaRequest;
import br.com.synki.nfse.portal.web.dto.OnboardingEmpresaRequest;
import br.com.synki.nfse.portal.web.dto.RegistrarContaRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OnboardingService {

    /** Token de sessão antes da primeira empresa (EmbedSession.empresaId = 0). */
    public static final long EMPRESA_ONBOARDING = 0L;

    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaCadastroService empresaCadastroService;
    private final MembershipService membershipService;
    private final PasswordEncoder passwordEncoder;
    private final EmbedTokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PortalMailService portalMailService;

    public OnboardingService(
            UsuarioRepository usuarioRepository,
            ContaRepository contaRepository,
            AssinaturaRepository assinaturaRepository,
            EmpresaRepository empresaRepository,
            EmpresaCadastroService empresaCadastroService,
            MembershipService membershipService,
            PasswordEncoder passwordEncoder,
            EmbedTokenService tokenService,
            RefreshTokenService refreshTokenService,
            PortalMailService portalMailService) {
        this.usuarioRepository = usuarioRepository;
        this.contaRepository = contaRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.empresaRepository = empresaRepository;
        this.empresaCadastroService = empresaCadastroService;
        this.membershipService = membershipService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.portalMailService = portalMailService;
    }

    @Transactional
    public Map<String, Object> registrar(RegistrarContaRequest req) {
        var email = req.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByEmailAndAtivoTrue(email).isPresent()) {
            throw new IllegalArgumentException("E-mail ja cadastrado — faca login ou recupere a senha");
        }

        var conta = Conta.criar(req.nomeConta().trim(), null);
        contaRepository.save(conta);

        var user = new Usuario();
        user.setNome(req.nome().trim());
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(req.senha()));
        user.setEmpresaId(null);
        user.setPerfil(UsuarioEmpresa.PAPEL_OWNER);
        usuarioRepository.save(user);

        conta.setOwnerUsuarioId(user.getId());
        contaRepository.save(conta);
        assinaturaRepository.save(Assinatura.trial(conta.getId()));

        portalMailService.enviarBoasVindas(user, conta.getNome());
        return respostaOnboarding(user, conta);
    }

    public Map<String, Object> sessaoOnboarding(Usuario user) {
        var conta = contaRepository.findByOwnerUsuarioId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Conta do usuario nao encontrada"));
        return respostaOnboarding(user, conta);
    }

    public Map<String, Object> status(Long usuarioId) {
        var user = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        var conta = contaRepository.findByOwnerUsuarioId(usuarioId).orElse(null);
        var body = new LinkedHashMap<String, Object>();
        body.put("onboardingRequired", membershipService.precisaOnboarding(usuarioId));
        body.put("nome", user.getNome());
        body.put("email", user.getEmail());
        if (conta != null) {
            body.put("contaId", conta.getId());
            body.put("contaNome", conta.getNome());
            assinaturaRepository.findByContaId(conta.getId()).ifPresent(a -> {
                body.put("trialFim", a.getPeriodoFim());
                body.put("assinaturaStatus", a.getStatus());
            });
        }
        body.put("empresas", membershipService.listarTodasEmpresasPermitidas(usuarioId).stream()
                .map(e -> Map.of("id", e.getId(), "nome", e.getNome(), "cnpj", e.getCnpj()))
                .toList());
        return body;
    }

    @Transactional
    public Map<String, Object> criarPrimeiraEmpresa(Long usuarioId, OnboardingEmpresaRequest req) {
        if (!membershipService.precisaOnboarding(usuarioId)) {
            throw new IllegalStateException("Onboarding ja concluido");
        }
        var conta = contaRepository.findByOwnerUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Conta do usuario nao encontrada"));
        var user = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        var cnpj = apenasDigitos(req.cnpj());
        var emailIntegracao = "nfse." + cnpj + "@integracao.local";
        var senhaIntegracao = gerarSenhaIntegracao();

        var criar = new CriarEmpresaRequest(
                cnpj,
                req.nome().trim(),
                req.nomeFantasia(),
                req.email(),
                req.telefone(),
                req.inscricaoEstadual(),
                req.inscricaoMunicipal(),
                req.cep(),
                req.logradouro(),
                req.numero(),
                req.complemento(),
                req.bairro(),
                req.municipio(),
                req.uf(),
                req.cnaePrincipal(),
                req.cnaePrincipalDescricao(),
                req.optanteSimples(),
                req.situacaoCadastral(),
                req.prefeitura().trim(),
                req.codigoMunicipioIbge().replaceAll("\\D", ""),
                req.ambiente() != null && !req.ambiente().isBlank() ? req.ambiente() : "homologacao",
                "1",
                0L,
                emailIntegracao,
                senhaIntegracao,
                "Integracao " + req.nome().trim(),
                null,
                null,
                null,
                null,
                false,
                null);

        var det = empresaCadastroService.criarPrimeiraEmpresaConta(criar, usuarioId, conta.getId());
        var empresaId = ((Number) det.get("id")).longValue();
        user.setEmpresaId(empresaId);
        usuarioRepository.save(user);

        var body = AuthSessionBuilder.of(
                        user,
                        empresaId,
                        tokenService,
                        refreshTokenService,
                        empresaRepository)
                .papel(UsuarioEmpresa.PAPEL_OWNER)
                .contaId(conta.getId())
                .build();
        body.put("onboardingRequired", false);
        body.put("emailIntegracao", emailIntegracao);
        body.put("senhaIntegracao", senhaIntegracao);
        portalMailService.enviarEmitentePronto(user, req.nome().trim());
        return body;
    }

    public boolean precisaOnboarding(Long usuarioId) {
        return membershipService.precisaOnboarding(usuarioId);
    }

    private Map<String, Object> respostaOnboarding(Usuario user, Conta conta) {
        var body = new LinkedHashMap<String, Object>();
        body.put("token", tokenService.createToken(EMPRESA_ONBOARDING, user.getId()));
        var refresh = refreshTokenService.emitir(user.getId());
        if (refresh != null) {
            body.put("refreshToken", refresh);
        }
        body.put("onboardingRequired", true);
        body.put("empresaId", EMPRESA_ONBOARDING);
        body.put("usuarioId", user.getId());
        body.put("nome", user.getNome());
        body.put("email", user.getEmail());
        body.put("papel", UsuarioEmpresa.PAPEL_OWNER);
        body.put("contaId", conta.getId());
        body.put("contaNome", conta.getNome());
        return body;
    }

    private static String gerarSenhaIntegracao() {
        var raw = UUID.randomUUID().toString().replace("-", "");
        return raw.substring(0, 12);
    }

    private static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
