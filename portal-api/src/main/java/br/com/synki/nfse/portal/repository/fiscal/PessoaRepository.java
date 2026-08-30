package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Pessoa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
    List<Pessoa> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId, Pageable pageable);
    List<Pessoa> findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAsc(Long empresaId, String nome, Pageable pageable);
    Optional<Pessoa> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
