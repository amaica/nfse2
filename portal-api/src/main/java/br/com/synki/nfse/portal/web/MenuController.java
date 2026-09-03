package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.MenuService;
import br.com.synki.nfse.portal.web.dto.MenuDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;
    private final PortalAuthorization authorization;

    public MenuController(MenuService menuService, PortalAuthorization authorization) {
        this.menuService = menuService;
        this.authorization = authorization;
    }

    @GetMapping
    public List<MenuDto> listar(@AuthenticationPrincipal EmbedSession session) {
        // qualquer autenticado lê (sidebar); escrita é só gestão
        return menuService.listar();
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
