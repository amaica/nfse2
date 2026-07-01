package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.TributacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tribut-operacao-fiscal")
public class TributOperacaoFiscalController {

    private final TributacaoService service;

    public TributOperacaoFiscalController(TributacaoService service) {
        this.service = service;
    }

    @GetMapping
    public Object listar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(name = "simple", defaultValue = "false") boolean simple) {
        var lista = service.listarOperacoes(session.empresaId());
        if (!simple) {
            return lista;
        }
        return lista.stream()
                .map(o -> Map.of(
                        "id", o.getId(),
                        "descricao", o.getDescricao(),
                        "cfop", o.getCfop() != null ? o.getCfop() : 0,
                        "habilitarIbsCbs", o.isHabilitarIbsCbs()))
                .toList();
    }

    @GetMapping("/{id}")
    public TributOperacaoFiscal buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterOperacao(session.empresaId(), id);
    }

    @PostMapping
    public TributOperacaoFiscal salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody TributOperacaoFiscal body) {
        return service.salvarOperacao(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public TributOperacaoFiscal atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody TributOperacaoFiscal body) {
        return service.atualizarOperacao(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirOperacao(session.empresaId(), id);
    }
}
