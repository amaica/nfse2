-- MySQL: CREATE INDEX IF NOT EXISTS não é suportado em todas as versões
CREATE INDEX idx_empresa_cnpj_ativo ON empresa (cnpj, ativo);
