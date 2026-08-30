package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cest;
import br.com.synki.nfse.portal.domain.fiscal.ProdutoGrupo;
import br.com.synki.nfse.portal.domain.fiscal.ProdutoSubgrupo;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.ProdutoClassificacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produto-grupo")
class ProdutoGrupoController {
    private final ProdutoClassificacaoService service;

    ProdutoGrupoController(ProdutoClassificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoGrupo> listar(@AuthenticationPrincipal EmbedSession session) {
        return service.listarGrupos(session.empresaId());
    }
}

@RestController
@RequestMapping("/api/produto-subgrupo")
class ProdutoSubgrupoController {
    private final ProdutoClassificacaoService service;

    ProdutoSubgrupoController(ProdutoClassificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoSubgrupo> listar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) Long grupoId) {
        return service.listarSubgrupos(session.empresaId(), grupoId);
    }
}

@RestController
@RequestMapping("/api/cest")
class CestController {
    private final ProdutoClassificacaoService service;

    CestController(ProdutoClassificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Cest> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String ncm) {
        return service.buscarCest(q, ncm);
    }
}
