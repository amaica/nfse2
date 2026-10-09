package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfeEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;

public interface NfeEventoRepository extends JpaRepository<NfeEvento, Long> {

    @Query("""
            SELECT COALESCE(MAX(e.sequencial), 0) FROM NfeEvento e
            WHERE e.chave = :chave AND e.tipo = :tipo AND e.status = '135'
            """)
    int maxSequencialHomologado(@Param("chave") String chave, @Param("tipo") String tipo);

    @Query("""
            SELECT e FROM NfeEvento e
            WHERE e.empresaId IN :empresaIds
              AND e.modelo = :modelo
              AND e.status IN ('101', '102', '135', '136', '155')
              AND (:tipo IS NULL OR e.tipo = :tipo)
              AND (:dataDe IS NULL OR e.createdAt >= :dataDe)
              AND (:dataAte IS NULL OR e.createdAt < :dataAte)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(e.serie, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                CAST(e.numeroInicial AS string) LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(e.motivo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<NfeEvento> findLista(
            @Param("empresaIds") Collection<Long> empresaIds,
            @Param("modelo") String modelo,
            @Param("tipo") String tipo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte,
            @Param("q") String q,
            Pageable pageable);
}
