-- Fase 5 SaaS: audit log por conta + controle de avisos de cobrança

CREATE TABLE audit_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_id    BIGINT NOT NULL,
    empresa_id  BIGINT NULL,
    usuario_id  BIGINT NULL,
    acao        VARCHAR(80) NOT NULL,
    recurso     VARCHAR(120) NULL,
    detalhe     TEXT NULL,
    ip          VARCHAR(45) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_conta FOREIGN KEY (conta_id) REFERENCES conta (id)
);

CREATE INDEX idx_audit_conta_created ON audit_event (conta_id, created_at DESC);
CREATE INDEX idx_audit_empresa ON audit_event (empresa_id);

CREATE TABLE dunning_aviso (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_id    BIGINT NOT NULL,
    tipo        VARCHAR(32) NOT NULL,
    referencia  VARCHAR(64) NOT NULL,
    enviado_em  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dunning_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT uk_dunning_conta_tipo_ref UNIQUE (conta_id, tipo, referencia)
);
