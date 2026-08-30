package br.com.synki.nfse.portal.repository.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CestRepository extends JpaRepository<Cest, Long> {

    @Query("""
            SELECT c FROM Cest c
            WHERE (:q = '' OR c.codigo LIKE CONCAT('%', :q, '%')
                OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:ncm = '' OR c.ncmPrefixo IS NULL
                OR :ncm LIKE CONCAT(c.ncmPrefixo, '%')
                OR c.ncmPrefixo LIKE CONCAT(:ncm, '%'))
            ORDER BY c.codigo ASC
            """)
    List<Cest> buscar(@Param("q") String q, @Param("ncm") String ncm, Pageable pageable);
}
