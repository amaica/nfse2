package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.EmpresaCadastroService;
import br.com.synki.nfse.portal.service.MembershipService;
import br.com.synki.nfse.portal.service.NfeDistribuicaoDFeService;
import br.com.synki.nfse.portal.web.dto.AtualizarEmpresaRequest;
import br.com.synki.nfse.portal.web.dto.CriarEmpresaRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaPortalController {

    private final EmpresaCadastroService empresaCadastroService;
    private final MembershipService membershipService;
    private final NfeDistribuicaoDFeService distribuicaoDFeService;

    public EmpresaPortalController(
            EmpresaCadastroService empresaCadastroService,
            MembershipService membershipService,
            NfeDistribuicaoDFeService distribuicaoDFeService) {
        this.empresaCadastroService = empresaCadastroService;
        this.membershipService = membershipService;
        this.distribuicaoDFeService = distribuicaoDFeService;
    }

    @GetMapping
    public Object listar(@AuthenticationPrincipal EmbedSession session) {
        return Map.of("itens", empresaCadastroService.listarParaUsuario(session.usuarioId()));
    }

    @GetMapping("/{id}")
    public Object obter(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        return empresaCadastroService.obterParaUsuario(id, session.usuarioId());
    }

    @GetMapping("/cnpj/{cnpj}")
    public Object obterPorCnpj(@AuthenticationPrincipal EmbedSession session, @PathVariable String cnpj) {
        return empresaCadastroService.obterPorCnpjParaUsuario(cnpj, session.usuarioId());
    }

    @PostMapping
    public Object criar(
            @AuthenticationPrincipal EmbedSession session,
            @Valid @RequestBody CriarEmpresaRequest body) {
        var criada = empresaCadastroService.criarParaUsuario(body, session.usuarioId(), session.empresaId());
        return Map.of("ok", true, "empresa", criada);
    }

    @PutMapping("/{id}")
    public Object atualizar(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id,
            @Valid @RequestBody AtualizarEmpresaRequest body) {
        return Map.of("ok", true, "empresa", empresaCadastroService.atualizarParaUsuario(id, body, session.usuarioId()));
    }

    @DeleteMapping("/{id}")
    public Object excluir(@AuthenticationPrincipal EmbedSession session, @PathVariable Long id) {
        empresaCadastroService.excluirParaUsuario(id, session.usuarioId());
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/distribuicao/baixar")
    public Object baixarXmlsDestinatario(
            @AuthenticationPrincipal EmbedSession session,
            @PathVariable Long id) throws Exception {
        membershipService.requireOperador(session.usuarioId(), id);
        return distribuicaoDFeService.baixarEmpresa(id);
    }
}
