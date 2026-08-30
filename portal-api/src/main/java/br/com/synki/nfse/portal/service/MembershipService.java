package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Assinatura;
import br.com.synki.nfse.portal.domain.Conta;
import br.com.synki.nfse.portal.domain.ContaEmpresa;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaEmpresaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MembershipService {

    private static final Set<String> PAPEIS_GESTAO = Set.of(
            UsuarioEmpresa.PAPEL_OWNER,
            UsuarioEmpresa.PAPEL_ADMIN);

    private static final Set<String> PAPEIS_ESCRITA = Set.of(
            UsuarioEmpresa.PAPEL_OWNER,
            UsuarioEmpresa.PAPEL_ADMIN,
            UsuarioEmpresa.PAPEL_OPERADOR);

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final ContaRepository contaRepository;
    private final ContaEmpresaRepository contaEmpresaRepository;
    private final EmpresaRepository empresaRepository;
    private final AssinaturaRepository assinaturaRepository;

    public MembershipService(
            UsuarioEmpresaRepository usuarioEmpresaRepository,
            ContaRepository contaRepository,
            ContaEmpresaRepository contaEmpresaRepository,
            EmpresaRepository empresaRepository,
            AssinaturaRepository assinaturaRepository) {
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.contaRepository = contaRepository;
        this.contaEmpresaRepository = contaEmpresaRepository;
        this.empresaRepository = empresaRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    public boolean hasAccess(Long usuarioId, Long empresaId) {
        if (usuarioId == null || empresaId == null) {
            return false;
        }
        return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaIdAndAtivoTrue(usuarioId, empresaId);
    }

    public void requireAccess(Long usuarioId, Long empresaId) {
        if (!hasAccess(usuarioId, empresaId)) {
            throw new AccessDeniedException("Sem permissao para esta empresa");
        }
    }

    public void requireGestao(Long usuarioId, Long empresaId) {
        var membership = membershipAtivo(usuarioId, empresaId);
        if (!PAPEIS_GESTAO.contains(membership.getPapel())) {
            throw new AccessDeniedException("Permissao insuficiente para esta operacao");
        }
    }

    public void requireOperador(Long usuarioId, Long empresaId) {
        var membership = membershipAtivo(usuarioId, empresaId);
        if (!PAPEIS_ESCRITA.contains(membership.getPapel())) {
            throw new AccessDeniedException("Perfil somente leitura — operacao nao permitida");
        }
    }

    public String papelAtivo(Long usuarioId, Long empresaId) {
        return membershipAtivo(usuarioId, empresaId).getPapel();
    }

    public boolean usuarioNaConta(Long usuarioId, Long contaId) {
        if (usuarioId == null || contaId == null) {
            return false;
        }
        return usuarioEmpresaRepository.existsByUsuarioIdAndContaIdAndAtivoTrue(usuarioId, contaId);
    }

    public Long resolverEmpresaInicial(Long usuarioId, Long empresaPreferida) {
        if (empresaPreferida != null && empresaPreferida > 0 && hasAccess(usuarioId, empresaPreferida)) {
            return empresaPreferida;
        }
        return usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioId).stream()
                .map(UsuarioEmpresa::getEmpresaId)
                .filter(id -> empresaRepository.findById(id).map(Empresa::isAtivo).orElse(false))
                .findFirst()
                .orElse(null);
    }

    /** Todas as empresas (emitentes) vinculadas à conta — independente de usuário. */
    public List<Empresa> listarTodasEmpresasPermitidasPorConta(Long contaId) {
        var ids = contaEmpresaRepository.findByContaId(contaId).stream()
                .map(ContaEmpresa::getEmpresaId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return empresaRepository.findAllById(ids).stream()
                .filter(Empresa::isAtivo)
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();
    }

    /** Empresas que o usuário pode delegar (convite/vínculo) dentro da conta. */
    public List<Empresa> listarEmpresasDelegaveis(Long usuarioId, Long contaId) {
        var naConta = contaEmpresaRepository.findByContaId(contaId).stream()
                .map(ContaEmpresa::getEmpresaId)
                .collect(java.util.stream.Collectors.toSet());
        return listarTodasEmpresasPermitidas(usuarioId).stream()
                .filter(e -> naConta.contains(e.getId()))
                .toList();
    }

    private UsuarioEmpresa membershipAtivo(Long usuarioId, Long empresaId) {
        return usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaIdAndAtivoTrue(usuarioId, empresaId)
                .orElseThrow(() -> new AccessDeniedException("Sem permissao para esta empresa"));
    }

    public List<Empresa> listarEmpresasPermitidas(Long usuarioId, String termo, int limite) {
        int max = limite <= 0 ? 40 : Math.min(limite, 100);
        String q = termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT);
        String cnpj = q.replaceAll("\\D", "");
        if (cnpj.length() < 3) {
            cnpj = "";
        }

        var ids = usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioId).stream()
                .map(UsuarioEmpresa::getEmpresaId)
                .toList();

        if (ids.isEmpty()) {
            return List.of();
        }

        if (q.length() < 2) {
            return empresaRepository.findAllById(ids).stream()
                    .filter(Empresa::isAtivo)
                    .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                    .limit(max)
                    .toList();
        }

        return empresaRepository.buscarAtivasEntreIds(ids, q, cnpj).stream()
                .limit(max)
                .toList();
    }

    public List<Empresa> listarTodasEmpresasPermitidas(Long usuarioId) {
        var ids = usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioId).stream()
                .map(UsuarioEmpresa::getEmpresaId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return empresaRepository.findAllById(ids).stream()
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();
    }

    public Long contaIdDaEmpresa(Long empresaId) {
        return contaEmpresaRepository.findByEmpresaId(empresaId)
                .map(ContaEmpresa::getContaId)
                .orElse(null);
    }

    public Long contaIdDoUsuario(Long usuarioId) {
        return contaIdDoUsuario(usuarioId, null);
    }

    /** Resolve conta preferindo a do emitente ativo na sessão. */
    public Long contaIdDoUsuario(Long usuarioId, Long empresaId) {
        if (empresaId != null && empresaId > 0) {
            var contaEmpresa = contaEmpresaRepository.findByEmpresaId(empresaId).orElse(null);
            if (contaEmpresa != null && usuarioNaConta(usuarioId, contaEmpresa.getContaId())) {
                return contaEmpresa.getContaId();
            }
        }
        var contas = usuarioEmpresaRepository.findContaIdsByUsuarioId(usuarioId);
        if (!contas.isEmpty()) {
            return contas.getFirst();
        }
        return contaRepository.findByOwnerUsuarioId(usuarioId)
                .map(Conta::getId)
                .orElse(null);
    }

    public boolean precisaOnboarding(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        if (!usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioId).isEmpty()) {
            return false;
        }
        return contaRepository.findByOwnerUsuarioId(usuarioId).isPresent();
    }

    @Transactional
    public void vincularUsuarioEmpresa(Long usuarioId, Long empresaId, Long contaId, String papel) {
        var papelFinal = papel != null ? papel : UsuarioEmpresa.PAPEL_OPERADOR;
        var existente = usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId);
        if (existente.isPresent()) {
            var ue = existente.get();
            ue.setAtivo(true);
            ue.setPapel(papelFinal);
            usuarioEmpresaRepository.save(ue);
            return;
        }
        usuarioEmpresaRepository.save(UsuarioEmpresa.vincular(usuarioId, empresaId, contaId, papelFinal));
    }

    /** Emitentes em que o usuário tem papel OWNER ou ADMIN (pode delegar acesso). */
    public List<Empresa> listarEmpresasDelegaveisPorGestor(Long usuarioId) {
        var ids = usuarioEmpresaRepository.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioId).stream()
                .filter(ue -> PAPEIS_GESTAO.contains(ue.getPapel()))
                .map(UsuarioEmpresa::getEmpresaId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return empresaRepository.findAllById(ids).stream()
                .filter(Empresa::isAtivo)
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();
    }

    @Transactional
    public Conta provisionarContaParaEmpresa(Empresa empresa, Usuario ownerUsuario, String papelOwner) {
        var conta = Conta.criar("Conta — " + empresa.getNome(), ownerUsuario != null ? ownerUsuario.getId() : null);
        contaRepository.save(conta);
        contaEmpresaRepository.save(ContaEmpresa.of(conta.getId(), empresa.getId()));
        if (assinaturaRepository.findByContaId(conta.getId()).isEmpty()) {
            assinaturaRepository.save(Assinatura.trial(conta.getId()));
        }
        if (ownerUsuario != null) {
            vincularUsuarioEmpresa(ownerUsuario.getId(), empresa.getId(), conta.getId(), papelOwner);
        }
        return conta;
    }

    @Transactional
    public void vincularEmpresaAContaExistente(
            Long contaId,
            Long empresaId,
            Usuario usuario,
            String papel) {
        if (contaEmpresaRepository.findByEmpresaId(empresaId).isPresent()) {
            return;
        }
        contaEmpresaRepository.save(ContaEmpresa.of(contaId, empresaId));
        if (usuario != null) {
            vincularUsuarioEmpresa(usuario.getId(), empresaId, contaId, papel);
        }
    }

    @Transactional
    public void removerVinculosEmpresa(Long empresaId) {
        usuarioEmpresaRepository.deleteByEmpresaId(empresaId);
        contaEmpresaRepository.findByEmpresaId(empresaId).ifPresent(contaEmpresaRepository::delete);
    }
}
