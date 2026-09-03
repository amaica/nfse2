package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.PortalPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalPerfilRepository extends JpaRepository<PortalPerfil, Long> {
    List<PortalPerfil> findByContaIdOrderByNomeAsc(Long contaId);
    Optional<PortalPerfil> findByIdAndContaId(Long id, Long contaId);
    boolean existsByContaIdAndNomeIgnoreCase(Long contaId, String nome);
}
