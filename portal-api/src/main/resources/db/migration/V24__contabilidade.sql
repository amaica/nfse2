CREATE TABLE config_contabilidade (
    empresa_id BIGINT NOT NULL PRIMARY KEY,
    email_contabilidade VARCHAR(255) NULL,
    envio_automatico BOOLEAN NOT NULL DEFAULT FALSE,
    enviar_nfse BOOLEAN NOT NULL DEFAULT TRUE,
    enviar_nfe BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contab_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE
);
