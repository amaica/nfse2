package br.com.synki.nfse.portal.web.fiscal;

import br.com.synki.nfse.portal.security.EmbedSession;
import br.com.synki.nfse.portal.security.PortalAuthorization;
import br.com.synki.nfse.portal.service.fiscal.ReformaCatalogoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tributacao/reforma")
public class ReformaCatalogoController {

    private final ReformaCatalogoService catalogo;
    private final PortalAuthorization authz;

    public ReformaCatalogoController(ReformaCatalogoService catalogo, PortalAuthorization authz) {
        this.catalogo = catalogo;
        this.authz = authz;
    }

    @GetMapping("/cst-ibs-cbs")
    public List<Map<String, Object>> cstIbsCbs(@AuthenticationPrincipal EmbedSession session) {
        authz.requireOperador(session);
        return catalogo.listarCstIbsCbs();
    }

    @GetMapping("/class-trib")
    public List<Map<String, Object>> classTrib(
            @AuthenticationPrincipal EmbedSession session,
            @RequestParam(required = false) String cst) {
        authz.requireOperador(session);
        return catalogo.listarClassTrib(cst);
    }

    @GetMapping("/cst-is")
    public List<Map<String, Object>> cstIs(@AuthenticationPrincipal EmbedSession session) {
        authz.requireOperador(session);
        return catalogo.listarCstIs();
    }
}
