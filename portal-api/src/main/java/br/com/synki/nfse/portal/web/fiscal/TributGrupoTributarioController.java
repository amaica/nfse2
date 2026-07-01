package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributGrupoTributario;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.TributacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tribut-grupo-tributario")
public class TributGrupoTributarioController {

    private final TributacaoService service;

    public TributGrupoTributarioController(TributacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TributGrupoTributario> listar(@AuthenticationPrincipal EmbedSession session) {
        return service.listarGrupos(session.empresaId());
    }

    @GetMapping("/{id}")
    public TributGrupoTributario buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterGrupo(session.empresaId(), id);
    }

    @PostMapping
    public TributGrupoTributario salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody TributGrupoTributario body) {
        return service.salvarGrupo(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public TributGrupoTributario atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody TributGrupoTributario body) {
        return service.atualizarGrupo(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirGrupo(session.empresaId(), id);
    }
}
