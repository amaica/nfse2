package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PortalProperties.class)
public class AppConfig {
}
