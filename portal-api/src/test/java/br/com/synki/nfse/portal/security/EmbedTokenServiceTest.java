package br.com.synki.nfse.portal.security;

import br.com.synki.nfse.portal.config.PortalProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbedTokenServiceTest {

    private static final Long EMPRESA_ID = 10L;
    private static final Long USUARIO_ID = 1L;

    private PortalProperties propsComExpiracao(int minutos) {
        return new PortalProperties(
                "jwt-secret-de-teste-bem-longo-1234567890",
                minutos,
                30,
                "http://localhost:3000",
                "admin-secret",
                "http://localhost:3000");
    }

    @Test
    void tokenSemExpiracaoConfiguradaEhPermanenteEValido() {
        var service = new EmbedTokenService(propsComExpiracao(0));
        var token = service.createToken(EMPRESA_ID, USUARIO_ID);

        var session = service.validate(token);

        assertThat(session.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(session.usuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    void tokenComExpiracaoConfiguradaEhValidoAntesDeExpirar() {
        var service = new EmbedTokenService(propsComExpiracao(60));
        var token = service.createToken(EMPRESA_ID, USUARIO_ID);

        var session = service.validate(token);

        assertThat(session.empresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        var service = new EmbedTokenService(propsComExpiracao(0));
        var token = service.createToken(EMPRESA_ID, USUARIO_ID);
        var adulterado = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> service.validate(adulterado))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRejeitado() {
        var service = new EmbedTokenService(propsComExpiracao(0));
        var outroService = new EmbedTokenService(new PortalProperties(
                "outro-segredo-jwt-completamente-diferente",
                0, 30, "http://localhost:3000", "admin-secret", "http://localhost:3000"));
        var tokenDeOutraChave = outroService.createToken(EMPRESA_ID, USUARIO_ID);

        assertThatThrownBy(() -> service.validate(tokenDeOutraChave))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenComEmpresaOuUsuarioTrocadoEhRejeitado() {
        var service = new EmbedTokenService(propsComExpiracao(0));
        var token = service.createToken(EMPRESA_ID, USUARIO_ID);
        var decoded = new String(java.util.Base64.getUrlDecoder().decode(token), java.nio.charset.StandardCharsets.UTF_8);
        var partes = decoded.split("\\|");
        var empresaTrocada = String.join("|", "999", partes[1], partes[2], partes[3]);
        var tokenTrocado = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(empresaTrocada.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.validate(tokenTrocado))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenExpiradoEhRejeitado() throws Exception {
        var service = new EmbedTokenService(propsComExpiracao(60));
        var signMethod = EmbedTokenService.class.getDeclaredMethod("sign", String.class);
        signMethod.setAccessible(true);

        long expNoPassado = java.time.Instant.now().getEpochSecond() - 3600;
        String payload = EMPRESA_ID + "|" + USUARIO_ID + "|" + expNoPassado;
        String sig = (String) signMethod.invoke(service, payload);
        String tokenExpirado = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + "|" + sig).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.validate(tokenExpirado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void tokenInvalidoOuMalFormadoLancaExcecao() {
        var service = new EmbedTokenService(propsComExpiracao(0));
        assertThatThrownBy(() -> service.validate("nao-e-base64-valido-!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
