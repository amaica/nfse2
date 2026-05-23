package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.ConfiguracaoNfse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracaoNfseRepository extends JpaRepository<ConfiguracaoNfse, Long> {
    Optional<ConfiguracaoNfse> findByEmpresaId(Long empresaId);
}
