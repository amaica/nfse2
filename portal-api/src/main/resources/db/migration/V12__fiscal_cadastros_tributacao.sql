-- Cadastros fiscais e tributação (portal) + campos Reforma Tributária IBS/CBS

CREATE TABLE cfop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NULL,
    cfop VARCHAR(4) NOT NULL,
    aplicacao VARCHAR(500) NULL,
    descricao VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cfop_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    INDEX idx_cfop_empresa (empresa_id),
    INDEX idx_cfop_codigo (cfop)
);

CREATE TABLE ncm (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(8) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    observacao VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ncm_codigo UNIQUE (codigo)
);

CREATE TABLE tribut_grupo_tributario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    origem_mercadoria VARCHAR(1) NULL,
    observacao VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gt_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    INDEX idx_gt_empresa (empresa_id)
);

CREATE TABLE tribut_operacao_fiscal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    tipo_operacao VARCHAR(1) NULL,
    gera_financeiro VARCHAR(1) NULL DEFAULT 'S',
    movimenta_estoque VARCHAR(1) NULL DEFAULT 'S',
    descricao_na_nf VARCHAR(255) NULL,
    cfop INT NULL,
    observacao VARCHAR(1000) NULL,
    principal VARCHAR(1) NULL DEFAULT 'N',
    finalidade VARCHAR(1) NULL,
    finalidade_operacao VARCHAR(2) NULL,
    -- Reforma Tributária (identificação NF-e)
    c_mun_fg_ibs VARCHAR(7) NULL,
    tp_nf_debito VARCHAR(2) NULL,
    tp_nf_credito VARCHAR(2) NULL,
    tp_ente_gov VARCHAR(1) NULL,
    p_redutor DECIMAL(15,4) NULL,
    tp_oper_gov VARCHAR(1) NULL,
    ind_intermed VARCHAR(1) NULL DEFAULT '0',
    -- Defaults IBS/CBS por item (alíquota teste 1% — obrigatório a partir de 03/08/2026)
    ibs_cbs_cst VARCHAR(3) NULL DEFAULT '000',
    ibs_cbs_class_trib VARCHAR(6) NULL DEFAULT '000001',
    aliquota_ibs_uf DECIMAL(7,4) NULL DEFAULT 0.0090,
    aliquota_ibs_mun DECIMAL(7,4) NULL DEFAULT 0.0010,
    aliquota_cbs DECIMAL(7,4) NULL DEFAULT 0.0100,
    habilitar_ibs_cbs BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tof_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    INDEX idx_tof_empresa (empresa_id)
);

CREATE TABLE tribut_configura_of_gt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    tribut_operacao_fiscal_id BIGINT NOT NULL,
    tribut_grupo_tributario_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cogt_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    CONSTRAINT fk_cogt_of FOREIGN KEY (tribut_operacao_fiscal_id) REFERENCES tribut_operacao_fiscal (id) ON DELETE CASCADE,
    CONSTRAINT fk_cogt_gt FOREIGN KEY (tribut_grupo_tributario_id) REFERENCES tribut_grupo_tributario (id) ON DELETE CASCADE,
    INDEX idx_cogt_empresa (empresa_id)
);

CREATE TABLE tribut_icms_uf (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    configura_of_gt_id BIGINT NOT NULL,
    uf_destino VARCHAR(2) NOT NULL,
    cfop INT NULL,
    cst VARCHAR(3) NULL,
    csosn VARCHAR(3) NULL,
    aliquota DECIMAL(7,4) NULL,
    origem_mercadoria VARCHAR(1) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_icms_cogt FOREIGN KEY (configura_of_gt_id) REFERENCES tribut_configura_of_gt (id) ON DELETE CASCADE
);

CREATE TABLE pessoa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    tipo VARCHAR(1) NOT NULL DEFAULT 'J',
    cpf_cnpj VARCHAR(14) NULL,
    email VARCHAR(255) NULL,
    inscricao_estadual VARCHAR(20) NULL,
    logradouro VARCHAR(255) NULL,
    numero VARCHAR(20) NULL,
    bairro VARCHAR(100) NULL,
    municipio VARCHAR(100) NULL,
    uf VARCHAR(2) NULL,
    cep VARCHAR(8) NULL,
    codigo_municipio_ibge VARCHAR(7) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pessoa_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    INDEX idx_pessoa_empresa (empresa_id),
    INDEX idx_pessoa_nome (nome)
);

CREATE TABLE produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    gtin VARCHAR(14) NULL,
    codigo_ncm VARCHAR(8) NULL,
    unidade VARCHAR(6) NOT NULL DEFAULT 'UN',
    valor_unitario DECIMAL(15,4) NULL,
    grupo_tributario_id BIGINT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_produto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    CONSTRAINT fk_produto_gt FOREIGN KEY (grupo_tributario_id) REFERENCES tribut_grupo_tributario (id) ON DELETE SET NULL,
    INDEX idx_produto_empresa (empresa_id)
);

CREATE TABLE veiculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    placa VARCHAR(7) NOT NULL,
    modelo VARCHAR(100) NULL,
    marca VARCHAR(100) NULL,
    renavam VARCHAR(20) NULL,
    tipo_rodado VARCHAR(2) NULL,
    tipo_carroceria VARCHAR(2) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_veiculo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    INDEX idx_veiculo_empresa (empresa_id)
);

ALTER TABLE usuario ADD COLUMN cpf VARCHAR(11) NULL;
ALTER TABLE usuario ADD COLUMN perfil VARCHAR(30) NULL DEFAULT 'OPERADOR';

-- CFOP/NCM de referência
INSERT INTO cfop (empresa_id, cfop, aplicacao, descricao) VALUES
    (NULL, '5102', 'Venda', 'Venda de mercadoria adquirida ou recebida de terceiros'),
    (NULL, '5101', 'Venda', 'Venda de producao do estabelecimento'),
    (NULL, '6102', 'Venda interestadual', 'Venda de mercadoria adquirida ou recebida de terceiros'),
    (NULL, '6101', 'Venda interestadual', 'Venda de producao do estabelecimento'),
    (NULL, '5949', 'Outras', 'Outra saida de mercadoria ou prestacao de servico nao especificado'),
    (NULL, '1102', 'Compra', 'Compra para comercializacao');

INSERT INTO ncm (codigo, descricao) VALUES
    ('61091000', 'Camisetas de malha de algodao'),
    ('84713012', 'Computadores portateis'),
    ('22030000', 'Cervejas de malte'),
    ('30049099', 'Medicamentos'),
    ('85171231', 'Telefones celulares');
