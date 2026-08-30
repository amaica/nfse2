package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.service.MembershipService;
import org.springframework.stereotype.Component;

@Component
public class PortalAuthorization {

    private final MembershipService membershipService;

    public PortalAuthorization(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public void requireOperador(EmbedSession session) {
        membershipService.requireOperador(session.usuarioId(), session.empresaId());
    }

    public void requireGestao(EmbedSession session) {
        membershipService.requireGestao(session.usuarioId(), session.empresaId());
    }
}
