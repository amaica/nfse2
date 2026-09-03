-- Link no menu Conta: Permissões (grupos de menu)
INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id)
VALUES (32, 'Permissões', 'shield', '/parametros/permissoes', 9, 1, 'NAO', 6)
ON DUPLICATE KEY UPDATE label = VALUES(label), outcome = VALUES(outcome);

ALTER TABLE portal_menu AUTO_INCREMENT = 100;
