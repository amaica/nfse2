package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.UsuarioConvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioConviteRepository extends JpaRepository<UsuarioConvite, Long> {
    Optional<UsuarioConvite> findByToken(String token);

    List<UsuarioConvite> findByContaIdAndAceitoEmIsNullOrderByCreatedAtDesc(Long contaId);

    boolean existsByContaIdAndEmailAndAceitoEmIsNull(Long contaId, String email);
}
