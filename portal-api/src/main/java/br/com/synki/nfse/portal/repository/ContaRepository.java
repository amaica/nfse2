package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    java.util.Optional<Conta> findByStripeCustomerId(String stripeCustomerId);

    java.util.Optional<Conta> findByOwnerUsuarioId(Long ownerUsuarioId);
}
