package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.service.UsuarioContaService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conta")
public class ContaUsuarioController {

    private final UsuarioContaService usuarioContaService;

    public ContaUsuarioController(UsuarioContaService usuarioContaService) {
        this.usuarioContaService = usuarioContaService;
    }

    public record ConviteRequest(
            @NotBlank String email,
            String papel,
            List<Long> empresaIds) {}

    public record VincularRequest(
            Long usuarioId,
            List<Long> empresaIds,
            String papel) {}

    @GetMapping("/usuarios")
    public List<Map<String, Object>> listarMembros(@AuthenticationPrincipal EmbedSession session) {
        return usuarioContaService.listarMembros(session.usuarioId(), session.empresaId());
    }

    @GetMapping("/empresas-delegaveis")
    public List<Map<String, Object>> listarEmpresasDelegaveis(@AuthenticationPrincipal EmbedSession session) {
        return usuarioContaService.listarEmpresasDelegaveis(session.usuarioId(), session.empresaId());
    }

    @GetMapping("/convites")
    public List<Map<String, Object>> listarConvites(@AuthenticationPrincipal EmbedSession session) {
        return usuarioContaService.listarConvitesPendentes(session.usuarioId(), session.empresaId());
    }

    @PostMapping("/convites")
    public Map<String, Object> criarConvite(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody ConviteRequest body) {
        return usuarioContaService.criarConvite(
                session.usuarioId(),
                session.empresaId(),
                body.email(),
                body.papel() != null ? body.papel() : UsuarioEmpresa.PAPEL_OPERADOR,
                body.empresaIds());
    }

    @PostMapping("/usuarios/vincular")
    public Map<String, Object> vincularEmpresas(
            @AuthenticationPrincipal EmbedSession session,
            @RequestBody VincularRequest body) {
        if (body.usuarioId() == null) {
            throw new IllegalArgumentException("usuarioId obrigatorio");
        }
        return usuarioContaService.vincularEmpresas(
                session.usuarioId(),
                session.empresaId(),
                body.usuarioId(),
                body.empresaIds(),
                body.papel());
    }
}
