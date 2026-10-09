ALTER TABLE nfe_emissao
    ADD COLUMN valor_total DECIMAL(15, 2) NULL,
    ADD COLUMN destinatario_nome VARCHAR(255) NULL,
    ADD COLUMN cfop VARCHAR(60) NULL,
    ADD COLUMN tipo_operacao VARCHAR(20) NULL,
    ADD COLUMN produtos_resumo TEXT NULL;
