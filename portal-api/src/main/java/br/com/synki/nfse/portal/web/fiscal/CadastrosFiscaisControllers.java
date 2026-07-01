package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cfop;
import br.com.synki.nfse.portal.domain.fiscal.Ncm;
import br.com.synki.nfse.portal.domain.fiscal.Pessoa;
import br.com.synki.nfse.portal.domain.fiscal.Produto;
import br.com.synki.nfse.portal.domain.fiscal.Veiculo;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.fiscal.CadastroFiscalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public Cfop atualizar(@PathVariable Long id, @RequestBody Cfop body) {
        return service.atualizarCfop(id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) { service.excluirCfop(id); }
}

@RestController
@RequestMapping("/api/ncm")
class NcmController {
    private final CadastroFiscalService service;
    NcmController(CadastroFiscalService service) { this.service = service; }

    @GetMapping
    public List<Ncm> listar(@RequestParam(required = false) String q) {
        return service.listarNcm(q);
    }

    @PostMapping
    public Ncm salvar(@RequestBody Ncm body) { return service.salvarNcm(body); }

    @PutMapping("/{id}")
    public Ncm atualizar(@PathVariable Long id, @RequestBody Ncm body) {
        return service.atualizarNcm(id, body);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) { service.excluirNcm(id); }
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
        var lista = service.listarProdutos(session.empresaId());
        if (!simple) return lista;
        return lista.stream()
                .map(p -> Map.of("id", p.getId(), "codigo", p.getCodigo(), "nome", p.getNome()))
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
        var lista = service.listarVeiculos(session.empresaId());
        if (!simple) return lista;
        return lista.stream()
                .map(v -> Map.of("id", v.getId(), "placa", v.getPlaca(), "modelo", v.getModelo() != null ? v.getModelo() : ""))
                .toList();
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
    UsuarioFiscalController(CadastroFiscalService service) { this.service = service; }

    @GetMapping
    public List<Map<String, Object>> listar(@AuthenticationPrincipal EmbedSession session) {
        return service.listarUsuarios(session.empresaId());
    }

    @PostMapping
    public Map<String, Object> salvar(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody CadastroFiscalService.UsuarioRequest body) {
        return service.salvarUsuario(session.empresaId(), body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @RequestBody CadastroFiscalService.UsuarioRequest body) {
        return service.atualizarUsuario(session.empresaId(), id, body);
    }
}
