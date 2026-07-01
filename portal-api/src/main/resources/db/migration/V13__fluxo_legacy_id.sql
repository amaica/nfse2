ALTER TABLE empresa
    ADD COLUMN fluxo_legacy_id INT UNSIGNED NULL;

CREATE UNIQUE INDEX uk_empresa_fluxo_legacy ON empresa (fluxo_legacy_id);
