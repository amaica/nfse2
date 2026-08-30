package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.service.fiscal.ProdutoClassificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(152)
public class ProdutoGrupoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdutoGrupoSeedRunner.class);

    private final ProdutoClassificacaoService service;
    private final EmpresaRepository empresaRepository;

    public ProdutoGrupoSeedRunner(
            ProdutoClassificacaoService service,
            EmpresaRepository empresaRepository) {
        this.service = service;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        int total = 0;
        for (var empresa : empresaRepository.findAll()) {
            if (!empresa.isAtivo()) {
                continue;
            }
            try {
                total += service.garantirPadrao(empresa.getId());
            } catch (Exception ex) {
                log.warn("Falha seed grupo/subgrupo produto empresa {}: {}", empresa.getId(), ex.getMessage());
            }
        }
        if (total > 0) {
            log.info("Seed grupo/subgrupo produto: {} item(ns)", total);
        }
    }
}
