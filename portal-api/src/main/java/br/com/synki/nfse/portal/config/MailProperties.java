package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nfse.mail")
public record MailProperties(
        boolean enabled,
        String from,
        String fromName
) {
    public MailProperties {
        if (fromName == null || fromName.isBlank()) {
            fromName = "Synki NFS-e";
        }
    }
}
