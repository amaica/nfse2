ALTER TABLE empresa
    ADD COLUMN baixar_xml TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN ultimo_nsu VARCHAR(15) NULL,
    ADD COLUMN ultimo_nsu_baixado_em DATETIME NULL;

CREATE TABLE nfe_entrada (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    chave VARCHAR(44) NOT NULL,
    nsu VARCHAR(15),
    schema_xml VARCHAR(80),
    cnpj_emitente VARCHAR(14),
    nome_emitente VARCHAR(255),
    numero VARCHAR(20),
    serie VARCHAR(10),
    data_emissao DATE,
    natureza VARCHAR(255),
    valor DECIMAL(19,2),
    xml LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_nfe_entrada_empresa_chave UNIQUE (empresa_id, chave),
    CONSTRAINT fk_nfe_entrada_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE
);

CREATE INDEX idx_nfe_entrada_empresa_data ON nfe_entrada (empresa_id, data_emissao);
