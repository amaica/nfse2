package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfsePdfEmailPendente;
import br.com.synki.nfse.portal.domain.NfsePdfEmailPendente.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NfsePdfEmailPendenteRepository extends JpaRepository<NfsePdfEmailPendente, Long> {

    Optional<NfsePdfEmailPendente> findByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
            Long empresaId, String chaveAcesso, String destinatario, Status status);

    boolean existsByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
            Long empresaId, String chaveAcesso, String destinatario, Status status);

    List<NfsePdfEmailPendente> findTop30ByStatusAndProximaTentativaEmLessThanEqualOrderByProximaTentativaEmAsc(
            Status status, Instant agora);
}
