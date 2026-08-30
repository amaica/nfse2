package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.DunningAviso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DunningAvisoRepository extends JpaRepository<DunningAviso, Long> {

    boolean existsByContaIdAndTipoAndReferencia(Long contaId, String tipo, String referencia);
}
