-- Reforma Tributária: Imposto Seletivo (IS) na operação fiscal
ALTER TABLE tribut_operacao_fiscal
    ADD COLUMN habilitar_is TINYINT(1) NOT NULL DEFAULT 0 AFTER habilitar_ibs_cbs,
    ADD COLUMN is_cst VARCHAR(3) NULL AFTER habilitar_is,
    ADD COLUMN is_class_trib VARCHAR(6) NULL AFTER is_cst,
    ADD COLUMN aliquota_is DECIMAL(7,4) NULL AFTER is_class_trib;
