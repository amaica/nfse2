package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Rejeita tokens de usuarios desativados ou inexistentes. */
@Component
public class UsuarioActiveAuthFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActiveAuthFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmbedSession session) {
            var user = usuarioRepository.findById(session.usuarioId()).orElse(null);
            if (user == null || !user.isAtivo()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\":\"Usuario inativo ou nao encontrado\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
