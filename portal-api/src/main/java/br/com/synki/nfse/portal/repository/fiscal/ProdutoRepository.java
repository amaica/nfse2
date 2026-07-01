package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);
    Optional<Produto> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
    Optional<Produto> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
}
