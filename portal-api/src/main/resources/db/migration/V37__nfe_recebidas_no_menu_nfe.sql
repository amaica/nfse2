-- Garante NF-e recebidas (DF-e) como filho do menu NF-e e libera em perfis que já têm NF-e
UPDATE portal_menu
SET parent_id = 2,
    ordem_menu = 4,
    label = 'NF-e recebidas (DF-e)',
    icon = 'download',
    outcome = '/nfe/notas-entrada',
    ativo = 1,
    operador_tem_acesso = 'SIM'
WHERE id = 33;

INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id)
SELECT 33, 'NF-e recebidas (DF-e)', 'download', '/nfe/notas-entrada', 4, 1, 'SIM', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM portal_menu WHERE id = 33);

-- Perfis que já tinham Emitir/Pesquisar/Eventos NF-e passam a ter DF-e também
INSERT IGNORE INTO portal_perfil_menu (perfil_id, menu_id)
SELECT DISTINCT pm.perfil_id, 33
FROM portal_perfil_menu pm
WHERE pm.menu_id IN (2, 7, 8, 9);
