-- Endereços adicionais do cliente/pessoa (estilo Fluxo PESSOA_ENDERECO)
CREATE TABLE pessoa_endereco (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pessoa_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    inscricao_estadual VARCHAR(20) NULL,
    logradouro VARCHAR(120) NULL,
    numero VARCHAR(20) NULL,
    complemento VARCHAR(60) NULL,
    bairro VARCHAR(60) NULL,
    municipio VARCHAR(60) NULL,
    uf VARCHAR(2) NULL,
    cep VARCHAR(8) NULL,
    codigo_municipio_ibge VARCHAR(7) NULL,
    principal TINYINT(1) NOT NULL DEFAULT 0,
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pessoa_endereco_pessoa FOREIGN KEY (pessoa_id) REFERENCES pessoa (id) ON DELETE CASCADE,
    INDEX idx_pessoa_endereco_pessoa (pessoa_id),
    INDEX idx_pessoa_endereco_empresa (empresa_id)
);
