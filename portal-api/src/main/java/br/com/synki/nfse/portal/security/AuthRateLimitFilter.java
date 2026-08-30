package br.com.synki.nfse.portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Limite simples por IP em login, registro e consulta CNPJ publica. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_POR_JANELA = 30;
    private static final long JANELA_MS = 60_000L;

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod()) && !HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        var path = request.getRequestURI();
        return !path.startsWith("/api/auth/login")
                && !path.startsWith("/api/auth/register")
                && !path.startsWith("/api/public/cnpj/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var key = clientIp(request) + "|" + request.getRequestURI();
        var now = System.currentTimeMillis();
        var window = buckets.compute(key, (k, w) -> {
            if (w == null || now - w.startMs > JANELA_MS) {
                return new Window(now);
            }
            return w;
        });
        if (window.count.incrementAndGet() > MAX_POR_JANELA) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\":\"Muitas tentativas — aguarde um minuto\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long startMs;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMs) {
            this.startMs = startMs;
        }
    }
}
