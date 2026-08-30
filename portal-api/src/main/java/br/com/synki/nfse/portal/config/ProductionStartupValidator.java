package br.com.synki.nfse.portal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/** Falha rápido em produção se segredos padrão ainda estiverem configurados. */
@Component
@Profile("prod")
public class ProductionStartupValidator implements ApplicationRunner {

    private static final Set<String> JWT_SECRETS_FRACOS = Set.of(
            "change-me-in-production-use-long-random-string",
            "change-me",
            "secret");

    private static final Set<String> ADMIN_SECRETS_FRACOS = Set.of(
            "admin-change-me",
            "admin",
            "change-me");

    private final Environment env;

    public ProductionStartupValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            return;
        }
        var jwt = env.getProperty("nfse.portal.jwt-secret", "");
        var admin = env.getProperty("nfse.portal.admin-secret", "");
        if (jwt.isBlank() || JWT_SECRETS_FRACOS.contains(jwt) || jwt.length() < 32) {
            throw new IllegalStateException(
                    "Producao: defina NFSE_JWT_SECRET com pelo menos 32 caracteres aleatorios");
        }
        if (admin.isBlank() || ADMIN_SECRETS_FRACOS.contains(admin) || admin.length() < 16) {
            throw new IllegalStateException(
                    "Producao: defina NFSE_ADMIN_SECRET com pelo menos 16 caracteres aleatorios");
        }
        var expMin = env.getProperty("nfse.portal.jwt-expiration-minutes", "0");
        if ("0".equals(expMin.trim())) {
            throw new IllegalStateException(
                    "Producao: defina NFSE_JWT_EXP_MIN > 0 (ex.: 480) — tokens permanentes nao sao permitidos");
        }
        validarStripeSeHabilitado();
    }

    private void validarStripeSeHabilitado() {
        var enabled = "true".equalsIgnoreCase(env.getProperty("nfse.portal.stripe.enabled", "false"));
        if (!enabled) {
            return;
        }
        var stripeEnv = env.getProperty("nfse.portal.stripe.env", "test");
        var isLive = "live".equalsIgnoreCase(stripeEnv);
        var secret = isLive
                ? env.getProperty("nfse.portal.stripe.live-secret-key", "")
                : env.getProperty("nfse.portal.stripe.test-secret-key", "");
        var price = isLive
                ? env.getProperty("nfse.portal.stripe.live-price-id", "")
                : env.getProperty("nfse.portal.stripe.test-price-id", "");
        var webhook = env.getProperty("nfse.portal.stripe.webhook-secret", "");
        if (secret.isBlank() || !secret.startsWith(isLive ? "sk_live_" : "sk_test_")) {
            throw new IllegalStateException(
                    "Producao + Stripe: defina " + (isLive ? "STRIPE_LIVE_SECRET_KEY" : "STRIPE_TEST_SECRET_KEY"));
        }
        if (price.isBlank() || !price.startsWith("price_")) {
            throw new IllegalStateException(
                    "Producao + Stripe: defina " + (isLive ? "STRIPE_LIVE_PRICE_ID" : "STRIPE_TEST_PRICE_ID"));
        }
        if (webhook.isBlank() || !webhook.startsWith("whsec_")) {
            throw new IllegalStateException("Producao + Stripe: defina STRIPE_WEBHOOK_SECRET (whsec_...)");
        }
    }
}
