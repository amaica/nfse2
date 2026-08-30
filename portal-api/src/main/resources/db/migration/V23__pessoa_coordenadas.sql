ALTER TABLE pessoa
    ADD COLUMN nome_fantasia VARCHAR(255) NULL AFTER nome,
    ADD COLUMN fone VARCHAR(30) NULL AFTER email,
    ADD COLUMN celular VARCHAR(30) NULL AFTER fone,
    ADD COLUMN complemento VARCHAR(255) NULL AFTER numero,
    ADD COLUMN latitude VARCHAR(20) NULL AFTER codigo_municipio_ibge,
    ADD COLUMN longitude VARCHAR(20) NULL AFTER latitude,
    ADD COLUMN observacoes TEXT NULL AFTER longitude;
