package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class NfseEmailService {

    private static final Logger log = LoggerFactory.getLogger(NfseEmailService.class);

    public record EnviarDanfeResult(boolean anexoXml, boolean pdfRetryAgendado) {}

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final NfseLibService nfseLibService;
    private final EmpresaRepository empresaRepository;
    private final NfsePdfEmailRetryService pdfEmailRetryService;

    public NfseEmailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            NfseLibService nfseLibService,
            EmpresaRepository empresaRepository,
            @Lazy NfsePdfEmailRetryService pdfEmailRetryService) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.nfseLibService = nfseLibService;
        this.empresaRepository = empresaRepository;
        this.pdfEmailRetryService = pdfEmailRetryService;
    }

    public EnviarDanfeResult enviarDanfe(Long empresaId, String chave, String destinatario, String mensagem) throws Exception {
        if (!mailProperties.enabled()) {
            throw new IllegalStateException("Envio de e-mail nao configurado (MAIL_USER / MAIL_PASSWORD)");
        }
        String email = destinatario == null ? "" : destinatario.trim().toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("E-mail do destinatario invalido");
        }

        byte[] pdf = baixarPdfComRetentativas(empresaId, chave);
        if (pdf != null && pdf.length >= 500) {
            enviarDanfePdf(empresaId, chave, email, mensagem, pdf, false);
            return new EnviarDanfeResult(false, false);
        }

        log.warn("DANFSe PDF indisponivel para {} — anexando XML e agendando reenvio", chave);
        String xml = nfseLibService.downloadXml(empresaId, chave);
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("PDF e XML da NFS-e indisponiveis para a chave informada");
        }
        enviarDanfeXml(empresaId, chave, email, mensagem, xml.getBytes(StandardCharsets.UTF_8));
        if (!pdfEmailRetryService.jaEnviado(empresaId, chave, email)) {
            pdfEmailRetryService.agendar(empresaId, chave, email, mensagem);
            return new EnviarDanfeResult(true, true);
        }
        return new EnviarDanfeResult(true, false);
    }

    public void enviarDanfePdf(
            Long empresaId,
            String chave,
            String destinatario,
            String mensagem,
            byte[] pdf,
            boolean reenvioAutomatico) throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        String sufixo = chave.substring(Math.max(0, chave.length() - 8));
        String assunto = "NFS-e " + empresa.getNome() + " — DANFSe"
                + (reenvioAutomatico ? " (atualizacao)" : "");
        String corpoBase = mensagem != null && !mensagem.isBlank()
                ? mensagem.trim()
                : reenvioAutomatico
                        ? "O PDF do DANFSe ficou disponivel na SEFIN. Segue em anexo."
                        : "Segue em anexo o documento da NFS-e emitida.";
        String corpo = corpoBase + "\n\nChave de acesso: " + chave;

        MimeMessage message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.from(), mailProperties.fromName());
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(corpo, false);
        final byte[] pdfAnexo = pdf;
        helper.addAttachment("nfse-" + sufixo + ".pdf",
                () -> new ByteArrayInputStream(pdfAnexo), "application/pdf");
        mailSender.send(message);
    }

    private void enviarDanfeXml(Long empresaId, String chave, String destinatario, String mensagem, byte[] xmlBytes)
            throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        String sufixo = chave.substring(Math.max(0, chave.length() - 8));
        String assunto = "NFS-e " + empresa.getNome() + " — XML";
        String corpoBase = mensagem != null && !mensagem.isBlank()
                ? mensagem.trim()
                : "Segue em anexo o documento da NFS-e emitida.";
        String corpo = corpoBase + "\n\nChave de acesso: " + chave
                + "\n\n(O PDF do DANFSe ainda nao estava disponivel na SEFIN; enviamos o XML autorizado."
                + " O PDF sera enviado automaticamente quando o servico nacional responder.)";

        MimeMessage message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.from(), mailProperties.fromName());
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(corpo, false);
        final byte[] xmlAnexo = xmlBytes;
        helper.addAttachment("nfse-" + sufixo + ".xml",
                () -> new ByteArrayInputStream(xmlAnexo), "application/xml");
        mailSender.send(message);
    }

    private byte[] baixarPdfComRetentativas(Long empresaId, String chave) {
        int[] esperasMs = {0, 2000, 5000, 10000};
        for (int espera : esperasMs) {
            if (espera > 0) {
                try {
                    Thread.sleep(espera);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                byte[] pdf = nfseLibService.downloadPdf(empresaId, chave);
                if (pdf != null && pdf.length > 500) {
                    return pdf;
                }
            } catch (Exception ex) {
                log.debug("Tentativa PDF {} falhou: {}", chave, ex.getMessage());
            }
        }
        return null;
    }
}
