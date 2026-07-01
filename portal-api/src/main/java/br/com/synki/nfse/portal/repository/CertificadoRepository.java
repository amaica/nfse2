package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {
    Optional<Certificado> findFirstByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
