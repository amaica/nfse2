package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfeEntrada;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NfeEntradaRepository extends JpaRepository<NfeEntrada, Long> {
    boolean existsByEmpresaIdAndChave(Long empresaId, String chave);

    Optional<NfeEntrada> findByEmpresaIdAndChave(Long empresaId, String chave);

    List<NfeEntrada> findByEmpresaIdAndDataEmissaoBetweenOrderByDataEmissaoAscIdAsc(
            Long empresaId, LocalDate de, LocalDate ate);

    @Query("""
            SELECT e FROM NfeEntrada e
            WHERE e.empresaId = :empresaId
              AND (:de IS NULL OR e.dataEmissao >= :de)
              AND (:ate IS NULL OR e.dataEmissao <= :ate)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                e.numero LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(e.nomeEmitente, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                e.cnpjEmitente LIKE CONCAT('%', :q, '%')
              )
            ORDER BY e.dataEmissao DESC, e.id DESC
            """)
    List<NfeEntrada> filtrar(
            @Param("empresaId") Long empresaId,
            @Param("de") LocalDate de,
            @Param("ate") LocalDate ate,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT COUNT(e) FROM NfeEntrada e
            WHERE e.empresaId = :empresaId
              AND (:de IS NULL OR e.dataEmissao >= :de)
              AND (:ate IS NULL OR e.dataEmissao <= :ate)
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                e.numero LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(e.nomeEmitente, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                e.cnpjEmitente LIKE CONCAT('%', :q, '%')
              )
            """)
    long contarFiltradas(
            @Param("empresaId") Long empresaId,
            @Param("de") LocalDate de,
            @Param("ate") LocalDate ate,
            @Param("q") String q);

    List<NfeEntrada> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);

    @Query("""
            SELECT e FROM NfeEntrada e
            WHERE e.empresaId = :empresaId
              AND (
                :q IS NULL OR :q = '' OR
                e.chave LIKE CONCAT('%', :q, '%') OR
                e.numero LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(e.nomeEmitente, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                e.cnpjEmitente LIKE CONCAT('%', :q, '%')
              )
            ORDER BY e.dataEmissao DESC, e.id DESC
            """)
    List<NfeEntrada> buscar(
            @Param("empresaId") Long empresaId,
            @Param("q") String q,
            Pageable pageable);

    void deleteByEmpresaId(Long empresaId);
}
