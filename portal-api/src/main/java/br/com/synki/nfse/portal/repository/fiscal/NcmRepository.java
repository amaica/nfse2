package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Ncm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NcmRepository extends JpaRepository<Ncm, Long> {
    List<Ncm> findByCodigoContainingOrDescricaoContainingOrderByCodigoAsc(String codigo, String descricao, Pageable pageable);
    Optional<Ncm> findByCodigo(String codigo);
    long count();
}
