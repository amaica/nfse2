package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.config.NfsePdfRetryProperties;
import br.com.synki.nfse.portal.domain.NfsePdfEmailPendente;
import br.com.synki.nfse.portal.domain.NfsePdfEmailPendente.Status;
import br.com.synki.nfse.portal.repository.NfsePdfEmailPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NfsePdfEmailRetryService {

    private static final Logger log = LoggerFactory.getLogger(NfsePdfEmailRetryService.class);
    private static final int[] BACKOFF_SEGUNDOS = {120, 300, 600, 900, 1800};

    private final NfsePdfEmailPendenteRepository repository;
    private final NfseEmailService nfseEmailService;
    private final NfseLibService nfseLibService;
    private final MailProperties mailProperties;
    private final NfsePdfRetryProperties retryProperties;

    public NfsePdfEmailRetryService(
            NfsePdfEmailPendenteRepository repository,
            NfseEmailService nfseEmailService,
            NfseLibService nfseLibService,
            MailProperties mailProperties,
            NfsePdfRetryProperties retryProperties) {
        this.repository = repository;
        this.nfseEmailService = nfseEmailService;
        this.nfseLibService = nfseLibService;
        this.mailProperties = mailProperties;
        this.retryProperties = retryProperties;
    }

    @Transactional
    public void agendar(Long empresaId, String chave, String destinatario, String mensagem) {
        if (!retryProperties.enabled() || !mailProperties.enabled()) {
            return;
        }
        String email = destinatario == null ? "" : destinatario.trim().toLowerCase();
        if (!email.contains("@")) {
            return;
        }
        if (repository.existsByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
                empresaId, chave, email, Status.ENVIADO)) {
            log.info("DANFSe ja enviado para {} -> {} — reenvio automatico ignorado", chave, email);
            return;
        }
        var existente = repository.findByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
                empresaId, chave, email, Status.PENDENTE);
        if (existente.isPresent()) {
            log.info("Reenvio PDF ja agendado para {} -> {}", chave, email);
            return;
        }
        var item = NfsePdfEmailPendente.criar(empresaId, chave, email, mensagem);
        repository.save(item);
        log.info("Reenvio PDF agendado para {} -> {} (proxima tentativa imediata no job)", chave, email);
    }

    public boolean jaEnviado(Long empresaId, String chave, String destinatario) {
        String email = destinatario == null ? "" : destinatario.trim().toLowerCase();
        return repository.existsByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
                empresaId, chave, email, Status.ENVIADO);
    }

    @Scheduled(fixedDelayString = "${nfse.pdf-retry.interval-ms:60000}")
    public void processarPendentes() {
        if (!retryProperties.enabled() || !mailProperties.enabled()) {
            return;
        }
        Instant agora = Instant.now();
        List<NfsePdfEmailPendente> pendentes = repository
                .findTop30ByStatusAndProximaTentativaEmLessThanEqualOrderByProximaTentativaEmAsc(Status.PENDENTE, agora);
        if (pendentes.isEmpty()) {
            return;
        }
        log.info("Processando {} reenvio(s) de DANFSe PDF pendente(s)", pendentes.size());
        for (var item : pendentes) {
            processarItem(item, agora);
        }
    }

    private void processarItem(NfsePdfEmailPendente item, Instant agora) {
        if (repository.existsByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
                item.getEmpresaId(), item.getChaveAcesso(), item.getDestinatario(), Status.ENVIADO)) {
            item.marcarExpirado();
            repository.save(item);
            log.info("Reenvio PDF cancelado (ja enviado) para {} -> {}", item.getChaveAcesso(), item.getDestinatario());
            return;
        }
        try {
            byte[] pdf = nfseLibService.downloadPdf(item.getEmpresaId(), item.getChaveAcesso());
            if (pdf == null || pdf.length < 500) {
                registrarFalha(item, agora, "PDF indisponivel ou vazio");
                return;
            }
            nfseEmailService.enviarDanfePdf(
                    item.getEmpresaId(),
                    item.getChaveAcesso(),
                    item.getDestinatario(),
                    item.getMensagem(),
                    pdf,
                    true);
            item.marcarEnviado();
            repository.save(item);
            log.info("DANFSe PDF reenviado com sucesso para {} -> {}", item.getChaveAcesso(), item.getDestinatario());
        } catch (Exception ex) {
            if (repository.existsByEmpresaIdAndChaveAcessoAndDestinatarioAndStatus(
                    item.getEmpresaId(), item.getChaveAcesso(), item.getDestinatario(), Status.ENVIADO)) {
                item.marcarExpirado();
                repository.save(item);
                log.warn("Reenvio PDF encerrado apos envio parcial para {} -> {}: {}",
                        item.getChaveAcesso(), item.getDestinatario(), ex.getMessage());
                return;
            }
            registrarFalha(item, agora, ex.getMessage());
        }
    }

    private void registrarFalha(NfsePdfEmailPendente item, Instant agora, String motivo) {
        int proximaTentativa = item.getTentativas() + 1;
        if (proximaTentativa >= retryProperties.maxTentativas()) {
            item.registrarTentativa(agora);
            item.marcarExpirado();
            repository.save(item);
            log.warn("Reenvio PDF expirado para {} -> {} apos {} tentativas: {}",
                    item.getChaveAcesso(), item.getDestinatario(), proximaTentativa, motivo);
            return;
        }
        int idx = Math.min(proximaTentativa - 1, BACKOFF_SEGUNDOS.length - 1);
        Instant proxima = agora.plusSeconds(BACKOFF_SEGUNDOS[idx]);
        item.registrarTentativa(proxima);
        repository.save(item);
        log.debug("Reenvio PDF adiado para {} -> {} (tentativa {}/{}): {}",
                item.getChaveAcesso(), item.getDestinatario(), proximaTentativa, retryProperties.maxTentativas(), motivo);
    }
}
