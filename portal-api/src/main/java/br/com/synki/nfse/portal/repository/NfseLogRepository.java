package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NfseLogRepository extends JpaRepository<NfseLog, Long> {
    List<NfseLog> findTop50ByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
}
