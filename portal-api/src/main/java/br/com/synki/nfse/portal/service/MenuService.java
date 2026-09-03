package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.PortalMenu;
import br.com.synki.nfse.portal.domain.PortalMenuAcesso;
import br.com.synki.nfse.portal.domain.PortalPerfil;
import br.com.synki.nfse.portal.domain.PortalSubMenu;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.PortalMenuAcessoRepository;
import br.com.synki.nfse.portal.repository.PortalMenuRepository;
import br.com.synki.nfse.portal.repository.PortalPerfilRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import br.com.synki.nfse.portal.web.dto.MenuDto;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final Set<String> PAPEIS_GESTAO = Set.of(
            UsuarioEmpresa.PAPEL_OWNER,
            UsuarioEmpresa.PAPEL_ADMIN);

    private final PortalMenuRepository repository;
    private final PortalMenuAcessoRepository acessoRepository;
    private final PortalPerfilRepository perfilRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final MembershipService membershipService;

    public MenuService(
            PortalMenuRepository repository,
            PortalMenuAcessoRepository acessoRepository,
            PortalPerfilRepository perfilRepository,
            UsuarioEmpresaRepository usuarioEmpresaRepository,
            MembershipService membershipService) {
        this.repository = repository;
        this.acessoRepository = acessoRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.membershipService = membershipService;
    }

    @Transactional(readOnly = true)
    public List<MenuDto> listar() {
        return toDtoList(repository.findAllByOrderByOrdemMenuAscLabelAsc());
    }

    /** Menus visíveis no lateral para o usuário na empresa da sessão. */
    @Transactional(readOnly = true)
    public List<MenuDto> listarParaUsuario(Long usuarioId, Long empresaId) {
        membershipService.requireAccess(usuarioId, empresaId);
        var membership = usuarioEmpresaRepository.findByUsuarioIdAndEmpresaIdAndAtivoTrue(usuarioId, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Membership nao encontrado"));
        List<PortalMenu> todos = repository.findAllByOrderByOrdemMenuAscLabelAsc();

        if (PAPEIS_GESTAO.contains(membership.getPapel())) {
            return toDtoList(todos.stream().filter(PortalMenu::isAtivo).toList());
        }

        // 1) Grupo de permissão (perfil) — prioridade
        if (membership.getPortalPerfilId() != null) {
            PortalPerfil perfil = perfilRepository.findById(membership.getPortalPerfilId()).orElse(null);
            if (perfil != null && perfil.isAtivo() && perfil.getMenuIds() != null && !perfil.getMenuIds().isEmpty()) {
                Set<Long> liberados = expandirComAncestrais(todos, new HashSet<>(perfil.getMenuIds()));
                return toDtoList(todos.stream()
                        .filter(m -> m.isAtivo() && m.getId() != null && liberados.contains(m.getId()))
                        .toList());
            }
            // perfil sem menus → lateral vazio (só Início se existir no catálogo? não — vazio intencional)
            return List.of();
        }

        // 2) ACL por usuário (legado)
        List<Long> explicitos = acessoRepository.findMenuIdsByUsuarioIdAndEmpresaId(usuarioId, empresaId);
        if (!explicitos.isEmpty()) {
            Set<Long> liberados = expandirComAncestrais(todos, new HashSet<>(explicitos));
            return toDtoList(todos.stream()
                    .filter(m -> m.isAtivo() && m.getId() != null && liberados.contains(m.getId()))
                    .toList());
        }

        // 3) Fallback operadorTemAcesso
        return toDtoList(todos.stream()
                .filter(m -> m.isAtivo()
                        && !"NAO".equalsIgnoreCase(String.valueOf(m.getOperadorTemAcesso())))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<Long> listarMenuIdsDoUsuario(Long gestorId, Long empresaSessao, Long usuarioId, Long empresaId) {
        membershipService.requireGestao(gestorId, empresaSessao);
        membershipService.requireAccess(gestorId, empresaId);
        membershipService.requireAccess(usuarioId, empresaId);
        return acessoRepository.findMenuIdsByUsuarioIdAndEmpresaId(usuarioId, empresaId);
    }

    @Transactional
    public void salvarMenusDoUsuario(
            Long gestorId,
            Long empresaSessao,
            Long usuarioId,
            Long empresaId,
            List<Long> menuIds) {
        membershipService.requireGestao(gestorId, empresaSessao);
        membershipService.requireAccess(gestorId, empresaId);
        membershipService.requireAccess(usuarioId, empresaId);

        String papelAlvo = membershipService.papelAtivo(usuarioId, empresaId);
        if (PAPEIS_GESTAO.contains(papelAlvo)) {
            // ADMIN/OWNER vê tudo — limpa ACL específica
            acessoRepository.deleteByUsuarioIdAndEmpresaId(usuarioId, empresaId);
            return;
        }

        Set<Long> validos = repository.findAllById(menuIds == null ? List.of() : menuIds).stream()
                .map(PortalMenu::getId)
                .collect(Collectors.toSet());

        acessoRepository.deleteByUsuarioIdAndEmpresaId(usuarioId, empresaId);
        List<PortalMenuAcesso> rows = new ArrayList<>();
        for (Long menuId : validos) {
            rows.add(PortalMenuAcesso.of(usuarioId, empresaId, menuId));
        }
        if (!rows.isEmpty()) {
            acessoRepository.saveAll(rows);
        }
    }

    @Transactional(readOnly = true)
    public MenuDto buscar(Long id) {
        PortalMenu menu = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu nao encontrado"));
        Hibernate.initialize(menu.getParent());
        Hibernate.initialize(menu.getSubmenus());
        return toDto(menu);
    }

    @Transactional
    public MenuDto salvar(MenuDto body) {
        PortalMenu menu = new PortalMenu();
        apply(menu, body, null);
        return toDto(repository.save(menu));
    }

    @Transactional
    public MenuDto atualizar(Long id, MenuDto body) {
        PortalMenu menu = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu nao encontrado"));
        apply(menu, body, id);
        return toDto(repository.save(menu));
    }

    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Menu nao encontrado");
        }
        repository.deleteById(id);
    }

    private Set<Long> expandirComAncestrais(List<PortalMenu> todos, Set<Long> selecionados) {
        Map<Long, PortalMenu> byId = todos.stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(PortalMenu::getId, m -> m, (a, b) -> a));
        Set<Long> out = new LinkedHashSet<>(selecionados);
        for (Long id : selecionados) {
            PortalMenu cur = byId.get(id);
            while (cur != null && cur.getParent() != null && cur.getParent().getId() != null) {
                Long pid = cur.getParent().getId();
                out.add(pid);
                cur = byId.get(pid);
            }
        }
        return out;
    }

    private List<MenuDto> toDtoList(List<PortalMenu> menus) {
        List<MenuDto> out = new ArrayList<>(menus.size());
        for (PortalMenu menu : menus) {
            Hibernate.initialize(menu.getParent());
            Hibernate.initialize(menu.getSubmenus());
            out.add(toDto(menu));
        }
        return out;
    }

    private void apply(PortalMenu menu, MenuDto body, Long selfId) {
        String label = body.label == null ? "" : body.label.trim();
        if (label.isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio");
        }
        Integer ordem = body.ordemMenu == null ? 0 : body.ordemMenu;
        if (ordem < 0) {
            throw new IllegalArgumentException("Ordem deve ser >= 0");
        }

        menu.setLabel(label);
        menu.setIcon(trimToNull(body.icon));
        menu.setOutcome(trimToNull(body.outcome));
        menu.setOrdemMenu(ordem);
        menu.setAtivo(body.ativo == null || body.ativo);
        String acesso = body.operadorTemAcesso == null ? "SIM" : body.operadorTemAcesso.trim().toUpperCase();
        if (!acesso.equals("SIM") && !acesso.equals("NAO")) {
            throw new IllegalArgumentException("operadorTemAcesso deve ser SIM ou NAO");
        }
        menu.setOperadorTemAcesso(acesso);

        if (body.parent != null && body.parent.id != null) {
            if (selfId != null && body.parent.id.equals(selfId)) {
                throw new IllegalArgumentException("Menu nao pode ser pai de si mesmo");
            }
            PortalMenu parent = repository.findById(body.parent.id)
                    .orElseThrow(() -> new IllegalArgumentException("Menu pai nao encontrado"));
            menu.setParent(parent);
        } else {
            menu.setParent(null);
        }

        List<PortalSubMenu> nextSubs = new ArrayList<>();
        if (body.submenus != null) {
            for (MenuDto.SubMenuDto s : body.submenus) {
                String subLabel = s.label == null ? "" : s.label.trim();
                if (subLabel.isBlank()) continue;
                PortalSubMenu sub = new PortalSubMenu();
                sub.setLabel(subLabel);
                sub.setIcon(trimToNull(s.icon));
                sub.setOutcome(trimToNull(s.outcome));
                nextSubs.add(sub);
            }
        }
        menu.replaceSubmenus(nextSubs);
    }

    private static MenuDto toDto(PortalMenu menu) {
        MenuDto dto = new MenuDto();
        dto.id = menu.getId();
        dto.label = menu.getLabel();
        dto.icon = menu.getIcon();
        dto.ordemMenu = menu.getOrdemMenu();
        dto.outcome = menu.getOutcome();
        dto.operadorTemAcesso = menu.getOperadorTemAcesso();
        dto.ativo = menu.isAtivo();
        if (menu.getParent() != null) {
            dto.parent = new MenuDto.MenuRefDto(menu.getParent().getId(), menu.getParent().getLabel());
        }
        for (PortalSubMenu sub : menu.getSubmenus()) {
            MenuDto.SubMenuDto s = new MenuDto.SubMenuDto();
            s.id = sub.getId();
            s.label = sub.getLabel();
            s.icon = sub.getIcon();
            s.outcome = sub.getOutcome();
            dto.submenus.add(s);
        }
        return dto;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
