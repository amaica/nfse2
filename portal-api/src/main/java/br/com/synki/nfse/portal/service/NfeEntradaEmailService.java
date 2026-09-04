package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.repository.ConfigContabilidadeRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class NfeEntradaEmailService {

    private static final Logger log = LoggerFactory.getLogger(NfeEntradaEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmpresaRepository empresaRepository;
    private final ConfigContabilidadeRepository contabilidadeRepository;
    private final NfeEntradaRepository entradaRepository;

    public NfeEntradaEmailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            EmpresaRepository empresaRepository,
            ConfigContabilidadeRepository contabilidadeRepository,
            NfeEntradaRepository entradaRepository) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.empresaRepository = empresaRepository;
        this.contabilidadeRepository = contabilidadeRepository;
        this.entradaRepository = entradaRepository;
    }

    public void enviarNovasSeConfigurado(Long empresaId, Collection<Long> idsNovas) {
        if (idsNovas == null || idsNovas.isEmpty()) {
            return;
        }
        if (!mailProperties.enabled()) {
            return;
        }
        var cfg = contabilidadeRepository.findById(empresaId).orElse(null);
        if (cfg == null
                || !cfg.isEnvioAutomatico()
                || !cfg.isEnviarNfeEntrada()
                || cfg.getEmailContabilidade() == null
                || !cfg.getEmailContabilidade().contains("@")) {
            return;
        }
        try {
            enviarZip(empresaId, cfg.getEmailContabilidade().trim(), idsNovas);
        } catch (Exception ex) {
            log.warn("Falha ao e-mailar NF-e de entrada empresa {}: {}", empresaId, ex.getMessage());
        }
    }

    public void enviarZip(Long empresaId, String destinatario, Collection<Long> ids) throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        List<NfeEntrada> notas = entradaRepository.findByEmpresaIdAndIdIn(empresaId, ids);
        if (notas.isEmpty()) {
            return;
        }
        byte[] zip = montarZip(notas);
        MimeMessage message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.from(), mailProperties.fromName());
        helper.setTo(destinatario.trim().toLowerCase());
        helper.setSubject("SyncNota — " + notas.size() + " NF-e recebida(s) (DF-e) · " + empresa.getNome());
        helper.setText(
                "Segue em anexo o ZIP com XML(s) de NF-e emitidas contra o CNPJ do emitente "
                        + empresa.getNome()
                        + " ("
                        + empresa.getCnpj()
                        + ").\n\n"
                        + "Quantidade: "
                        + notas.size()
                        + "\n\nEquipe SyncNota",
                false);
        helper.addAttachment(
                "nfe-entrada-" + empresa.getCnpj() + ".zip",
                () -> new ByteArrayInputStream(zip),
                "application/zip");
        mailSender.send(message);
        log.info("E-mail DF-e enviado para {} ({} notas, empresa {})", destinatario, notas.size(), empresaId);
    }

    private static byte[] montarZip(List<NfeEntrada> notas) throws Exception {
        var baos = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(baos)) {
            for (NfeEntrada e : notas) {
                if (e.getXml() == null || e.getXml().isBlank()) {
                    continue;
                }
                String nome = (e.getChave() != null ? e.getChave() : "nfe-" + e.getId()) + "-proc.xml";
                zos.putNextEntry(new ZipEntry(nome));
                zos.write(e.getXml().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
