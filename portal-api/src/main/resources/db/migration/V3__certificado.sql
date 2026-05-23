CREATE TABLE certificado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    arquivo LONGBLOB NOT NULL,
    senha VARCHAR(255) NOT NULL,
    validade DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_certificado_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_certificado_empresa ON certificado (empresa_id);
