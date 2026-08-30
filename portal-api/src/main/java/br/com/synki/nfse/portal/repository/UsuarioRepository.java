package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndAtivoTrue(String email);

    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(Long empresaId);
    List<Usuario> findByEmpresaId(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
