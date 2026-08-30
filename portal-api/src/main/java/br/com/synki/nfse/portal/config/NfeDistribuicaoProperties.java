package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nfse.nfe.distribuicao")
public record NfeDistribuicaoProperties(
        boolean enabled,
        String cron,
        int maxPaginas
) {
    public NfeDistribuicaoProperties {
        if (cron == null || cron.isBlank()) {
            cron = "0 20 * * * *";
        }
        if (maxPaginas <= 0) {
            maxPaginas = 20;
        }
    }
}
