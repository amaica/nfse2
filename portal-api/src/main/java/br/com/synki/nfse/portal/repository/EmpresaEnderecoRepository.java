package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface EmpresaEnderecoRepository extends JpaRepository<EmpresaEndereco, Long> {
    List<EmpresaEndereco> findByEmpresaIdOrderByPrincipalDescApelidoAsc(Long empresaId);
    Optional<EmpresaEndereco> findByEmpresaIdAndPrincipalTrue(Long empresaId);
    void deleteByEmpresaIdAndIdNotIn(Long empresaId, List<Long> ids);
    void deleteByEmpresaId(Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmpresaEndereco> findLockedById(Long id);
}
