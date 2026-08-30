package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributConfiguraOfGt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TributConfiguraOfGtRepository extends JpaRepository<TributConfiguraOfGt, Long> {

    @EntityGraph(attributePaths = "listaIcmsUf")
    List<TributConfiguraOfGt> findByEmpresaIdOrderByIdAsc(Long empresaId);

    @EntityGraph(attributePaths = "listaIcmsUf")
    Optional<TributConfiguraOfGt> findByIdAndEmpresaId(Long id, Long empresaId);

    @EntityGraph(attributePaths = "listaIcmsUf")
    Optional<TributConfiguraOfGt> findByEmpresaIdAndTributOperacaoFiscalIdAndTributGrupoTributarioId(
            Long empresaId, Long tributOperacaoFiscalId, Long tributGrupoTributarioId);

    void deleteByEmpresaId(Long empresaId);
}
