package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByContaIdOrderByCreatedAtDesc(Long contaId, Pageable pageable);

    long countByContaIdAndCreatedAtAfter(Long contaId, Instant after);

    List<AuditEvent> findByContaIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long contaId, Instant after, Pageable pageable);
}
