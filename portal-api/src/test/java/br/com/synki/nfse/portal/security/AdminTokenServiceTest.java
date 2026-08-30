package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.config.PortalProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTokenServiceTest {

    private final PortalProperties props = new PortalProperties(
            "jwt-secret-de-teste-bem-longo-1234567890",
            0,
            30,
            "http://localhost:3000",
            "admin-secret-correto",
            "http://localhost:3000");

    private final AdminTokenService service = new AdminTokenService(props);

    @Test
    void secretValidoAceitaSegredoCorreto() {
        assertThat(service.secretValido("admin-secret-correto")).isTrue();
    }

    @Test
    void secretValidoRejeitaSegredoErrado() {
        assertThat(service.secretValido("outro-valor")).isFalse();
    }

    @Test
    void secretValidoRejeitaNulo() {
        assertThat(service.secretValido(null)).isFalse();
    }

    @Test
    void tokenCriadoEValidoImediatamente() {
        var token = service.createToken();
        assertThat(service.validar(token)).isTrue();
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        var token = service.createToken();
        var adulterado = token.substring(0, token.length() - 2) + "xx";
        assertThat(service.validar(adulterado)).isFalse();
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRejeitado() {
        var outroService = new AdminTokenService(new PortalProperties(
                "segredo-jwt-diferente-987654321",
                0, 30, "http://localhost:3000", "admin-secret-correto", "http://localhost:3000"));
        var tokenDeOutraChave = outroService.createToken();
        assertThat(service.validar(tokenDeOutraChave)).isFalse();
    }

    @Test
    void tokenVazioOuNuloEhInvalido() {
        assertThat(service.validar(null)).isFalse();
        assertThat(service.validar("")).isFalse();
        assertThat(service.validar("lixo-qualquer")).isFalse();
    }
}
