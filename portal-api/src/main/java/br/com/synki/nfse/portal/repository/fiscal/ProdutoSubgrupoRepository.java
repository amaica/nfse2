package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.ProdutoSubgrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoSubgrupoRepository extends JpaRepository<ProdutoSubgrupo, Long> {
    List<ProdutoSubgrupo> findByEmpresaIdOrderByNomeAsc(Long empresaId);
    List<ProdutoSubgrupo> findByEmpresaIdAndProdutoGrupoIdOrderByNomeAsc(Long empresaId, Long produtoGrupoId);
    Optional<ProdutoSubgrupo> findFirstByEmpresaIdAndProdutoGrupoIdAndNomeIgnoreCase(
            Long empresaId, Long produtoGrupoId, String nome);
}
