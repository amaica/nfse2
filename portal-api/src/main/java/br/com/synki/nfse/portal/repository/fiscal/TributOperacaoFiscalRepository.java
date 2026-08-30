package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TributOperacaoFiscalRepository extends JpaRepository<TributOperacaoFiscal, Long> {
    List<TributOperacaoFiscal> findByEmpresaIdOrderByDescricaoAsc(Long empresaId);
    Optional<TributOperacaoFiscal> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<TributOperacaoFiscal> findFirstByEmpresaIdAndDescricaoIgnoreCase(Long empresaId, String descricao);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);

    @Modifying
    @Query(value = """
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
                      AND c.empresa_id = :empresaId
                    GROUP BY c.tribut_operacao_fiscal_id, i.cfop
                ) ranked
                WHERE rn = 1
            ) src ON src.tribut_operacao_fiscal_id = o.id
            SET o.cfop = src.cfop
            WHERE o.empresa_id = :empresaId
              AND (o.cfop IS NULL OR o.cfop < 1000)
            """, nativeQuery = true)
    int preencherCfopPadrao(@Param("empresaId") Long empresaId);
}
