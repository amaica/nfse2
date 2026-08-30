CREATE TABLE produto_grupo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_produto_grupo_emp_nome (empresa_id, nome),
    KEY idx_produto_grupo_emp (empresa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE produto_subgrupo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    produto_grupo_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_produto_sub_emp_grp_nome (empresa_id, produto_grupo_id, nome),
    KEY idx_produto_sub_grupo (produto_grupo_id),
    CONSTRAINT fk_produto_sub_grupo FOREIGN KEY (produto_grupo_id) REFERENCES produto_grupo (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE produto
    ADD COLUMN grupo_id BIGINT NULL AFTER grupo_tributario_id,
    ADD COLUMN subgrupo_id BIGINT NULL AFTER grupo_id;

ALTER TABLE produto
    ADD CONSTRAINT fk_produto_grupo FOREIGN KEY (grupo_id) REFERENCES produto_grupo (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_produto_subgrupo FOREIGN KEY (subgrupo_id) REFERENCES produto_subgrupo (id) ON DELETE SET NULL;

ALTER TABLE produto MODIFY observacoes TEXT NULL;

CREATE TABLE cest (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(7) NOT NULL,
    descricao VARCHAR(400) NOT NULL,
    ncm_prefixo VARCHAR(8) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cest_codigo (codigo),
    KEY idx_cest_ncm (ncm_prefixo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO cest (codigo, descricao, ncm_prefixo) VALUES
('0100100', 'Soja em grão', '1201'),
('0100200', 'Farelo e resíduos da extração de óleo de soja', '2304'),
('0100300', 'Óleo de soja', '1507'),
('0100700', 'Milho em grão', '1005'),
('0100800', 'Trigo em grão', '1001'),
('0100900', 'Arroz', '1006'),
('0101000', 'Feijão', '0713'),
('0101100', 'Cevada', '1003'),
('0101200', 'Aveia', '1004'),
('0101300', 'Sorgo', '1007'),
('0102500', 'Leite', '0401'),
('0102600', 'Leite em pó', '0402'),
('0102800', 'Queijos', '0406'),
('0103100', 'Ovos de aves', '0407'),
('0104300', 'Animais vivos da espécie bovina', '0102'),
('0105600', 'Animais vivos da espécie suína', '0103'),
('0105900', 'Aves (gallus domesticus, patos, gansos, perus, galinhas-d''angola)', '0105'),
('0106100', 'Carnes de bovinos', '0201'),
('0106400', 'Carnes de suínos', '0203'),
('0106700', 'Carnes e miudezas de aves', '0207'),
('1700100', 'Rações para animais', '2309'),
('1700300', 'Preparações para alimentação animal (premix/núcleo)', '2309'),
('2000100', 'Adubos ou fertilizantes', '3102'),
('2000200', 'Adubos ou fertilizantes (outros)', '3105'),
('2000300', 'Inseticidas, fungicidas, herbicidas e afins (defensivos)', '3808'),
('0102100', 'Sementes de soja', '1201'),
('0102200', 'Sementes de milho', '1005'),
('2800100', 'Álcool etílico', '2207'),
('0600100', 'Cimento', '2523'),
('1700500', 'Sal mineral / suplemento para gado', '2309'),
('0101500', 'Farinhas de trigo', '1101'),
('0101700', 'Farelo de trigo', '2302'),
('0800100', 'Óleo diesel', '2710'),
('0800200', 'Gasolina', '2710'),
('0800500', 'Óleos lubrificantes', '2710'),
('2100100', 'Partes e peças para tratores e máquinas agrícolas', '8432'),
('2100200', 'Máquinas e aparelhos para agricultura', '8433');
