package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.ConfiguracaoNfse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConfiguracaoNfseRepository extends JpaRepository<ConfiguracaoNfse, Long> {
    Optional<ConfiguracaoNfse> findByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConfiguracaoNfse c WHERE c.empresaId = :empresaId")
    Optional<ConfiguracaoNfse> findByEmpresaId(@Param("empresaId") Long empresaId, LockModeType lockMode);
}
