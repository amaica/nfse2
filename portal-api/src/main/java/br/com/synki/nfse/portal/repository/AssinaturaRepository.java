package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    Optional<Assinatura> findByContaId(Long contaId);

    List<Assinatura> findByStatus(String status);
}
