-- Fase 3 SaaS: assinatura Stripe + uso mensal (cotas)

ALTER TABLE conta
    ADD COLUMN stripe_customer_id VARCHAR(64) NULL;

CREATE TABLE assinatura (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_id                 BIGINT NOT NULL,
    stripe_subscription_id   VARCHAR(64) NULL,
    status                   VARCHAR(32) NOT NULL DEFAULT 'trial',
    plano_codigo             VARCHAR(32) NOT NULL DEFAULT 'starter',
    pacotes                  INT NOT NULL DEFAULT 1,
    empresas_quota           INT NOT NULL DEFAULT 1,
    usuarios_quota           INT NOT NULL DEFAULT 5,
    nfse_mes_quota           INT NOT NULL DEFAULT 100,
    nfe_mes_quota            INT NOT NULL DEFAULT 50,
    periodo_fim              TIMESTAMP NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_assinatura_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT uk_assinatura_conta UNIQUE (conta_id)
);

CREATE TABLE uso_mensal (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_id    BIGINT NOT NULL,
    ano_mes     CHAR(7) NOT NULL,
    nfse_count  INT NOT NULL DEFAULT 0,
    nfe_count   INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_uso_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT uk_uso_conta_mes UNIQUE (conta_id, ano_mes)
);

-- Contas legadas: assinatura ativa generosa (sem cobrança até configurar Stripe)
INSERT INTO assinatura (conta_id, status, pacotes, empresas_quota, usuarios_quota, nfse_mes_quota, nfe_mes_quota)
SELECT id, 'ativa', 10, 10, 50, 1000, 500 FROM conta;
