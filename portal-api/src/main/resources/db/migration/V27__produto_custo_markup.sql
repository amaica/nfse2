ALTER TABLE produto
    ADD COLUMN valor_custo DECIMAL(15, 4) NULL AFTER valor_unitario,
    ADD COLUMN markup DECIMAL(7, 2) NULL AFTER valor_custo;
