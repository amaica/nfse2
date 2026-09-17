-- Simulado IR como menu raiz (fora de Conta)
UPDATE portal_menu
SET parent_id = NULL,
    outcome = '/simulado-ir',
    label = 'Simulado IR',
    icon = 'calculator',
    ordem_menu = 52,
    ativo = 1,
    operador_tem_acesso = 'NAO'
WHERE id = 34;

INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id)
SELECT 34, 'Simulado IR', 'calculator', '/simulado-ir', 52, 1, 'NAO', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM portal_menu WHERE id = 34);

-- Restaura ordem dos itens de Conta (Assinatura em diante) após remoção do filho 34
UPDATE portal_menu SET ordem_menu = 4 WHERE id = 25 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 5 WHERE id = 26 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 6 WHERE id = 27 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 7 WHERE id = 28 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 8 WHERE id = 29 AND parent_id = 6;
UPDATE portal_menu SET ordem_menu = 9 WHERE id = 32 AND parent_id = 6;

ALTER TABLE portal_menu AUTO_INCREMENT = 100;
