package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfeEmissao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NfeEmissaoRepository extends JpaRepository<NfeEmissao, Long> {
    Optional<NfeEmissao> findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(Long empresaId, String chave);
    Page<NfeEmissao> findByEmpresaIdAndModeloOrderByCreatedAtDesc(Long empresaId, String modelo, Pageable pageable);

    @Query("""
            SELECT e FROM NfeEmissao e
            WHERE e.empresaId = :empresaId
              AND e.modelo = :modelo
              AND (:dataDe IS NULL OR e.createdAt >= :dataDe)
              AND (:dataAte IS NULL OR e.createdAt < :dataAte)
              AND (:numero IS NULL OR e.numero = :numero)
              AND (:serie IS NULL OR e.serie = :serie)
              AND (:status IS NULL OR e.statusProtocolo = :status)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                CAST(e.numero AS string) LIKE CONCAT('%', :q, '%') OR
                LOWER(e.serie) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<NfeEmissao> findFiltradas(
            @Param("empresaId") Long empresaId,
            @Param("modelo") String modelo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte,
            @Param("numero") Long numero,
            @Param("serie") String serie,
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable);

    List<NfeEmissao> findByEmpresaIdAndModeloAndChaveIn(Long empresaId, String modelo, List<String> chaves);

    List<NfeEmissao> findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long empresaId, Instant de, Instant ate);
    void deleteByEmpresaId(Long empresaId);

    @Query("SELECT COALESCE(MAX(e.numero), 0) FROM NfeEmissao e WHERE e.empresaId = :empresaId AND e.serie = :serie")
    long findMaxNumeroByEmpresaIdAndSerie(@Param("empresaId") Long empresaId, @Param("serie") String serie);
}
