package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributNfseServico;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.TributacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tribut-nfse-servico")
public class TributNfseServicoController {

    private final TributacaoService service;

    public TributNfseServicoController(TributacaoService service) {
        this.service = service;
    }

    @GetMapping
    public Object listar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(name = "simple", defaultValue = "false") boolean simple,
            @RequestParam(name = "ativos", defaultValue = "true") boolean ativos,
            @RequestParam(required = false) String q) {
        List<TributNfseServico> lista = service.listarNfseServicos(session.empresaId(), ativos, q);
        if (!simple) {
            return lista;
        }
        return lista.stream()
                .map(s -> Map.of(
                        "id", s.getId(),
                        "descricao", s.getDescricao(),
                        "itemListaServico", s.getItemListaServico(),
                        "aliquotaIss", s.getAliquotaIss() != null ? s.getAliquotaIss() : 0,
                        "principal", s.isPrincipal()))
                .toList();
    }

    @GetMapping("/{id}")
    public TributNfseServico buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterNfseServico(session.empresaId(), id);
    }

    @PostMapping
    public TributNfseServico salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody TributNfseServico body) {
        return service.salvarNfseServico(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public TributNfseServico atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody TributNfseServico body) {
        return service.atualizarNfseServico(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirNfseServico(session.empresaId(), id);
    }
}
