package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributNfseServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TributNfseServicoRepository extends JpaRepository<TributNfseServico, Long> {
    List<TributNfseServico> findByEmpresaIdAndAtivoTrueOrderByPrincipalDescDescricaoAsc(Long empresaId);
    List<TributNfseServico> findByEmpresaIdOrderByPrincipalDescDescricaoAsc(Long empresaId);
    Optional<TributNfseServico> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
