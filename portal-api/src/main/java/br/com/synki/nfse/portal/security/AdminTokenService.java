package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.config.PortalProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Emite e valida um token de sessao admin de curta duracao, para nao manter o
 * NFSE_ADMIN_SECRET (segredo mestre) guardado no navegador apos o login.
 */
@Service
public class AdminTokenService {

    private static final int TOKEN_TTL_MINUTES = 240;
    private static final String MARKER = "ADMIN";

    private final PortalProperties properties;

    public AdminTokenService(PortalProperties properties) {
        this.properties = properties;
    }

    public boolean secretValido(String secret) {
        var esperado = properties.adminSecret();
        if (esperado == null || esperado.isBlank() || secret == null) {
            return false;
        }
        return constantTimeEquals(esperado, secret);
    }

    public String createToken() {
        long exp = Instant.now().getEpochSecond() + TOKEN_TTL_MINUTES * 60L;
        String payload = MARKER + "|" + exp;
        String sig = sign(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + "|" + sig).getBytes(StandardCharsets.UTF_8));
    }

    public boolean validar(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");
            if (parts.length != 3 || !MARKER.equals(parts[0])) {
                return false;
            }
            long exp = Long.parseLong(parts[1]);
            String payload = parts[0] + "|" + parts[1];
            if (!constantTimeEquals(sign(payload), parts[2])) {
                return false;
            }
            return Instant.now().getEpochSecond() <= exp;
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
