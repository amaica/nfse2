CREATE TABLE configuracao_nfse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    prefeitura VARCHAR(120) NULL,
    codigo_municipio_ibge VARCHAR(7) NOT NULL,
    ambiente VARCHAR(20) NOT NULL DEFAULT 'producao',
    token_integracao VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uk_config_empresa UNIQUE (empresa_id)
);
