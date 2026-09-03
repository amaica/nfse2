-- Menu dinâmico do portal (esquema alinhado ao agrowFront /parametros/configurar-menu)

CREATE TABLE portal_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    label VARCHAR(120) NOT NULL,
    icon VARCHAR(80) NULL,
    outcome VARCHAR(500) NULL,
    ordem_menu INT NOT NULL DEFAULT 0,
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    operador_tem_acesso VARCHAR(3) NOT NULL DEFAULT 'SIM',
    parent_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portal_menu_parent FOREIGN KEY (parent_id) REFERENCES portal_menu (id) ON DELETE SET NULL,
    INDEX idx_portal_menu_parent (parent_id),
    INDEX idx_portal_menu_ordem (ordem_menu)
);

CREATE TABLE portal_submenu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    label VARCHAR(120) NOT NULL,
    icon VARCHAR(80) NULL,
    outcome VARCHAR(500) NULL,
    CONSTRAINT fk_portal_submenu_menu FOREIGN KEY (menu_id) REFERENCES portal_menu (id) ON DELETE CASCADE,
    INDEX idx_portal_submenu_menu (menu_id)
);

-- Seed: estrutura atual do menu estático (ADMIN), com itens de gestão marcados NAO para operador
INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id) VALUES
(1,  'Início',      'home',     '/painel', 10, 1, 'SIM', NULL),
(2,  'NF-e',         'file',     NULL,      20, 1, 'SIM', NULL),
(3,  'NFS-e',        'receipt',  NULL,      30, 1, 'NAO', NULL),
(4,  'Cadastros',    'database', NULL,      40, 1, 'SIM', NULL),
(5,  'Tributação',   'scale',    NULL,      50, 1, 'SIM', NULL),
(6,  'Conta',        'settings', NULL,      60, 1, 'NAO', NULL),
(7,  'Emitir NF-e',           'pencil',     '/nfe/emissao',           1, 1, 'SIM', 2),
(8,  'NF-e — XMLs / DANFE',   'list',       '/nfe/notas-emitidas',    2, 1, 'SIM', 2),
(9,  'Eventos da NF-e',       'file-edit',  '/nfe/eventos-fiscais',   3, 1, 'SIM', 2),
(10, 'Emitir NFS-e',          'receipt',    '/nfse/emissao',          1, 1, 'NAO', 3),
(11, 'NFS-e mensais',         'calendar',   '/nfse/mensais',          2, 1, 'NAO', 3),
(12, 'NFS-e emitidas',        'list',       '/nfse/emitidas',         3, 1, 'NAO', 3),
(13, 'Clientes',              'users',      '/cadastros/pessoas',     1, 1, 'SIM', 4),
(14, 'Produtos',              'box',        '/cadastros/produtos',    2, 1, 'SIM', 4),
(15, 'Serviços (NFS-e)',      'cog',        '/tributacao/nfse-servico', 3, 1, 'SIM', 4),
(16, 'Veículos',              'car',        '/cadastros/veiculos',    4, 1, 'SIM', 4),
(17, 'Usuários',              'user-plus',  '/cadastros/usuarios',    5, 1, 'NAO', 4),
(18, 'Grupos tributários',    'sitemap',    '/tributacao/grupo-tributario', 1, 1, 'SIM', 5),
(19, 'Operações fiscais',     'briefcase',  '/tributacao/operacao-fiscal',  2, 1, 'SIM', 5),
(20, 'ICMS por operação × grupo', 'settings', '/tributacao/configura-of-gt', 3, 1, 'SIM', 5),
(21, 'Tributação NFS-e',      'receipt',    '/tributacao/nfse-servico', 4, 1, 'NAO', 5),
(22, 'Emitentes',             'building',   '/cadastros/empresa',     1, 1, 'NAO', 6),
(23, 'Integração ERP',        'plug',       '/conta/integracao',      2, 1, 'NAO', 6),
(24, 'Livro Caixa + LCDPR',   'book',       '/conta/contabilidade',   3, 1, 'NAO', 6),
(25, 'Assinatura',            'credit-card','/conta/assinatura',      4, 1, 'NAO', 6),
(26, 'LGPD e segurança',      'shield',     '/conta/lgpd',            5, 1, 'NAO', 6),
(27, 'Auditoria',             'clipboard-list', '/conta/auditoria',   6, 1, 'NAO', 6),
(28, 'Métricas de uso',       'chart-bar',  '/conta/metricas',        7, 1, 'NAO', 6),
(29, 'Configurar Menu',       'list',       '/parametros/configurar-menu', 8, 1, 'NAO', 6),
(30, 'Emitente',              'building',   NULL,                      55, 1, 'SIM', NULL),
(31, 'Dados do emitente',     'settings',   '/cadastros/empresa',      1, 1, 'SIM', 30);

ALTER TABLE portal_menu AUTO_INCREMENT = 100;
