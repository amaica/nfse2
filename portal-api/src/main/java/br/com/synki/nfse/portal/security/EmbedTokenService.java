package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.config.PortalProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Token opaco para iframe: Base64(empresaId|usuarioId|exp|HMAC-SHA256).
 * <p>
 * {@code exp = 0} significa token permanente (padrão para embed por empresa).
 * {@code exp > 0} é epoch Unix de expiração (opcional, se {@code NFSE_JWT_EXP_MIN} &gt; 0).
 */
@Service
public class EmbedTokenService {

    /** Sem expiração — uso em iframe (uma URL/token por empresa). */
    public static final long EXP_NUNCA = 0L;

    private final PortalProperties properties;

    public EmbedTokenService(PortalProperties properties) {
        this.properties = properties;
    }

    public String createToken(Long empresaId, Long usuarioId) {
        long exp = expiracaoEpoch();
        String payload = empresaId + "|" + usuarioId + "|" + exp;
        String sig = sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((payload + "|" + sig).getBytes(StandardCharsets.UTF_8));
    }

    public EmbedSession validate(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Token invalido");
            }
            long empresaId = Long.parseLong(parts[0]);
            long usuarioId = Long.parseLong(parts[1]);
            long exp = Long.parseLong(parts[2]);
            String sig = parts[3];
            String payload = parts[0] + "|" + parts[1] + "|" + parts[2];
            if (!sign(payload).equals(sig)) {
                throw new IllegalArgumentException("Assinatura invalida");
            }
            if (exp != EXP_NUNCA && Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException("Token expirado");
            }
            return new EmbedSession(empresaId, usuarioId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Token invalido ou expirado");
        }
    }

    private long expiracaoEpoch() {
        int minutos = properties.jwtExpirationMinutes();
        if (minutos <= 0) {
            return EXP_NUNCA;
        }
        return Instant.now().getEpochSecond() + minutos * 60L;
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
}
