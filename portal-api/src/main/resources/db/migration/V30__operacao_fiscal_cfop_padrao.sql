-- CFOP da operação fiscal no Fluxo fica no ICMS por UF, não no cabeçalho.
-- Preenche o CFOP padrão com o mais frequente (e válido) das regras ICMS.
UPDATE tribut_operacao_fiscal o
INNER JOIN (
    SELECT tribut_operacao_fiscal_id, cfop
    FROM (
        SELECT
            c.tribut_operacao_fiscal_id,
            i.cfop,
            ROW_NUMBER() OVER (
                PARTITION BY c.tribut_operacao_fiscal_id
                ORDER BY COUNT(*) DESC, i.cfop
            ) AS rn
        FROM tribut_configura_of_gt c
        INNER JOIN tribut_icms_uf i ON i.configura_of_gt_id = c.id
        WHERE i.cfop BETWEEN 1000 AND 7999
        GROUP BY c.tribut_operacao_fiscal_id, i.cfop
    ) ranked
    WHERE rn = 1
) src ON src.tribut_operacao_fiscal_id = o.id
SET o.cfop = src.cfop
WHERE o.cfop IS NULL OR o.cfop < 1000;
