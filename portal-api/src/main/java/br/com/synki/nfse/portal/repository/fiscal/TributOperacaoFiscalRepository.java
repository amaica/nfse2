package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TributOperacaoFiscalRepository extends JpaRepository<TributOperacaoFiscal, Long> {
    List<TributOperacaoFiscal> findByEmpresaIdOrderByDescricaoAsc(Long empresaId);
    Optional<TributOperacaoFiscal> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
