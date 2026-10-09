ALTER TABLE nfe_emissao
    ADD COLUMN ambiente CHAR(1) NOT NULL DEFAULT '1';

UPDATE nfe_emissao
   SET ambiente = '2'
 WHERE xml_proc LIKE '%<tpAmb>2</tpAmb>%';

CREATE INDEX idx_nfe_emissao_empresa_ambiente ON nfe_emissao (empresa_id, ambiente, created_at);
