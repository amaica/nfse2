package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.service.fiscal.NfseServicoSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(150)
public class NfseServicoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NfseServicoSeedRunner.class);

    private final NfseServicoSeedService seedService;

    public NfseServicoSeedRunner(NfseServicoSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int criados = seedService.garantirCadastrosTodasEmpresas();
            if (criados > 0) {
                log.info("Seed tributacao NFS-e: {} cadastro(s) criado(s)", criados);
            }
        } catch (Exception ex) {
            log.warn("Falha ao criar cadastros NFS-e padrao: {}", ex.getMessage());
        }
    }
}
