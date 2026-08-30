package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Produto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId, Pageable pageable);

    @Query("""
            SELECT p FROM Produto p
            WHERE p.empresaId = :empresaId AND p.ativo = true
              AND (LOWER(p.nome) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.nome ASC
            """)
    List<Produto> buscarAtivosPorNomeOuCodigo(
            @Param("empresaId") Long empresaId, @Param("q") String q, Pageable pageable);

    List<Produto> findByEmpresaIdOrderByNomeAsc(Long empresaId, Pageable pageable);
    Optional<Produto> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
    Optional<Produto> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
}
