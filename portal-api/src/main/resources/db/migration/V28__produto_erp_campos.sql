ALTER TABLE produto
    ADD COLUMN descricao_pdv VARCHAR(120) NULL AFTER nome,
    ADD COLUMN cest VARCHAR(7) NULL AFTER codigo_ncm,
    ADD COLUMN ex_tipi VARCHAR(3) NULL AFTER cest,
    ADD COLUMN origem CHAR(1) NULL DEFAULT '0' AFTER unidade,
    ADD COLUMN tipo CHAR(1) NOT NULL DEFAULT 'P' AFTER origem,
    ADD COLUMN peso DECIMAL(15, 4) NULL AFTER markup,
    ADD COLUMN estoque_minimo DECIMAL(15, 4) NULL AFTER peso,
    ADD COLUMN estoque_atual DECIMAL(15, 4) NULL AFTER estoque_minimo,
    ADD COLUMN observacoes VARCHAR(500) NULL AFTER estoque_atual;
