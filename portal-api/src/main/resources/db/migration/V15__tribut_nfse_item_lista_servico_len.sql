-- LC 116 usa formato 00.00.00.000 (12 caracteres)
ALTER TABLE tribut_nfse_servico
    MODIFY item_lista_servico VARCHAR(15) NOT NULL;
