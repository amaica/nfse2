-- Demo: empresa + config (usuario criado em DemoDataInitializer)
INSERT INTO empresa (nome, cnpj, ativo) VALUES ('Synki Demo', '00000000000191', TRUE);

INSERT INTO configuracao_nfse (empresa_id, prefeitura, codigo_municipio_ibge, ambiente)
VALUES (1, 'Ibiruba/RS', '4310009', 'producao');
