DELETE pm
FROM portal_perfil_menu pm
INNER JOIN portal_menu m ON m.id = pm.menu_id
WHERE m.outcome = '/nfe/registrar-evento';

DELETE FROM portal_menu
WHERE outcome = '/nfe/registrar-evento';
