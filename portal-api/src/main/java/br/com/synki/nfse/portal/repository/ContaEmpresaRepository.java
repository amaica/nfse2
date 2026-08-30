package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.ContaEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaEmpresaRepository extends JpaRepository<ContaEmpresa, ContaEmpresa.ContaEmpresaId> {
    Optional<ContaEmpresa> findByEmpresaId(Long empresaId);

    List<ContaEmpresa> findByContaId(Long contaId);
}
