package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfseOperacaoMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NfseOperacaoMensalRepository extends JpaRepository<NfseOperacaoMensal, Long> {
    List<NfseOperacaoMensal> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);
    Optional<NfseOperacaoMensal> findByIdAndEmpresaId(Long id, Long empresaId);
}
