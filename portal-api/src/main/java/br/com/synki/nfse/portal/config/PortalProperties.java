package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nfse.portal")
public record PortalProperties(
        String jwtSecret,
        int jwtExpirationMinutes,
        int refreshExpirationDays,
        String corsOrigins,
        String adminSecret,
        String embedBaseUrl
) {
}
