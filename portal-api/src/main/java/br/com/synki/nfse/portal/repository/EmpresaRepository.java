package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByCnpj(String cnpj);
    Optional<Empresa> findByCnpjAndAtivoTrue(String cnpj);
    Optional<Empresa> findByFluxoLegacyId(Integer fluxoLegacyId);
    List<Empresa> findAllByOrderByNomeAsc();
}
