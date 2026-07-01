CREATE TABLE nfe_emissao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    chave VARCHAR(44) NOT NULL,
    serie VARCHAR(10),
    numero BIGINT NOT NULL,
    modelo VARCHAR(3) NOT NULL,
    status_protocolo VARCHAR(5),
    motivo_protocolo VARCHAR(500),
    xml_proc LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nfe_emissao_empresa_chave ON nfe_emissao (empresa_id, chave);
