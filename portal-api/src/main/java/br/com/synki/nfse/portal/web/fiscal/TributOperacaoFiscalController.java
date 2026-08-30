package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.TributacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

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
                .map(o -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("id", o.getId());
                    m.put("descricao", o.getDescricao());
                    if (o.getDescricaoNaNf() != null && !o.getDescricaoNaNf().isBlank()) {
                        m.put("descricaoNaNf", o.getDescricaoNaNf());
                    }
                    if (o.getTipoOperacao() != null && !o.getTipoOperacao().isBlank()) {
                        m.put("tipoOperacao", o.getTipoOperacao());
                    }
                    if (o.getPrincipal() != null) {
                        m.put("principal", o.getPrincipal());
                    }
                    if (cfopValido(o.getCfop())) {
                        m.put("cfop", o.getCfop());
                    }
                    if (o.getGeraFinanceiro() != null && !o.getGeraFinanceiro().isBlank()) {
                        m.put("geraFinanceiro", o.getGeraFinanceiro());
                    }
                    if (o.getFinalidadeOperacao() != null && !o.getFinalidadeOperacao().isBlank()) {
                        m.put("finalidadeOperacao", o.getFinalidadeOperacao());
                    }
                    if (o.getObservacao() != null && !o.getObservacao().isBlank()) {
                        m.put("observacao", o.getObservacao());
                    }
                    m.put("habilitarIbsCbs", o.isHabilitarIbsCbs());
                    return m;
                })
                .toList();
    }

    private static boolean cfopValido(Integer cfop) {
        return cfop != null && cfop >= 1000 && cfop <= 7999;
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
