package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {

    boolean existsByUsuarioIdAndEmpresaIdAndAtivoTrue(Long usuarioId, Long empresaId);

    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaIdAndAtivoTrue(Long usuarioId, Long empresaId);

    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);

    List<UsuarioEmpresa> findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(Long usuarioId);

    List<UsuarioEmpresa> findByContaIdAndAtivoTrueOrderByUsuarioIdAsc(Long contaId);

    List<UsuarioEmpresa> findByEmpresaIdInAndAtivoTrue(Collection<Long> empresaIds);

    List<UsuarioEmpresa> findByUsuarioIdAndContaIdAndAtivoTrue(Long usuarioId, Long contaId);

    boolean existsByUsuarioIdAndContaIdAndAtivoTrue(Long usuarioId, Long contaId);

    Optional<UsuarioEmpresa> findFirstByUsuarioIdAndContaIdAndAtivoTrueOrderByIdAsc(Long usuarioId, Long contaId);

    @Query("""
            SELECT ue.contaId FROM UsuarioEmpresa ue
            WHERE ue.usuarioId = :usuarioId AND ue.ativo = true
            ORDER BY ue.id ASC
            """)
    List<Long> findContaIdsByUsuarioId(@Param("usuarioId") Long usuarioId);

    void deleteByEmpresaId(Long empresaId);
}
