package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nfse.portal.stripe")
public record StripeProperties(
        boolean enabled,
        String env,
        String testSecretKey,
        String testPriceId,
        String liveSecretKey,
        String livePriceId,
        String webhookSecret,
        String portalReturnUrl
) {
    public boolean isLive() {
        return "live".equalsIgnoreCase(env);
    }

    public String secretKey() {
        return isLive() ? liveSecretKey : testSecretKey;
    }

    public String priceId() {
        return isLive() ? livePriceId : testPriceId;
    }
}
