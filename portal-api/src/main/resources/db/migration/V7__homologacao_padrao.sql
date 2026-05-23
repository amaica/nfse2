-- Ambiente de homologação (produção restrita) como padrão da empresa demo
UPDATE configuracao_nfse
SET ambiente = 'homologacao'
WHERE empresa_id = 1 AND ambiente = 'producao';
