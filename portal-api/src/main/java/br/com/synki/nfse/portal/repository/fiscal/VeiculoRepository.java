package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    List<Veiculo> findByEmpresaIdAndAtivoTrueOrderByPlacaAsc(Long empresaId);
    Optional<Veiculo> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<Veiculo> findByEmpresaIdAndPlaca(Long empresaId, String placa);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
