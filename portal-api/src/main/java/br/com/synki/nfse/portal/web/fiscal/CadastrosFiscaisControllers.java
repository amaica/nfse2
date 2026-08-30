package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cfop;
import br.com.synki.nfse.portal.domain.fiscal.Ncm;
import br.com.synki.nfse.portal.domain.fiscal.Pessoa;
import br.com.synki.nfse.portal.domain.fiscal.Produto;
import br.com.synki.nfse.portal.domain.fiscal.Veiculo;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.UsuarioContaService;
import br.com.synki.nfse.portal.service.fiscal.CadastroFiscalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cfop")
class CfopController {
    private final CadastroFiscalService service;
    CfopController(CadastroFiscalService service) { this.service = service; }

    @GetMapping
    public List<Cfop> listar(@AuthenticationPrincipal EmbedSession session) {
        return service.listarCfop(session.empresaId());
    }

    @PostMapping
    public Cfop salvar(@AuthenticationPrincipal EmbedSession session, @RequestBody Cfop body) {
        return service.salvarCfop(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public Cfop atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody Cfop body) {
        return service.atualizarCfop(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirCfop(session.empresaId(), id);
    }
}

@RestController
@RequestMapping("/api/ncm")
class NcmController {
    private final CadastroFiscalService service;
    private final PortalAuthorization authz;
    NcmController(CadastroFiscalService service, PortalAuthorization authz) {
        this.service = service;
        this.authz = authz;
    }

    @GetMapping
    public List<Ncm> listar(@RequestParam(required = false) String q) {
        return service.listarNcm(q);
    }

    @PostMapping
    public Ncm salvar(@AuthenticationPrincipal EmbedSession session, @RequestBody Ncm body) {
        authz.requireGestao(session);
        return service.salvarNcm(body);
    }

    @PutMapping("/{id}")
    public Ncm atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody Ncm body) {
        authz.requireGestao(session);
        return service.atualizarNcm(id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        authz.requireGestao(session);
        service.excluirNcm(id);
    }
}

@RestController
@RequestMapping("/api/pessoas")
class PessoasController {
    private final CadastroFiscalService service;
    PessoasController(CadastroFiscalService service) { this.service = service; }

    @GetMapping
    public Object listar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) String q,
            @RequestParam(name = "simple", defaultValue = "false") boolean simple) {
        var lista = service.listarPessoas(session.empresaId(), q);
        if (!simple) return lista;
        return lista.stream()
                .map(p -> Map.of(
                        "id", p.getId(),
                        "nome", p.getNome(),
                        "cpfCnpj", p.getCpfCnpj() != null ? p.getCpfCnpj() : ""))
                .toList();
    }

    @GetMapping("/busca")
    public List<Map<String, Object>> busca(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam String q) {
        return service.listarPessoas(session.empresaId(), q).stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "nome", p.getNome(),
                        "cpfCnpj", p.getCpfCnpj() != null ? p.getCpfCnpj() : ""))
                .toList();
    }

    @GetMapping("/{id}")
    public Pessoa buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterPessoa(session.empresaId(), id);
    }

    @PostMapping
    public Pessoa salvar(@AuthenticationPrincipal EmbedSession session, @RequestBody Pessoa body) {
        return service.salvarPessoa(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public Pessoa atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody Pessoa body) {
        return service.atualizarPessoa(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirPessoa(session.empresaId(), id);
    }
}

@RestController
@RequestMapping("/api/produto")
class ProdutoController {
    private final CadastroFiscalService service;
    ProdutoController(CadastroFiscalService service) { this.service = service; }

    @GetMapping
    public Object listar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(name = "simple", defaultValue = "false") boolean simple) {
        var lista = simple ? service.listarProdutosAtivos(session.empresaId()) : service.listarProdutos(session.empresaId());
        if (!simple) return lista;
        return lista.stream()
                .map(p -> Map.of("id", p.getId(), "codigo", p.getCodigo(), "nome", p.getNome()))
                .toList();
    }

    @GetMapping("/busca")
    public List<Map<String, Object>> busca(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam String q) {
        return service.buscarProdutos(session.empresaId(), q).stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("codigo", p.getCodigo());
                    m.put("nome", p.getNome());
                    m.put("unidade", p.getUnidade() != null ? p.getUnidade() : "UN");
                    if (p.getValorUnitario() != null) {
                        m.put("valorUnitario", p.getValorUnitario());
                    }
                    return m;
                })
                .toList();
    }

    @GetMapping("/{id}")
    public Produto buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterProduto(session.empresaId(), id);
    }

    @PostMapping
    public Produto salvar(@AuthenticationPrincipal EmbedSession session, @RequestBody Produto body) {
        return service.salvarProduto(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public Produto atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody Produto body) {
        return service.atualizarProduto(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirProduto(session.empresaId(), id);
    }
}

@RestController
@RequestMapping("/api/veiculo")
class VeiculoController {
    private final CadastroFiscalService service;
    VeiculoController(CadastroFiscalService service) { this.service = service; }

    @GetMapping
    public Object listar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(name = "simple", defaultValue = "false") boolean simple) {
        var lista = simple ? service.listarVeiculosAtivos(session.empresaId()) : service.listarVeiculos(session.empresaId());
        if (!simple) return lista;
        return lista.stream()
                .map(v -> Map.of("id", v.getId(), "placa", v.getPlaca(), "modelo", v.getModelo() != null ? v.getModelo() : ""))
                .toList();
    }

    @GetMapping("/{id}")
    public Veiculo buscar(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return service.obterVeiculo(session.empresaId(), id);
    }

    @PostMapping
    public Veiculo salvar(@AuthenticationPrincipal EmbedSession session, @RequestBody Veiculo body) {
        return service.salvarVeiculo(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public Veiculo atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody Veiculo body) {
        return service.atualizarVeiculo(session.empresaId(), id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        service.excluirVeiculo(session.empresaId(), id);
    }
}

@RestController
@RequestMapping("/api/usuario")
class UsuarioFiscalController {
    private final CadastroFiscalService service;
    private final UsuarioContaService usuarioContaService;
    private final PortalAuthorization authz;

    UsuarioFiscalController(
            CadastroFiscalService service,
            UsuarioContaService usuarioContaService,
            PortalAuthorization authz) {
        this.service = service;
        this.usuarioContaService = usuarioContaService;
        this.authz = authz;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@AuthenticationPrincipal EmbedSession session) {
        authz.requireGestao(session);
        return usuarioContaService.listarMembros(session.usuarioId(), session.empresaId());
    }

    @PostMapping
    public Map<String, Object> salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody CadastroFiscalService.UsuarioRequest body) {
        return service.salvarUsuario(session.usuarioId(), session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody CadastroFiscalService.UsuarioRequest body) {
        return service.atualizarUsuario(session.usuarioId(), session.empresaId(), id, body);
    }
}
