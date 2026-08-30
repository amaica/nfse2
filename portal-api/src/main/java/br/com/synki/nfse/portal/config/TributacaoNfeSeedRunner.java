package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.service.fiscal.TributacaoNfeSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(151)
public class TributacaoNfeSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TributacaoNfeSeedRunner.class);

    private final TributacaoNfeSeedService seedService;
    private final EmpresaRepository empresaRepository;

    public TributacaoNfeSeedRunner(
            TributacaoNfeSeedService seedService,
            EmpresaRepository empresaRepository) {
        this.seedService = seedService;
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
                total += seedService.garantirCadastros(empresa.getId());
            } catch (Exception ex) {
                log.warn("Falha ao criar tributacao NF-e empresa {}: {}", empresa.getId(), ex.getMessage());
            }
        }
        if (total > 0) {
            log.info("Seed tributacao NF-e: {} ajuste(s) em emitentes", total);
        }
    }
}
