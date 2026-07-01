package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.EmpresaCadastroService;
import br.com.synki.nfse.portal.web.dto.AtualizarEmpresaRequest;
import br.com.synki.nfse.portal.web.dto.CriarEmpresaRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/empresas")
public class EmpresaAdminController {

    private final EmpresaCadastroService empresaCadastroService;

    public EmpresaAdminController(EmpresaCadastroService empresaCadastroService) {
        this.empresaCadastroService = empresaCadastroService;
    }

    @GetMapping
    public Object listar() {
        return Map.of("itens", empresaCadastroService.listar());
    }

    @GetMapping("/{id}")
    public Object obter(@PathVariable Long id) {
        return empresaCadastroService.obter(id);
    }

    @GetMapping("/cnpj/{cnpj}")
    public Object obterPorCnpj(@PathVariable String cnpj) {
        return empresaCadastroService.obterPorCnpj(cnpj);
    }

    @PostMapping
    public Object criar(@Valid @RequestBody CriarEmpresaRequest body) {
        var criada = empresaCadastroService.criar(body);
        return Map.of("ok", true, "empresa", criada);
    }

    @PutMapping("/{id}")
    public Object atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarEmpresaRequest body) {
        return Map.of("ok", true, "empresa", empresaCadastroService.atualizar(id, body));
    }

    @DeleteMapping("/{id}")
    public Object excluir(@PathVariable Long id) {
        empresaCadastroService.excluir(id);
        return Map.of("ok", true);
    }
}
