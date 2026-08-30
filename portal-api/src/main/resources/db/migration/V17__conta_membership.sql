-- Fase 1 SaaS: conta (tenant), vínculo empresa↔conta, membership usuário↔empresa

CREATE TABLE conta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    owner_usuario_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ativa',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_conta_owner FOREIGN KEY (owner_usuario_id) REFERENCES usuario (id)
);

CREATE TABLE conta_empresa (
    conta_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    PRIMARY KEY (conta_id, empresa_id),
    CONSTRAINT fk_ce_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT fk_ce_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uk_ce_empresa UNIQUE (empresa_id)
);

CREATE TABLE usuario_empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    conta_id BIGINT NOT NULL,
    papel VARCHAR(32) NOT NULL DEFAULT 'OPERADOR',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ue_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_ue_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_ue_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT uk_usuario_empresa UNIQUE (usuario_id, empresa_id)
);

CREATE INDEX idx_ue_usuario ON usuario_empresa (usuario_id);
CREATE INDEX idx_ue_empresa ON usuario_empresa (empresa_id);
CREATE INDEX idx_ue_conta ON usuario_empresa (conta_id);

-- Migra empresas existentes: 1 conta por empresa
INSERT INTO conta (nome, owner_usuario_id, status)
SELECT CONCAT('Conta #', e.id, ' — ', e.nome),
       (SELECT MIN(u.id) FROM usuario u WHERE u.empresa_id = e.id AND u.ativo = TRUE),
       'ativa'
FROM empresa e;

INSERT INTO conta_empresa (conta_id, empresa_id)
SELECT c.id, e.id
FROM empresa e
JOIN conta c ON c.nome = CONCAT('Conta #', e.id, ' — ', e.nome);

-- Membership: usuário vinculado à sua empresa; primeiro usuário = OWNER
INSERT INTO usuario_empresa (usuario_id, empresa_id, conta_id, papel, ativo)
SELECT u.id,
       u.empresa_id,
       ce.conta_id,
       CASE
           WHEN u.id = (
               SELECT MIN(u2.id)
               FROM usuario u2
               WHERE u2.empresa_id = u.empresa_id AND u2.ativo = TRUE
           ) THEN 'OWNER'
           ELSE COALESCE(NULLIF(u.perfil, ''), 'OPERADOR')
       END,
       u.ativo
FROM usuario u
JOIN conta_empresa ce ON ce.empresa_id = u.empresa_id;
