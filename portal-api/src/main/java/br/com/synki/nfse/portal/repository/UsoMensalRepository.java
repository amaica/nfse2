package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.UsoMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsoMensalRepository extends JpaRepository<UsoMensal, Long> {
    Optional<UsoMensal> findByContaIdAndAnoMes(Long contaId, String anoMes);

    java.util.List<UsoMensal> findByContaIdOrderByAnoMesDesc(Long contaId, org.springframework.data.domain.Pageable pageable);
}
