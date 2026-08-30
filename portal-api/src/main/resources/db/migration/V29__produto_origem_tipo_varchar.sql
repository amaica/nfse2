-- Hibernate valida String como VARCHAR; CHAR(1) do V28 quebra o boot.
ALTER TABLE produto
    MODIFY COLUMN origem VARCHAR(1) NULL DEFAULT '0',
    MODIFY COLUMN tipo VARCHAR(1) NOT NULL DEFAULT 'P';
