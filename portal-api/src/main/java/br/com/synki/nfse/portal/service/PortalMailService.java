package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.config.PortalProperties;
import br.com.synki.nfse.portal.domain.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PortalMailService {

    private static final Logger log = LoggerFactory.getLogger(PortalMailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final PortalProperties portalProperties;

    public PortalMailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            PortalProperties portalProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.portalProperties = portalProperties;
    }

    @Async
    public void enviarBoasVindas(Usuario user, String nomeConta) {
        if (!mailProperties.enabled() || user.getEmail() == null) {
            return;
        }
        enviarAsync(
                user.getEmail(),
                "Bem-vindo ao SyncNota — 14 dias grátis",
                """
                <p>Olá, <strong>%s</strong>!</p>
                <p>Sua conta <strong>%s</strong> foi criada no SyncNota.</p>
                <p>Você tem <strong>14 dias de trial</strong> para cadastrar emitentes, enviar certificado A1 e emitir NF-e e NFS-e.</p>
                <p><a href="%s/onboarding">Continuar cadastro</a></p>
                <p>Equipe SyncNota</p>
                """.formatted(esc(user.getNome()), esc(nomeConta), urlBase()));
    }

    @Async
    public void enviarEmitentePronto(Usuario user, String emitenteNome) {
        if (!mailProperties.enabled() || user.getEmail() == null) {
            return;
        }
        enviarAsync(
                user.getEmail(),
                "Emitente cadastrado — próximo passo: certificado",
                """
                <p>Olá, <strong>%s</strong>!</p>
                <p>O emitente <strong>%s</strong> está pronto no SyncNota.</p>
                <ol>
                  <li>Envie o certificado A1 em Cadastros → Emitentes</li>
                  <li>Cadastre clientes e produtos</li>
                  <li>Emita sua primeira nota</li>
                </ol>
                <p><a href="%s/painel">Abrir painel</a></p>
                <p>Equipe SyncNota</p>
                """.formatted(esc(user.getNome()), esc(emitenteNome), urlBase()));
    }

    @Async
    public void enviarAssinaturaAtiva(Usuario user) {
        if (!mailProperties.enabled() || user.getEmail() == null) {
            return;
        }
        enviarAsync(
                user.getEmail(),
                "Assinatura SyncNota confirmada",
                """
                <p>Olá, <strong>%s</strong>!</p>
                <p>Seu pagamento foi confirmado. O plano está <strong>ativo</strong>.</p>
                <p><a href="%s/painel">Continuar emitindo</a> · <a href="%s/conta/assinatura">Ver assinatura</a></p>
                <p>Obrigado por assinar o SyncNota!</p>
                """.formatted(esc(user.getNome()), urlBase(), urlBase()));
    }

    @Async
    public void enviarTrialEncerrado(Usuario user) {
        if (!mailProperties.enabled() || user.getEmail() == null) {
            return;
        }
        enviarAsync(
                user.getEmail(),
                "Trial encerrado — assine para continuar emitindo",
                """
                <p>Olá, <strong>%s</strong>!</p>
                <p>Seu período de teste de 14 dias terminou. A emissão de notas está pausada até a assinatura de um plano.</p>
                <p><a href="%s/conta/assinatura">Assinar agora</a></p>
                <p>Equipe SyncNota</p>
                """.formatted(esc(user.getNome()), urlBase()));
    }

    private void enviarAsync(String destinatario, String assunto, String corpoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.from(), mailProperties.fromName());
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);
            mailSender.send(message);
            log.info("E-mail enviado para {} — {}", destinatario, assunto);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail para {}: {}", destinatario, ex.getMessage());
        }
    }

    private String urlBase() {
        var base = portalProperties.embedBaseUrl();
        return base != null && !base.isBlank() ? base.replaceAll("/$", "") : "http://localhost:3000";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
