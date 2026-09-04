-- NF-e entradas (DF-e): menu + flag de e-mail periódico
ALTER TABLE config_contabilidade
    ADD COLUMN enviar_nfe_entrada BOOLEAN NOT NULL DEFAULT FALSE AFTER enviar_nfe;

INSERT INTO portal_menu (id, label, icon, outcome, ordem_menu, ativo, operador_tem_acesso, parent_id)
VALUES (33, 'NF-e recebidas (DF-e)', 'download', '/nfe/notas-entrada', 4, 1, 'SIM', 2)
ON DUPLICATE KEY UPDATE label = VALUES(label), outcome = VALUES(outcome), icon = VALUES(icon);

ALTER TABLE portal_menu AUTO_INCREMENT = 100;
