-- Fase 2 SaaS: convites, refresh tokens, vínculo multi-empresa na mesma conta

CREATE TABLE usuario_convite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    papel VARCHAR(32) NOT NULL DEFAULT 'OPERADOR',
    token VARCHAR(64) NOT NULL,
    criado_por_usuario_id BIGINT NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    aceito_em TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_convite_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT fk_convite_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_convite_criador FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario (id),
    CONSTRAINT uk_convite_token UNIQUE (token)
);

CREATE INDEX idx_convite_email ON usuario_convite (email);
CREATE INDEX idx_convite_conta ON usuario_convite (conta_id);

CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    revogado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT uk_refresh_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_usuario ON refresh_token (usuario_id);
