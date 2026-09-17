package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.PessoaEndereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PessoaEnderecoRepository extends JpaRepository<PessoaEndereco, Long> {
    List<PessoaEndereco> findByPessoaIdAndAtivoTrueOrderByPrincipalDescIdAsc(Long pessoaId);
    void deleteByPessoaId(Long pessoaId);
}
