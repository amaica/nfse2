package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.NfeDistribuicaoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NfeDistribuicaoDFeJob {

    private static final Logger log = LoggerFactory.getLogger(NfeDistribuicaoDFeJob.class);

    private final NfeDistribuicaoProperties props;
    private final NfeDistribuicaoDFeService service;

    public NfeDistribuicaoDFeJob(NfeDistribuicaoProperties props, NfeDistribuicaoDFeService service) {
        this.props = props;
        this.service = service;
    }

    @Scheduled(cron = "${nfse.nfe.distribuicao.cron:0 20 * * * *}", zone = "America/Sao_Paulo")
    public void executar() {
        if (!props.enabled()) {
            return;
        }
        int ok = service.baixarTodasMarcadas();
        if (ok > 0) {
            log.info("Download automatico de XML concluido para {} emitente(s) marcados", ok);
        }
    }
}
