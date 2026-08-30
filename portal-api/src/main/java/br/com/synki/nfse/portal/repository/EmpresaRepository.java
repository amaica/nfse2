package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByCnpj(String cnpj);
    Optional<Empresa> findByCnpjAndAtivoTrue(String cnpj);
    Optional<Empresa> findByFluxoLegacyId(Integer fluxoLegacyId);
    List<Empresa> findAllByOrderByNomeAsc();

    List<Empresa> findByAtivoTrueOrderByNomeAsc();

    List<Empresa> findByBaixarXmlTrueAndAtivoTrue();

    @Query("""
            SELECT e FROM Empresa e
            WHERE e.ativo = true
              AND (
                LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                OR LOWER(COALESCE(e.nomeFantasia, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
                OR (:cnpj <> '' AND e.cnpj LIKE CONCAT('%', :cnpj, '%'))
              )
            ORDER BY e.nome ASC
            """)
    List<Empresa> buscarAtivas(@Param("termo") String termo, @Param("cnpj") String cnpj);

    @Query("""
            SELECT e FROM Empresa e
            WHERE e.id IN :ids
              AND e.ativo = true
              AND (
                LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                OR LOWER(COALESCE(e.nomeFantasia, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
                OR (:cnpj <> '' AND e.cnpj LIKE CONCAT('%', :cnpj, '%'))
              )
            ORDER BY e.nome ASC
            """)
    List<Empresa> buscarAtivasEntreIds(
            @Param("ids") List<Long> ids,
            @Param("termo") String termo,
            @Param("cnpj") String cnpj);
}
