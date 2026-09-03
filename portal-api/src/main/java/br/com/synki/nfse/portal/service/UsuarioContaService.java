package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.UsuarioConvite;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioConviteRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioContaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioConviteRepository conviteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final EmpresaRepository empresaRepository;
    private final MembershipService membershipService;
    private final EmbedTokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioContaService(
            UsuarioConviteRepository conviteRepository,
            UsuarioRepository usuarioRepository,
            UsuarioEmpresaRepository usuarioEmpresaRepository,
            EmpresaRepository empresaRepository,
            MembershipService membershipService,
            EmbedTokenService tokenService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder) {
        this.conviteRepository = conviteRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.empresaRepository = empresaRepository;
        this.membershipService = membershipService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> listarMembros(Long gestorId, Long empresaSessaoId) {
        membershipService.requireGestao(gestorId, empresaSessaoId);
        var contaId = membershipService.contaIdDaEmpresa(empresaSessaoId);
        if (contaId == null) {
            return List.of();
        }

        var porUsuario = usuarioEmpresaRepository.findByContaIdAndAtivoTrueOrderByUsuarioIdAsc(contaId).stream()
                .collect(Collectors.groupingBy(UsuarioEmpresa::getUsuarioId));

        var resultado = new ArrayList<Map<String, Object>>();
        for (var entry : porUsuario.entrySet()) {
            var user = usuarioRepository.findById(entry.getKey()).orElse(null);
            if (user == null || !user.isAtivo()) {
                continue;
            }
            var todasMemberships = usuarioEmpresaRepository
                    .findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(entry.getKey());
            resultado.add(montarResumoMembro(user, todasMemberships));
        }
        resultado.sort(Comparator.comparing(m -> ((String) m.get("nome")).toLowerCase(Locale.ROOT)));
        return resultado;
    }

    @Transactional
    public Map<String, Object> criarConvite(
            Long gestorId,
            Long empresaSessaoId,
            String email,
            String papel,
            List<Long> empresaIds) {
        membershipService.requireGestao(gestorId, empresaSessaoId);
        var contaId = membershipService.contaIdDaEmpresa(empresaSessaoId);
        if (contaId == null) {
            throw new IllegalStateException("Conta nao encontrada");
        }

        var emailNorm = email.trim().toLowerCase(Locale.ROOT);
        if (emailNorm.isBlank()) {
            throw new IllegalArgumentException("E-mail obrigatorio");
        }

        var alvoEmpresas = resolverEmpresasConvite(gestorId, contaId, empresaSessaoId, empresaIds);
        if (conviteRepository.existsByContaIdAndEmailAndAceitoEmIsNull(contaId, emailNorm)) {
            throw new IllegalArgumentException("Ja existe convite pendente para este e-mail");
        }

        var convites = new ArrayList<Map<String, Object>>();
        for (Long empId : alvoEmpresas) {
            var token = gerarTokenConvite();
            var convite = UsuarioConvite.criar(
                    contaId,
                    empId,
                    emailNorm,
                    papel,
                    token,
                    gestorId,
                    Instant.now().plusSeconds(7 * 86400L));
            conviteRepository.save(convite);
            convites.add(Map.of(
                    "empresaId", empId,
                    "token", token,
                    "link", "/convite?token=" + token));
        }

        return Map.of(
                "email", emailNorm,
                "papel", papel != null ? papel : UsuarioEmpresa.PAPEL_OPERADOR,
                "convites", convites);
    }

    public List<Map<String, Object>> listarConvitesPendentes(Long gestorId, Long empresaSessaoId) {
        membershipService.requireGestao(gestorId, empresaSessaoId);
        var contaId = membershipService.contaIdDaEmpresa(empresaSessaoId);
        if (contaId == null) {
            return List.of();
        }
        return conviteRepository.findByContaIdAndAceitoEmIsNullOrderByCreatedAtDesc(contaId).stream()
                .map(this::resumoConvite)
                .toList();
    }

    public Map<String, Object> consultarConvite(String token) {
        var convite = conviteRepository.findByToken(token.trim())
                .filter(UsuarioConvite::isPendente)
                .orElseThrow(() -> new IllegalArgumentException("Convite invalido ou expirado"));
        var empresa = empresaRepository.findById(convite.getEmpresaId()).orElseThrow();
        return Map.of(
                "email", convite.getEmail(),
                "papel", convite.getPapel(),
                "empresaNome", empresa.getNome(),
                "empresaCnpj", empresa.getCnpj(),
                "expiraEm", convite.getExpiraEm().toString());
    }

    @Transactional
    public Map<String, Object> aceitarConvite(String token, String nome, String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha obrigatoria");
        }
        var convite = conviteRepository.findByToken(token.trim())
                .filter(UsuarioConvite::isPendente)
                .orElseThrow(() -> new IllegalArgumentException("Convite invalido ou expirado"));

        var user = usuarioRepository.findByEmail(convite.getEmail()).orElse(null);
        if (user == null) {
            var nomeFinal = nome != null && !nome.isBlank() ? nome.trim() : convite.getEmail();
            user = usuarioRepository.save(Usuario.create(
                    convite.getEmpresaId(),
                    nomeFinal,
                    convite.getEmail(),
                    passwordEncoder.encode(senha)));
        } else {
            if (!user.isAtivo()) {
                throw new IllegalArgumentException("Usuario inativo — contate o administrador");
            }
            if (!passwordEncoder.matches(senha, user.getSenha())) {
                throw new IllegalArgumentException("Senha incorreta para e-mail ja cadastrado");
            }
            if (!membershipService.usuarioNaConta(user.getId(), convite.getContaId())
                    && !membershipService.hasAccess(user.getId(), convite.getEmpresaId())) {
                var outraConta = membershipService.contaIdDoUsuario(user.getId());
                if (outraConta != null && !outraConta.equals(convite.getContaId())) {
                    throw new IllegalArgumentException("E-mail pertence a outra conta");
                }
            }
        }

        membershipService.vincularUsuarioEmpresa(
                user.getId(), convite.getEmpresaId(), convite.getContaId(), convite.getPapel());
        convite.marcarAceito();
        conviteRepository.save(convite);

        return AuthSessionBuilder.of(user, convite.getEmpresaId(), tokenService, refreshTokenService, empresaRepository)
                .papel(convite.getPapel())
                .contaId(convite.getContaId())
                .build();
    }

    @Transactional
    public Map<String, Object> vincularEmpresas(
            Long gestorId,
            Long empresaSessaoId,
            Long usuarioAlvoId,
            List<Long> empresaIds,
            String papel,
            Long portalPerfilId) {
        membershipService.requireGestao(gestorId, empresaSessaoId);
        var delegaveis = membershipService.listarEmpresasDelegaveisPorGestor(gestorId).stream()
                .map(e -> e.getId())
                .collect(Collectors.toSet());
        if (delegaveis.isEmpty()) {
            throw new IllegalArgumentException("Sem emitentes delegaveis");
        }

        var usuario = usuarioRepository.findById(usuarioAlvoId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Usuario inativo");
        }

        var alvoIds = resolverEmpresasAlvo(delegaveis, empresaSessaoId, empresaIds);
        var papelFinal = papel != null ? papel : UsuarioEmpresa.PAPEL_OPERADOR;
        Long perfilFinal = PAPEIS_GESTAO_LOCAL.contains(papelFinal) ? null : portalPerfilId;

        for (Long empId : alvoIds) {
            var contaId = membershipService.contaIdDaEmpresa(empId);
            if (contaId == null) {
                continue;
            }
            membershipService.vincularUsuarioEmpresa(usuarioAlvoId, empId, contaId, papelFinal, perfilFinal);
        }

        var ativos = usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioAlvoId);
        for (var ue : ativos) {
            if (delegaveis.contains(ue.getEmpresaId()) && !alvoIds.contains(ue.getEmpresaId())) {
                ue.setAtivo(false);
                usuarioEmpresaRepository.save(ue);
            }
        }

        var memberships = usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioAlvoId).stream()
                .filter(ue -> delegaveis.contains(ue.getEmpresaId()))
                .toList();
        return montarResumoMembro(usuario, memberships);
    }

    private static final Set<String> PAPEIS_GESTAO_LOCAL = Set.of(
            UsuarioEmpresa.PAPEL_OWNER,
            UsuarioEmpresa.PAPEL_ADMIN);

    public List<Map<String, Object>> listarEmpresasDelegaveis(Long gestorId, Long empresaSessaoId) {
        membershipService.requireGestao(gestorId, empresaSessaoId);
        return membershipService.listarEmpresasDelegaveisPorGestor(gestorId).stream()
                .<Map<String, Object>>map(e -> {
                    var item = new LinkedHashMap<String, Object>();
                    item.put("id", e.getId());
                    item.put("nome", e.getNome());
                    item.put("cnpj", e.getCnpj());
                    return item;
                })
                .toList();
    }

    private List<Long> resolverEmpresasAlvo(
            Set<Long> delegaveis,
            Long empresaPadraoId,
            List<Long> empresaIds) {
        if (empresaIds == null || empresaIds.isEmpty()) {
            if (!delegaveis.contains(empresaPadraoId)) {
                throw new IllegalArgumentException("Sem permissao para a empresa da sessao");
            }
            return List.of(empresaPadraoId);
        }
        var filtradas = empresaIds.stream().filter(delegaveis::contains).distinct().toList();
        if (filtradas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma empresa valida informada");
        }
        return filtradas;
    }

    private List<Long> resolverEmpresasConvite(
            Long gestorId,
            Long contaId,
            Long empresaPadraoId,
            List<Long> empresaIds) {
        var delegaveis = membershipService.listarEmpresasDelegaveis(gestorId, contaId).stream()
                .map(e -> e.getId())
                .collect(Collectors.toSet());
        return resolverEmpresasAlvo(delegaveis, empresaPadraoId, empresaIds);
    }

    private Map<String, Object> montarResumoMembro(Usuario user, List<UsuarioEmpresa> memberships) {
        var empresas = memberships.stream()
                .map(m -> {
                    var emp = empresaRepository.findById(m.getEmpresaId()).orElse(null);
                    var item = new LinkedHashMap<String, Object>();
                    item.put("empresaId", m.getEmpresaId());
                    item.put("papel", m.getPapel());
                    if (m.getPortalPerfilId() != null) {
                        item.put("portalPerfilId", m.getPortalPerfilId());
                    }
                    if (emp != null) {
                        item.put("empresaNome", emp.getNome());
                        item.put("cnpj", emp.getCnpj());
                    }
                    return item;
                })
                .toList();

        var body = new LinkedHashMap<String, Object>();
        body.put("id", user.getId());
        body.put("nome", user.getNome());
        body.put("email", user.getEmail());
        body.put("cpf", user.getCpf() != null ? user.getCpf() : "");
        body.put("ativo", user.isAtivo());
        body.put("empresas", empresas);
        body.put("papel", memberships.isEmpty() ? user.getPerfil() : memberships.getFirst().getPapel());
        var perfilId = memberships.stream()
                .map(UsuarioEmpresa::getPortalPerfilId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
        if (perfilId != null) {
            body.put("portalPerfilId", perfilId);
        }
        return body;
    }

    private Map<String, Object> resumoConvite(UsuarioConvite c) {
        var empresa = empresaRepository.findById(c.getEmpresaId()).orElse(null);
        var body = new LinkedHashMap<String, Object>();
        body.put("id", c.getId());
        body.put("email", c.getEmail());
        body.put("papel", c.getPapel());
        body.put("empresaId", c.getEmpresaId());
        body.put("token", c.getToken());
        body.put("expiraEm", c.getExpiraEm().toString());
        if (empresa != null) {
            body.put("empresaNome", empresa.getNome());
        }
        body.put("link", "/convite?token=" + c.getToken());
        return body;
    }

    private static String gerarTokenConvite() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
