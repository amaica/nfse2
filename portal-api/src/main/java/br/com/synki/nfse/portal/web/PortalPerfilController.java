package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.PortalPerfilService;
import br.com.synki.nfse.portal.web.dto.PortalPerfilDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissoes")
public class PortalPerfilController {

    private final PortalPerfilService service;

    public PortalPerfilController(PortalPerfilService service) {
        this.service = service;
    }

    @GetMapping
    public List<PortalPerfilDto> listar(@AuthenticationPrincipal EmbedSession session) {
        return service.listar(session.usuarioId(), session.empresaId());
    }

    @GetMapping("/{id}")
    public PortalPerfilDto buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.buscar(session.usuarioId(), session.empresaId(), id);
    }

    @PostMapping
    public PortalPerfilDto salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody PortalPerfilDto body) {
        return service.salvar(session.usuarioId(), session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public PortalPerfilDto atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody PortalPerfilDto body) {
        return service.atualizar(session.usuarioId(), session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluir(session.usuarioId(), session.empresaId(), id);
    }
}
