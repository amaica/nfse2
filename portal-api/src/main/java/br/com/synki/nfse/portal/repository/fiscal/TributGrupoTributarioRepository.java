package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.TributGrupoTributario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TributGrupoTributarioRepository extends JpaRepository<TributGrupoTributario, Long> {
    List<TributGrupoTributario> findByEmpresaIdOrderByDescricaoAsc(Long empresaId);
    Optional<TributGrupoTributario> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<TributGrupoTributario> findFirstByEmpresaIdAndDescricaoIgnoreCase(Long empresaId, String descricao);
    Optional<TributGrupoTributario> findFirstByEmpresaIdOrderByIdAsc(Long empresaId);
    long countByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
