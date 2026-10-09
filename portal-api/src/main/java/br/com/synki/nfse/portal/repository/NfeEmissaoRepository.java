package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfeEmissao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.synki.nfse.portal.service.NfeListaItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
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
              AND (:ambiente IS NULL OR e.ambiente = :ambiente)
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
            @Param("ambiente") String ambiente,
            @Param("q") String q,
            Pageable pageable);

    boolean existsByEmpresaIdAndStatusProtocolo(Long empresaId, String statusProtocolo);

    @Query("""
            SELECT COALESCE(SUM(e.valorTotal), 0) FROM NfeEmissao e
            WHERE e.empresaId IN :empresaIds
              AND e.modelo = :modelo
              AND (:dataDe IS NULL OR e.createdAt >= :dataDe)
              AND (:dataAte IS NULL OR e.createdAt < :dataAte)
              AND (:numero IS NULL OR e.numero = :numero)
              AND (:serie IS NULL OR e.serie = :serie)
              AND (:status IS NULL OR e.statusProtocolo = :status)
              AND (:ambiente IS NULL OR e.ambiente = :ambiente)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                CAST(e.numero AS string) LIKE CONCAT('%', :q, '%') OR
                LOWER(e.serie) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(COALESCE(e.destinatarioNome, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    BigDecimal somarValorLista(
            @Param("empresaIds") Collection<Long> empresaIds,
            @Param("modelo") String modelo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte,
            @Param("numero") Long numero,
            @Param("serie") String serie,
            @Param("status") String status,
            @Param("ambiente") String ambiente,
            @Param("q") String q);

    @Query("""
            SELECT e FROM NfeEmissao e
            WHERE e.empresaId = :empresaId
              AND e.modelo = :modelo
              AND e.statusProtocolo = '100'
              AND e.ambiente = '1'
              AND e.createdAt >= :dataDe
              AND e.createdAt < :dataAte
            ORDER BY e.createdAt DESC
            """)
    List<NfeEmissao> findAutorizadasProducaoNoPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("modelo") String modelo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte);

    @Query("""
            SELECT e FROM NfeEmissao e
            WHERE e.empresaId IN :empresaIds
              AND e.modelo = :modelo
              AND e.statusProtocolo = '100'
              AND e.ambiente = '1'
              AND e.createdAt >= :dataDe
              AND e.createdAt < :dataAte
            ORDER BY e.createdAt DESC
            """)
    List<NfeEmissao> findAutorizadasProducaoNosEmitentes(
            @Param("empresaIds") Collection<Long> empresaIds,
            @Param("modelo") String modelo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte);

    @Query("""
            SELECT new br.com.synki.nfse.portal.service.NfeListaItem(
                e.id, e.empresaId, e.chave, e.serie, e.numero, e.modelo,
                e.statusProtocolo, e.motivoProtocolo, e.ambiente, e.createdAt,
                e.xmlProc is not null,
                e.valorTotal, e.destinatarioNome, e.cfop, e.tipoOperacao, e.produtosResumo)
            FROM NfeEmissao e
            WHERE e.empresaId IN :empresaIds
              AND e.modelo = :modelo
              AND (:dataDe IS NULL OR e.createdAt >= :dataDe)
              AND (:dataAte IS NULL OR e.createdAt < :dataAte)
              AND (:numero IS NULL OR e.numero = :numero)
              AND (:serie IS NULL OR e.serie = :serie)
              AND (:status IS NULL OR e.statusProtocolo = :status)
              AND (:ambiente IS NULL OR e.ambiente = :ambiente)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                CAST(e.numero AS string) LIKE CONCAT('%', :q, '%') OR
                LOWER(e.serie) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(COALESCE(e.destinatarioNome, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<NfeListaItem> findLista(
            @Param("empresaIds") Collection<Long> empresaIds,
            @Param("modelo") String modelo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte,
            @Param("numero") Long numero,
            @Param("serie") String serie,
            @Param("status") String status,
            @Param("ambiente") String ambiente,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT COUNT(e) FROM NfeEmissao e
            WHERE e.empresaId IN :empresaIds
              AND e.modelo = :modelo
              AND e.ambiente = :ambiente
              AND (:dataDe IS NULL OR e.createdAt >= :dataDe)
              AND (:dataAte IS NULL OR e.createdAt < :dataAte)
            """)
    long contarAmbiente(
            @Param("empresaIds") Collection<Long> empresaIds,
            @Param("modelo") String modelo,
            @Param("ambiente") String ambiente,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte);

    @Query("""
            SELECT e FROM NfeEmissao e
            WHERE e.empresaId IN :empresaIds
              AND e.modelo = :modelo
              AND (:dataDe IS NULL OR e.createdAt >= :dataDe)
              AND (:dataAte IS NULL OR e.createdAt < :dataAte)
              AND (:numero IS NULL OR e.numero = :numero)
              AND (:serie IS NULL OR e.serie = :serie)
              AND (:status IS NULL OR e.statusProtocolo = :status)
              AND (:ambiente IS NULL OR e.ambiente = :ambiente)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                CAST(e.numero AS string) LIKE CONCAT('%', :q, '%') OR
                LOWER(e.serie) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<NfeEmissao> findFiltradasEm(
            @Param("empresaIds") Collection<Long> empresaIds,
            @Param("modelo") String modelo,
            @Param("dataDe") Instant dataDe,
            @Param("dataAte") Instant dataAte,
            @Param("numero") Long numero,
            @Param("serie") String serie,
            @Param("status") String status,
            @Param("ambiente") String ambiente,
            @Param("q") String q,
            Pageable pageable);

    Optional<NfeEmissao> findFirstByEmpresaIdInAndChaveOrderByCreatedAtDesc(Collection<Long> empresaIds, String chave);

    @Query("""
            SELECT e.serie, MAX(e.numero)
            FROM NfeEmissao e
            WHERE e.empresaId = :empresaId AND e.modelo = :modelo AND e.ambiente = '1'
            GROUP BY e.serie
            ORDER BY e.serie
            """)
    List<Object[]> resumoSeries(@Param("empresaId") Long empresaId, @Param("modelo") String modelo);

    List<NfeEmissao> findByEmpresaIdInAndModeloAndChaveIn(Collection<Long> empresaIds, String modelo, List<String> chaves);

    List<NfeEmissao> findByEmpresaIdAndModeloAndChaveIn(Long empresaId, String modelo, List<String> chaves);

    List<NfeEmissao> findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long empresaId, Instant de, Instant ate);
    void deleteByEmpresaId(Long empresaId);

    @Query("SELECT COALESCE(MAX(e.numero), 0) FROM NfeEmissao e WHERE e.empresaId = :empresaId AND e.serie = :serie")
    long findMaxNumeroByEmpresaIdAndSerie(@Param("empresaId") Long empresaId, @Param("serie") String serie);
}
