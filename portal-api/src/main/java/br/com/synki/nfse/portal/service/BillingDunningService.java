package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.config.StripeProperties;
import br.com.synki.nfse.portal.domain.Assinatura;
import br.com.synki.nfse.portal.domain.Conta;
import br.com.synki.nfse.portal.domain.DunningAviso;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.DunningAvisoRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class BillingDunningService {

    private static final Logger log = LoggerFactory.getLogger(BillingDunningService.class);
    private static final ZoneId TZ = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(TZ);

    public static final String TIPO_PAGAMENTO_FALHOU = "payment_failed";
    public static final String TIPO_TRIAL_EXPIRANDO = "trial_expiring";
    public static final String TIPO_ASSINATURA_VENCIDA = "subscription_vencida";

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final StripeProperties stripeProperties;
    private final ContaRepository contaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DunningAvisoRepository dunningAvisoRepository;
    private final AuditLogService auditLogService;

    public BillingDunningService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            StripeProperties stripeProperties,
            ContaRepository contaRepository,
            AssinaturaRepository assinaturaRepository,
            UsuarioRepository usuarioRepository,
            DunningAvisoRepository dunningAvisoRepository,
            AuditLogService auditLogService) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.stripeProperties = stripeProperties;
        this.contaRepository = contaRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioRepository = usuarioRepository;
        this.dunningAvisoRepository = dunningAvisoRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void avisarPagamentoFalhou(Long contaId, String invoiceId) {
        var ref = invoiceId != null && !invoiceId.isBlank() ? invoiceId : "sem-id";
        enviarSeNecessario(contaId, TIPO_PAGAMENTO_FALHOU, ref,
                "Pagamento da assinatura não confirmado",
                corpoPagamentoFalhou());
    }

    @Scheduled(cron = "${nfse.billing.dunning-cron:0 0 9 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public void jobAvisosRecorrentes() {
        if (!mailProperties.enabled()) {
            return;
        }
        var agora = Instant.now();
        var limiteTrial = agora.plusSeconds(3L * 86400);
        for (var assinatura : assinaturaRepository.findAll()) {
            if (Assinatura.STATUS_TRIAL.equals(assinatura.getStatus())
                    && assinatura.getPeriodoFim() != null
                    && assinatura.getPeriodoFim().isAfter(agora)
                    && assinatura.getPeriodoFim().isBefore(limiteTrial)) {
                var ref = assinatura.getPeriodoFim().atZone(TZ).toLocalDate().toString();
                enviarSeNecessario(
                        assinatura.getContaId(),
                        TIPO_TRIAL_EXPIRANDO,
                        ref,
                        "Seu trial SyncNota termina em breve",
                        corpoTrialExpirando(assinatura.getPeriodoFim()));
            }
            if (Assinatura.STATUS_VENCIDA.equals(assinatura.getStatus())) {
                var ref = java.time.LocalDate.now(TZ).toString();
                enviarSeNecessario(
                        assinatura.getContaId(),
                        TIPO_ASSINATURA_VENCIDA,
                        ref,
                        "Assinatura SyncNota vencida — regularize o pagamento",
                        corpoAssinaturaVencida());
            }
        }
    }

    private void enviarSeNecessario(
            Long contaId,
            String tipo,
            String referencia,
            String assunto,
            String corpoHtml) {
        if (!mailProperties.enabled()) {
            return;
        }
        if (dunningAvisoRepository.existsByContaIdAndTipoAndReferencia(contaId, tipo, referencia)) {
            return;
        }
        var owner = ownerDaConta(contaId);
        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
            return;
        }
        try {
            enviarEmail(owner.getEmail(), assunto, corpoHtml);
            dunningAvisoRepository.save(DunningAviso.of(contaId, tipo, referencia));
            auditLogService.logConta(contaId, null, owner.getId(), "DUNNING_EMAIL", tipo, referencia);
            log.info("Dunning {} enviado para conta {} ({})", tipo, contaId, owner.getEmail());
        } catch (Exception ex) {
            log.warn("Falha ao enviar dunning {} conta {}: {}", tipo, contaId, ex.getMessage());
        }
    }

    private Usuario ownerDaConta(Long contaId) {
        return contaRepository.findById(contaId)
                .map(Conta::getOwnerUsuarioId)
                .flatMap(usuarioRepository::findById)
                .orElse(null);
    }

    private void enviarEmail(String destinatario, String assunto, String corpoHtml) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.from(), mailProperties.fromName());
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(corpoHtml, true);
        mailSender.send(message);
    }

    private String urlAssinatura() {
        return stripeProperties.portalReturnUrl() != null
                ? stripeProperties.portalReturnUrl()
                : "http://localhost:3000/conta/assinatura";
    }

    private String corpoPagamentoFalhou() {
        return """
                <p>Olá,</p>
                <p>Não conseguimos confirmar o pagamento da sua assinatura <strong>SyncNota</strong>.</p>
                <p>Atualize a forma de pagamento para evitar interrupção na emissão de NF-e e NFS-e:</p>
                <p><a href="%s">Gerenciar assinatura</a></p>
                <p>Equipe SyncNota</p>
                """.formatted(urlAssinatura());
    }

    private String corpoTrialExpirando(Instant fim) {
        return """
                <p>Olá,</p>
                <p>Seu período de trial termina em <strong>%s</strong>.</p>
                <p>Assine um plano para continuar emitindo documentos fiscais:</p>
                <p><a href="%s">Ver planos</a></p>
                <p>Equipe SyncNota</p>
                """.formatted(FMT.format(fim), urlAssinatura());
    }

    private String corpoAssinaturaVencida() {
        return """
                <p>Olá,</p>
                <p>Sua assinatura <strong>SyncNota</strong> está vencida.</p>
                <p>Regularize o pagamento para restaurar o acesso completo:</p>
                <p><a href="%s">Gerenciar assinatura</a></p>
                <p>Equipe SyncNota</p>
                """.formatted(urlAssinatura());
    }
}
