package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.MenuService;
import br.com.synki.nfse.portal.web.dto.MenuDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;
    private final PortalAuthorization authorization;

    public MenuController(MenuService menuService, PortalAuthorization authorization) {
        this.menuService = menuService;
        this.authorization = authorization;
    }

    public record MenusUsuarioRequest(Long empresaId, List<Long> menuIds) {}

    /** Sidebar: menus liberados para o usuário na empresa da sessão. */
    @GetMapping
    public List<MenuDto> listar(@AuthenticationPrincipal EmbedSession session) {
        return menuService.listarParaUsuario(session.usuarioId(), session.empresaId());
    }

    /** Catálogo completo (gestão) — configurar menu / ACL de usuários. */
    @GetMapping("/catalogo")
    public List<MenuDto> catalogo(@AuthenticationPrincipal EmbedSession session) {
        authorization.requireGestao(session);
        return menuService.listar();
    }

    @GetMapping("/usuario/{usuarioId}")
    public Map<String, Object> menusDoUsuario(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long usuarioId,
            @RequestParam(required = false) Long empresaId) {
        Long emp = empresaId != null ? empresaId : session.empresaId();
        List<Long> ids = menuService.listarMenuIdsDoUsuario(
                session.usuarioId(), session.empresaId(), usuarioId, emp);
        return Map.of("empresaId", emp, "menuIds", ids);
    }

    @PutMapping("/usuario/{usuarioId}")
    public Map<String, Object> salvarMenusDoUsuario(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long usuarioId,
            @RequestBody MenusUsuarioRequest body) {
        Long emp = body.empresaId() != null ? body.empresaId() : session.empresaId();
        menuService.salvarMenusDoUsuario(
                session.usuarioId(),
                session.empresaId(),
                usuarioId,
                emp,
                body.menuIds());
        return Map.of("empresaId", emp, "menuIds", body.menuIds() == null ? List.of() : body.menuIds());
    }

    @GetMapping("/{id}")
    public MenuDto buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return menuService.buscar(id);
    }

    @PostMapping
    public MenuDto salvar(@AuthenticationPrincipal EmbedSession session, @RequestBody MenuDto body) {
        authorization.requireGestao(session);
        return menuService.salvar(body);
    }

    @PutMapping("/{id}")
    public MenuDto atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody MenuDto body) {
        authorization.requireGestao(session);
        return menuService.atualizar(id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        authorization.requireGestao(session);
        menuService.excluir(id);
    }
}
