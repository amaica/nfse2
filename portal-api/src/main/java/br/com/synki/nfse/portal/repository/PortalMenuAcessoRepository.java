package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.PortalMenuAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PortalMenuAcessoRepository extends JpaRepository<PortalMenuAcesso, Long> {

    @Query("select a.menuId from PortalMenuAcesso a where a.usuarioId = :usuarioId and a.empresaId = :empresaId")
    List<Long> findMenuIdsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);

    boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PortalMenuAcesso a where a.usuarioId = :usuarioId and a.empresaId = :empresaId")
    void deleteByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);
}
