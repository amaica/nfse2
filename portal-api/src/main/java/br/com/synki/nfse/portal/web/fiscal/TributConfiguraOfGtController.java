package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributConfiguraOfGt;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.TributacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tribut-configura-of-gt")
public class TributConfiguraOfGtController {

    private final TributacaoService service;

    public TributConfiguraOfGtController(TributacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TributConfiguraOfGt> listar(@AuthenticationPrincipal EmbedSession session) {
        return service.listarConfiguracoes(session.empresaId());
    }

    @GetMapping("/{id}")
    public TributConfiguraOfGt buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterConfiguracao(session.empresaId(), id);
    }

    @PostMapping
    public TributConfiguraOfGt salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody TributConfiguraOfGt body) {
        return service.salvarConfiguracao(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public TributConfiguraOfGt atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody TributConfiguraOfGt body) {
        return service.atualizarConfiguracao(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirConfiguracao(session.empresaId(), id);
    }
}
