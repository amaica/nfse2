package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.PortalPerfil;
import br.com.synki.nfse.portal.repository.PortalMenuRepository;
import br.com.synki.nfse.portal.repository.PortalPerfilRepository;
import br.com.synki.nfse.portal.web.dto.PortalPerfilDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortalPerfilService {

    private final PortalPerfilRepository repository;
    private final PortalMenuRepository menuRepository;
    private final MembershipService membershipService;

    public PortalPerfilService(
            PortalPerfilRepository repository,
            PortalMenuRepository menuRepository,
            MembershipService membershipService) {
        this.repository = repository;
        this.menuRepository = menuRepository;
        this.membershipService = membershipService;
    }

    @Transactional(readOnly = true)
    public List<PortalPerfilDto> listar(Long gestorId, Long empresaId) {
        membershipService.requireGestao(gestorId, empresaId);
        Long contaId = requireConta(empresaId);
        return repository.findByContaIdOrderByNomeAsc(contaId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PortalPerfilDto buscar(Long gestorId, Long empresaId, Long id) {
        membershipService.requireGestao(gestorId, empresaId);
        Long contaId = requireConta(empresaId);
        return toDto(repository.findByIdAndContaId(id, contaId)
                .orElseThrow(() -> new NoSuchElementException("Perfil nao encontrado")));
    }

    @Transactional
    public PortalPerfilDto salvar(Long gestorId, Long empresaId, PortalPerfilDto body) {
        membershipService.requireGestao(gestorId, empresaId);
        Long contaId = requireConta(empresaId);
        PortalPerfil perfil = new PortalPerfil();
        apply(perfil, body, contaId, null);
        return toDto(repository.save(perfil));
    }

    @Transactional
    public PortalPerfilDto atualizar(Long gestorId, Long empresaId, Long id, PortalPerfilDto body) {
        membershipService.requireGestao(gestorId, empresaId);
        Long contaId = requireConta(empresaId);
        PortalPerfil perfil = repository.findByIdAndContaId(id, contaId)
                .orElseThrow(() -> new NoSuchElementException("Perfil nao encontrado"));
        apply(perfil, body, contaId, id);
        return toDto(repository.save(perfil));
    }

    @Transactional
    public void excluir(Long gestorId, Long empresaId, Long id) {
        membershipService.requireGestao(gestorId, empresaId);
        Long contaId = requireConta(empresaId);
        PortalPerfil perfil = repository.findByIdAndContaId(id, contaId)
                .orElseThrow(() -> new NoSuchElementException("Perfil nao encontrado"));
        repository.delete(perfil);
    }

    private void apply(PortalPerfil perfil, PortalPerfilDto body, Long contaId, Long selfId) {
        String nome = body.nome == null ? "" : body.nome.trim();
        if (nome.isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio");
        }
        boolean nomeDuplicado = repository.findByContaIdOrderByNomeAsc(contaId).stream()
                .anyMatch(p -> p.getNome().equalsIgnoreCase(nome)
                        && (selfId == null || !selfId.equals(p.getId())));
        if (nomeDuplicado) {
            throw new IllegalArgumentException("Ja existe um perfil com este nome");
        }
        perfil.setContaId(contaId);
        perfil.setNome(nome);
        perfil.setDescricao(body.descricao == null || body.descricao.isBlank() ? null : body.descricao.trim());
        perfil.setAtivo(body.ativo == null || body.ativo);

        Set<Long> ids = body.menuIds == null ? Set.of() : new HashSet<>(body.menuIds);
        Set<Long> validos = menuRepository.findAllById(ids).stream()
                .map(m -> m.getId())
                .collect(Collectors.toSet());
        perfil.setMenuIds(validos);
    }

    private Long requireConta(Long empresaId) {
        Long contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) {
            throw new IllegalStateException("Conta nao encontrada para o emitente");
        }
        return contaId;
    }

    private PortalPerfilDto toDto(PortalPerfil p) {
        PortalPerfilDto dto = new PortalPerfilDto();
        dto.id = p.getId();
        dto.nome = p.getNome();
        dto.descricao = p.getDescricao();
        dto.ativo = p.isAtivo();
        dto.menuIds = p.getMenuIds() == null ? List.of() : p.getMenuIds().stream().sorted().toList();
        dto.totalMenus = dto.menuIds.size();
        return dto;
    }
}
