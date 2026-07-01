package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nfse.pdf-retry")
public record NfsePdfRetryProperties(
        boolean enabled,
        long intervalMs,
        int maxTentativas,
        int lote
) {
    public NfsePdfRetryProperties {
        if (intervalMs <= 0) {
            intervalMs = 60_000;
        }
        if (maxTentativas <= 0) {
            maxTentativas = 72;
        }
        if (lote <= 0) {
            lote = 30;
        }
    }
}
