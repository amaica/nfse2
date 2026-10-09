CREATE TABLE nfe_evento (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id      BIGINT NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    modelo          VARCHAR(3) NOT NULL,
    chave           VARCHAR(44) NULL,
    serie           VARCHAR(10) NULL,
    numero_inicial  BIGINT NULL,
    numero_final    BIGINT NULL,
    sequencial      INT NULL,
    protocolo       VARCHAR(20) NULL,
    status          VARCHAR(8) NULL,
    motivo          VARCHAR(500) NULL,
    texto           TEXT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nfe_evento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    INDEX idx_nfe_evento_empresa_data (empresa_id, created_at)
);
