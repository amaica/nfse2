package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.ProdutoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoGrupoRepository extends JpaRepository<ProdutoGrupo, Long> {
    List<ProdutoGrupo> findByEmpresaIdOrderByNomeAsc(Long empresaId);
    Optional<ProdutoGrupo> findFirstByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
    long countByEmpresaId(Long empresaId);
}
