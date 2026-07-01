package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.service.FluxoDataImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@ConditionalOnProperty(name = "nfse.fluxo.import-on-startup", havingValue = "true")
public class FluxoImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FluxoImportRunner.class);

    private final FluxoImportProperties props;
    private final FluxoDataImportService importService;

    public FluxoImportRunner(FluxoImportProperties props, FluxoDataImportService importService) {
        this.props = props;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.enabled()) {
            log.warn("nfse.fluxo.import-on-startup=true mas nfse.fluxo.enabled=false — import ignorado");
            return;
        }
        try {
            var result = importService.importar();
            log.info("Importacao fluxo na subida concluida: {}", result);
        } catch (Exception ex) {
            log.error("Falha na importacao fluxo na subida: {}", ex.getMessage(), ex);
        }
    }
}
