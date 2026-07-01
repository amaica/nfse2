CREATE TABLE nfse_pdf_email_pendente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    chave_acesso VARCHAR(50) NOT NULL,
    destinatario VARCHAR(255) NOT NULL,
    mensagem TEXT NULL,
    tentativas INT NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pdf_email_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT chk_pdf_email_status CHECK (status IN ('PENDENTE', 'ENVIADO', 'EXPIRADO'))
);

CREATE INDEX idx_pdf_email_pendente_retry ON nfse_pdf_email_pendente (status, proxima_tentativa_em);
CREATE UNIQUE INDEX uk_pdf_email_pendente_ativo ON nfse_pdf_email_pendente (empresa_id, chave_acesso, destinatario, status);
