package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ConfiguracaoDocumentoRepository extends JpaRepository<ConfiguracaoDocumento, Long> {
    List<ConfiguracaoDocumento> findByEmpresaIdOrderByTipoAsc(Long empresaId);
    Optional<ConfiguracaoDocumento> findByEmpresaIdAndTipo(Long empresaId, String tipo);
    void deleteByEmpresaId(Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConfiguracaoDocumento> findLockedByEmpresaIdAndTipo(Long empresaId, String tipo);
}
