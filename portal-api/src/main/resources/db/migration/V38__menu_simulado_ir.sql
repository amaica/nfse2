-- Simulado IR sob Conta; libera em perfis que já têm Livro Caixa
INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id)
SELECT 34, 'Simulado IR', 'calculator', '/conta/simulado-ir', 4, 1, 'NAO', 6
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM portal_menu WHERE id = 34);

-- Empurra Assinatura e demais itens Conta para depois do Simulado IR (ordem 4+)
UPDATE portal_menu SET ordem_menu = 5 WHERE id = 25 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 6 WHERE id = 26 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 7 WHERE id = 27 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 8 WHERE id = 28 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 9 WHERE id = 29 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 10 WHERE id = 32 AND parent_id = 6;

INSERT IGNORE INTO portal_perfil_menu (perfil_id, menu_id)
SELECT DISTINCT pm.perfil_id, 34
FROM portal_perfil_menu pm
WHERE pm.menu_id = 24;

ALTER TABLE portal_menu AUTO_INCREMENT = 100;
