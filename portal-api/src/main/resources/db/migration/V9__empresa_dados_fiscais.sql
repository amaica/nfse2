ALTER TABLE empresa ADD COLUMN nome_fantasia VARCHAR(255);
ALTER TABLE empresa ADD COLUMN email VARCHAR(255);
ALTER TABLE empresa ADD COLUMN telefone VARCHAR(30);
ALTER TABLE empresa ADD COLUMN inscricao_estadual VARCHAR(20);
ALTER TABLE empresa ADD COLUMN inscricao_municipal VARCHAR(20);
ALTER TABLE empresa ADD COLUMN cep VARCHAR(8);
ALTER TABLE empresa ADD COLUMN logradouro VARCHAR(255);
ALTER TABLE empresa ADD COLUMN numero VARCHAR(20);
ALTER TABLE empresa ADD COLUMN complemento VARCHAR(100);
ALTER TABLE empresa ADD COLUMN bairro VARCHAR(120);
ALTER TABLE empresa ADD COLUMN municipio VARCHAR(120);
ALTER TABLE empresa ADD COLUMN uf VARCHAR(2);
ALTER TABLE empresa ADD COLUMN cnae_principal VARCHAR(7);
ALTER TABLE empresa ADD COLUMN cnae_principal_descricao VARCHAR(255);
ALTER TABLE empresa ADD COLUMN optante_simples BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE empresa ADD COLUMN situacao_cadastral VARCHAR(40);

ALTER TABLE configuracao_nfse ADD COLUMN serie_rps VARCHAR(10) NOT NULL DEFAULT '1';
ALTER TABLE configuracao_nfse ADD COLUMN ultimo_numero_nfse BIGINT NOT NULL DEFAULT 0;

CREATE TABLE configuracao_documento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    serie VARCHAR(10) NOT NULL DEFAULT '1',
    ultimo_numero BIGINT NOT NULL DEFAULT 0,
    habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uk_doc_empresa_tipo UNIQUE (empresa_id, tipo)
);
