package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.service.NfeXmlResumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Order(200)
public class NfeEmissaoResumoBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NfeEmissaoResumoBackfillRunner.class);

    private final NfeEmissaoRepository repository;

    public NfeEmissaoResumoBackfillRunner(NfeEmissaoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        int atualizados = 0;
        int page = 0;
        while (true) {
            var pagina = repository.findAll(PageRequest.of(page, 50));
            if (pagina.isEmpty()) {
                break;
            }
            for (var e : pagina) {
                if (e.getDestinatarioNome() != null || e.getValorTotal() != null) {
                    continue;
                }
                if (e.getXmlProc() == null || e.getXmlProc().isBlank()) {
                    continue;
                }
                var resumo = NfeXmlResumo.extrair(e.getXmlProc());
                if (resumo.valorTotal() == null && resumo.destinatarioNome() == null) {
                    continue;
                }
                e.setValorTotal(resumo.valorTotal());
                e.setDestinatarioNome(resumo.destinatarioNome());
                e.setCfop(resumo.cfop());
                e.setTipoOperacao(resumo.tipoOperacao());
                e.setProdutosResumo(resumo.produtosResumo());
                repository.save(e);
                atualizados++;
            }
            if (!pagina.hasNext()) {
                break;
            }
            page++;
        }
        if (atualizados > 0) {
            log.info("Backfill resumo NF-e: {} registros atualizados", atualizados);
        }
    }
}
