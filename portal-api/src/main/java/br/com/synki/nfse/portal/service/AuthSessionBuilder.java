package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.security.EmbedTokenService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Monta resposta de login/troca empresa/refresh com tokens e metadados da sessao. */
public final class AuthSessionBuilder {

    private AuthSessionBuilder() {}

    public static Builder of(
            Usuario user,
            Long empresaId,
            EmbedTokenService tokenService,
            RefreshTokenService refreshTokenService,
            EmpresaRepository empresaRepository) {
        return new Builder(user, empresaId, tokenService, refreshTokenService, empresaRepository);
    }

    public static final class Builder {
        private final Usuario user;
        private final Long empresaId;
        private final EmbedTokenService tokenService;
        private final RefreshTokenService refreshTokenService;
        private final EmpresaRepository empresaRepository;
        private String papel;
        private Long contaId;

        private Builder(
                Usuario user,
                Long empresaId,
                EmbedTokenService tokenService,
                RefreshTokenService refreshTokenService,
                EmpresaRepository empresaRepository) {
            this.user = user;
            this.empresaId = empresaId;
            this.tokenService = tokenService;
            this.refreshTokenService = refreshTokenService;
            this.empresaRepository = empresaRepository;
        }

        public Builder papel(String papel) {
            this.papel = papel;
            return this;
        }

        public Builder contaId(Long contaId) {
            this.contaId = contaId;
            return this;
        }

        public Map<String, Object> build() {
            var empresa = empresaRepository.findById(empresaId).orElseThrow();
            var body = new LinkedHashMap<String, Object>();
            body.put("token", tokenService.createToken(empresaId, user.getId()));
            var refresh = refreshTokenService.emitir(user.getId());
            if (refresh != null) {
                body.put("refreshToken", refresh);
            }
            body.put("empresaId", empresaId);
            body.put("empresaNome", empresa.getNome());
            body.put("empresaCnpj", empresa.getCnpj());
            body.put("usuarioId", user.getId());
            body.put("nome", user.getNome());
            body.put("email", user.getEmail());
            if (papel != null) {
                body.put("papel", papel);
            }
            if (contaId != null) {
                body.put("contaId", contaId);
            }
            return body;
        }
    }
}
