package br.com.synki.nfse.portal.repository;

import br.com.synki.nfse.portal.domain.PortalMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalMenuRepository extends JpaRepository<PortalMenu, Long> {
    List<PortalMenu> findAllByOrderByOrdemMenuAscLabelAsc();
}
