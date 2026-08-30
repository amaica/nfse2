package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.service.MembershipService;
import br.com.synki.nfse.portal.service.OnboardingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Garante que o token Bearer referencia uma empresa à qual o usuário tem membership. */
@Component
public class MembershipAuthFilter extends OncePerRequestFilter {

    private final MembershipService membershipService;

    public MembershipAuthFilter(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return path.startsWith("/api/admin/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/public/")
                || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmbedSession session) {
            if (session.empresaId() == OnboardingService.EMPRESA_ONBOARDING) {
                if (!request.getRequestURI().startsWith("/api/onboarding/")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"erro\":\"Conclua o onboarding antes de acessar o portal\"}");
                    return;
                }
            } else if (!membershipService.hasAccess(session.usuarioId(), session.empresaId())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\":\"Sem permissao para a empresa da sessao\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
