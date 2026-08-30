package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfseLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NfseLogRepository extends JpaRepository<NfseLog, Long> {
    List<NfseLog> findTop50ByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    List<NfseLog> findByEmpresaIdAndAcaoOrderByCreatedAtDesc(Long empresaId, String acao, Pageable pageable);

    List<NfseLog> findByEmpresaIdAndAcaoAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long empresaId, String acao, Instant de, Instant ate);

    void deleteByEmpresaId(Long empresaId);
}
