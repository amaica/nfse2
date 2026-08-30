package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.PortalProperties;
import br.com.synki.nfse.portal.domain.RefreshToken;
import br.com.synki.nfse.portal.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final PortalProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, PortalProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public String emitir(Long usuarioId) {
        int dias = properties.refreshExpirationDays();
        if (dias <= 0) {
            return null;
        }
        String raw = gerarToken();
        repository.save(RefreshToken.of(
                usuarioId,
                hash(raw),
                Instant.now().plusSeconds(dias * 86400L)));
        return raw;
    }

    @Transactional
    public Long validarERevogar(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token obrigatorio");
        }
        var stored = repository.findByTokenHashAndRevogadoFalse(hash(rawToken.trim()))
                .filter(RefreshToken::isValido)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token invalido ou expirado"));
        stored.revogar();
        repository.save(stored);
        return stored.getUsuarioId();
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
