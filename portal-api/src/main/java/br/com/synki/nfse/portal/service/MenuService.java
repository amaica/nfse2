package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.PortalMenu;
import br.com.synki.nfse.portal.domain.PortalSubMenu;
import br.com.synki.nfse.portal.repository.PortalMenuRepository;
import br.com.synki.nfse.portal.web.dto.MenuDto;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MenuService {

    private final PortalMenuRepository repository;

    public MenuService(PortalMenuRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MenuDto> listar() {
        List<PortalMenu> menus = repository.findAllByOrderByOrdemMenuAscLabelAsc();
        List<MenuDto> out = new ArrayList<>(menus.size());
        for (PortalMenu menu : menus) {
            Hibernate.initialize(menu.getParent());
            Hibernate.initialize(menu.getSubmenus());
            out.add(toDto(menu));
        }
        return out;
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
        // filhos apontando para este menu: parent_id SET NULL via FK
        repository.deleteById(id);
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
