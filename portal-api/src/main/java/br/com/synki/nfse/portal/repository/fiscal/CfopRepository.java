package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cfop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CfopRepository extends JpaRepository<Cfop, Long> {
    List<Cfop> findByEmpresaIdIsNullOrEmpresaIdOrderByCfopAsc(Long empresaId);
    Optional<Cfop> findByCfopAndEmpresaIdIsNull(String cfop);
    long countByEmpresaIdIsNull();
    void deleteByEmpresaIdIsNull();
}
