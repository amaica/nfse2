CREATE TABLE nfse_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NULL,
    acao VARCHAR(80) NOT NULL,
    descricao TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_log_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_nfse_log_empresa ON nfse_log (empresa_id, created_at DESC);
