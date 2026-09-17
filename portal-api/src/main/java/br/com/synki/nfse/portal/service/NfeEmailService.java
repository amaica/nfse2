package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class NfeEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final NfeDanfeService danfeService;
    private final EmpresaRepository empresaRepository;

    public NfeEmailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            NfeDanfeService danfeService,
            EmpresaRepository empresaRepository) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.danfeService = danfeService;
        this.empresaRepository = empresaRepository;
    }

    public void enviarDanfe(Long empresaId, String chave, String destinatario, String mensagem) throws Exception {
        if (!mailProperties.enabled()) {
            throw new IllegalStateException("Envio de e-mail nao configurado (MAIL_USER / MAIL_PASSWORD)");
        }
        String email = destinatario == null ? "" : destinatario.trim().toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("E-mail do destinatario invalido");
        }

        String chaveNorm = chave.replace("NFe", "");
        byte[] pdf = danfeService.gerarPdf(empresaId, chaveNorm);
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        String sufixo = chaveNorm.substring(Math.max(0, chaveNorm.length() - 8));
        String assunto = "NF-e " + empresa.getNome() + " — DANFE";
        String corpoBase = mensagem != null && !mensagem.isBlank()
                ? mensagem.trim()
                : "Segue em anexo o DANFE da NF-e emitida.";
        String corpo = corpoBase + "\n\nChave de acesso: " + chaveNorm;

        MimeMessage message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.from(), mailProperties.fromName());
        helper.setTo(email);
        helper.setSubject(assunto);
        helper.setText(corpo, false);
        final byte[] pdfAnexo = pdf;
        helper.addAttachment("danfe-" + sufixo + ".pdf",
                () -> new ByteArrayInputStream(pdfAnexo), "application/pdf");
        mailSender.send(message);
    }
}
