INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id)
SELECT 34, 'Registrar evento', 'file-edit', '/nfe/registrar-evento', 5, 1, 'SIM', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM portal_menu WHERE id = 34 OR outcome = '/nfe/registrar-evento');

INSERT IGNORE INTO portal_perfil_menu (perfil_id, menu_id)
SELECT DISTINCT pm.perfil_id, 34
FROM portal_perfil_menu pm
WHERE pm.menu_id = 9
  AND EXISTS (SELECT 1 FROM portal_menu WHERE id = 34);
