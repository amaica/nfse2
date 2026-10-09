-- Nome amigável: NF-e recebidas (notas de terceiros contra o CNPJ / DF-e)
UPDATE portal_menu
SET label = 'NF-e recebidas',
    outcome = '/nfe/recebidas',
    icon = 'download'
WHERE id = 33
   OR outcome IN ('/nfe/notas-entrada', '/nfe/recebidas')
   OR label IN ('NF-e recebidas (DF-e)', 'Notas disponíveis para entrada', 'Notas de entrada');
