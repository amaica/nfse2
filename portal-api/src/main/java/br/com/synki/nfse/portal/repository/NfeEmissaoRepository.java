package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.NfeEmissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NfeEmissaoRepository extends JpaRepository<NfeEmissao, Long> {
    Optional<NfeEmissao> findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(Long empresaId, String chave);
    List<NfeEmissao> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
