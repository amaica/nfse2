-- Simulado IR liberado para operador (menu + ACL perfil Werlang)
UPDATE portal_menu
SET operador_tem_acesso = 'SIM',
    outcome = '/simulado-ir',
    parent_id = NULL,
    ativo = 1
WHERE id = 34;

INSERT IGNORE INTO portal_perfil_menu (perfil_id, menu_id)
SELECT p.id, 34
FROM portal_perfil p
WHERE p.nome IN ('operador_werlang')
   OR p.id IN (
        SELECT DISTINCT ue.portal_perfil_id
        FROM usuario_empresa ue
        JOIN usuario u ON u.id = ue.usuario_id
        WHERE u.email LIKE 'stefanni%'
          AND ue.portal_perfil_id IS NOT NULL
   );

-- Qualquer perfil que já tenha NF-e recebidas (DF-e) também ganha Simulado IR
INSERT IGNORE INTO portal_perfil_menu (perfil_id, menu_id)
SELECT DISTINCT pm.perfil_id, 34
FROM portal_perfil_menu pm
WHERE pm.menu_id = 33;
