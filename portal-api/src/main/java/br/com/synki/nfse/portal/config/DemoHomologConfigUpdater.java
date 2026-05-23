package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class DemoHomologConfigUpdater implements ApplicationRunner {

    @Value("${nfse.demo.ambiente:homologacao}")
    private String ambiente;

    @Value("${nfse.demo.codigo-municipio-ibge:}")
    private String codigoMunicipioIbge;

    @Value("${nfse.demo.prefeitura:}")
    private String prefeitura;

    private final ConfiguracaoNfseRepository configuracaoRepository;

    public DemoHomologConfigUpdater(ConfiguracaoNfseRepository configuracaoRepository) {
        this.configuracaoRepository = configuracaoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        configuracaoRepository.findByEmpresaId(1L).ifPresent(cfg -> {
            boolean changed = false;
            if (ambiente != null && !ambiente.isBlank() && !ambiente.equalsIgnoreCase(cfg.getAmbiente())) {
                cfg.setAmbiente(ambiente.trim().toLowerCase());
                changed = true;
            }
            if (codigoMunicipioIbge != null && codigoMunicipioIbge.matches("\\d{7}")) {
                cfg.setCodigoMunicipioIbge(codigoMunicipioIbge);
                changed = true;
            }
            if (prefeitura != null && !prefeitura.isBlank()) {
                cfg.setPrefeitura(prefeitura);
                changed = true;
            }
            if (changed) {
                configuracaoRepository.save(cfg);
            }
        });
    }
}
