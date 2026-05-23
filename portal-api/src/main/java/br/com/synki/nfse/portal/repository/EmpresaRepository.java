package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
