CREATE TABLE empresa_endereco (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    apelido VARCHAR(80) NOT NULL,
    cep VARCHAR(8),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(120),
    municipio VARCHAR(120),
    uf VARCHAR(2),
    codigo_municipio_ibge VARCHAR(7),
    inscricao_estadual VARCHAR(20),
    serie_nfe VARCHAR(10) NOT NULL DEFAULT '1',
    ultimo_numero_nfe BIGINT NOT NULL DEFAULT 0,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_endereco_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_endereco_empresa ON empresa_endereco (empresa_id);

INSERT INTO empresa_endereco (
    empresa_id, apelido, cep, logradouro, numero, complemento, bairro,
    municipio, uf, inscricao_estadual, principal, ativo
)
SELECT
    id, 'Matriz', cep, logradouro, numero, complemento, bairro,
    municipio, uf, inscricao_estadual, TRUE, TRUE
FROM empresa
WHERE COALESCE(cep, logradouro, municipio, '') <> '';
