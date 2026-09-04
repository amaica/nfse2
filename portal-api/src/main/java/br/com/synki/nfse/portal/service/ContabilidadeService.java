package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.MailProperties;
import br.com.synki.nfse.portal.domain.ConfigContabilidade;
import br.com.synki.nfse.portal.domain.NfeEmissao;
import br.com.synki.nfse.portal.domain.NfseLog;
import br.com.synki.nfse.portal.repository.ConfigContabilidadeRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ContabilidadeService {

    private static final Logger log = LoggerFactory.getLogger(ContabilidadeService.class);
    private static final Pattern CHAVE_NFSE = Pattern.compile("(\\d{50})");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ConfigContabilidadeRepository configRepository;
    private final NfseLibService nfseLibService;
    private final NfseLogRepository nfseLogRepository;
    private final NfeEmissaoRepository nfeEmissaoRepository;
    private final EmpresaRepository empresaRepository;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public ContabilidadeService(
            ConfigContabilidadeRepository configRepository,
            NfseLibService nfseLibService,
            NfseLogRepository nfseLogRepository,
            NfeEmissaoRepository nfeEmissaoRepository,
            EmpresaRepository empresaRepository,
            JavaMailSender mailSender,
            MailProperties mailProperties) {
        this.configRepository = configRepository;
        this.nfseLibService = nfseLibService;
        this.nfseLogRepository = nfseLogRepository;
        this.nfeEmissaoRepository = nfeEmissaoRepository;
        this.empresaRepository = empresaRepository;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    public Map<String, Object> obterConfig(Long empresaId) {
        return toMap(configRepository.findById(empresaId).orElse(ConfigContabilidade.padrao(empresaId)));
    }

    @Transactional
    public Map<String, Object> salvarConfig(
            Long empresaId,
            String emailContabilidade,
            boolean envioAutomatico,
            boolean enviarNfse,
            boolean enviarNfe,
            boolean enviarNfeEntrada) {
        var cfg = configRepository.findById(empresaId).orElseGet(() -> {
            var novo = ConfigContabilidade.padrao(empresaId);
            novo.setEmpresaId(empresaId);
            return novo;
        });
        var email = emailContabilidade != null ? emailContabilidade.trim().toLowerCase() : "";
        if (envioAutomatico && !email.contains("@")) {
            throw new IllegalArgumentException("Informe o e-mail da contabilidade para ativar o envio automatico");
        }
        if (!email.isBlank() && !email.contains("@")) {
            throw new IllegalArgumentException("E-mail da contabilidade invalido");
        }
        cfg.setEmailContabilidade(email.isBlank() ? null : email);
        cfg.setEnvioAutomatico(envioAutomatico);
        cfg.setEnviarNfse(enviarNfse);
        cfg.setEnviarNfe(enviarNfe);
        cfg.setEnviarNfeEntrada(enviarNfeEntrada);
        cfg.setUpdatedAt(Instant.now());
        configRepository.save(cfg);
        return toMap(cfg);
    }

    @Async
    public void enviarNfseAposEmissao(Long empresaId, String chave) {
        var cfg = configRepository.findById(empresaId).orElse(null);
        if (!deveEnviar(cfg, cfg != null && cfg.isEnviarNfse())) {
            return;
        }
        try {
            String xml = nfseLibService.downloadXml(empresaId, chave);
            if (xml == null || xml.isBlank()) {
                log.warn("XML NFS-e indisponivel para contabilidade: {}", chave);
                return;
            }
            enviarXmlPorEmail(empresaId, "NFS-e", chave, xml.getBytes(StandardCharsets.UTF_8), cfg.getEmailContabilidade());
            log.info("XML NFS-e {} enviado para contabilidade {}", chave, cfg.getEmailContabilidade());
        } catch (Exception ex) {
            log.warn("Falha envio XML NFS-e para contabilidade ({}): {}", chave, ex.getMessage());
        }
    }

    @Async
    public void enviarNfeAposEmissao(Long empresaId, String chave) {
        var cfg = configRepository.findById(empresaId).orElse(null);
        if (!deveEnviar(cfg, cfg != null && cfg.isEnviarNfe())) {
            return;
        }
        var chaveNorm = chave.replace("NFe", "");
        try {
            var emissao = nfeEmissaoRepository
                    .findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(empresaId, chaveNorm)
                    .filter(e -> e.getXmlProc() != null && !e.getXmlProc().isBlank())
                    .orElse(null);
            if (emissao == null) {
                log.warn("XML NF-e indisponivel para contabilidade: {}", chaveNorm);
                return;
            }
            enviarXmlPorEmail(empresaId, "NF-e", chaveNorm, emissao.getXmlProc().getBytes(StandardCharsets.UTF_8),
                    cfg.getEmailContabilidade());
            log.info("XML NF-e {} enviado para contabilidade {}", chaveNorm, cfg.getEmailContabilidade());
        } catch (Exception ex) {
            log.warn("Falha envio XML NF-e para contabilidade ({}): {}", chaveNorm, ex.getMessage());
        }
    }

    public byte[] exportarZip(Long empresaId, LocalDate de, LocalDate ate, boolean incluirNfse, boolean incluirNfe)
            throws Exception {
        if (de == null || ate == null || ate.isBefore(de)) {
            throw new IllegalArgumentException("Periodo invalido");
        }
        if (!incluirNfse && !incluirNfe) {
            throw new IllegalArgumentException("Selecione NFS-e e/ou NF-e");
        }
        Instant inicio = de.atStartOfDay(FUSO).toInstant();
        Instant fim = ate.plusDays(1).atStartOfDay(FUSO).toInstant();

        var baos = new ByteArrayOutputStream();
        int total = 0;
        try (var zos = new ZipOutputStream(baos)) {
            if (incluirNfse) {
                total += adicionarNfseNoZip(empresaId, inicio, fim, zos);
            }
            if (incluirNfe) {
                total += adicionarNfeNoZip(empresaId, inicio, fim, zos);
            }
        }
        if (total == 0) {
            throw new IllegalStateException("Nenhum XML encontrado no periodo informado");
        }
        return baos.toByteArray();
    }

    private int adicionarNfseNoZip(Long empresaId, Instant inicio, Instant fim, ZipOutputStream zos) throws Exception {
        List<NfseLog> logs = nfseLogRepository.findByEmpresaIdAndAcaoAndCreatedAtBetweenOrderByCreatedAtAsc(
                empresaId, "EMISSAO", inicio, fim);
        int count = 0;
        for (NfseLog item : logs) {
            String chave = extrairChaveNfse(item.getDescricao());
            if (chave == null) {
                continue;
            }
            try {
                String xml = nfseLibService.downloadXml(empresaId, chave);
                if (xml == null || xml.isBlank()) {
                    continue;
                }
                adicionarEntradaZip(zos, "nfse/" + chave + ".xml", xml.getBytes(StandardCharsets.UTF_8));
                count++;
            } catch (Exception ex) {
                log.debug("ZIP NFS-e {} ignorada: {}", chave, ex.getMessage());
            }
        }
        return count;
    }

    private int adicionarNfeNoZip(Long empresaId, Instant inicio, Instant fim, ZipOutputStream zos) throws Exception {
        List<NfeEmissao> notas = nfeEmissaoRepository.findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                empresaId, inicio, fim);
        int count = 0;
        for (NfeEmissao nota : notas) {
            if (nota.getXmlProc() == null || nota.getXmlProc().isBlank()) {
                continue;
            }
            adicionarEntradaZip(zos, "nfe/" + nota.getChave() + "-proc.xml",
                    nota.getXmlProc().getBytes(StandardCharsets.UTF_8));
            count++;
        }
        return count;
    }

    private void enviarXmlPorEmail(
            Long empresaId,
            String tipoDoc,
            String chave,
            byte[] xmlBytes,
            String destinatario) throws Exception {
        if (!mailProperties.enabled()) {
            log.warn("E-mail nao configurado — XML {} nao enviado para contabilidade", chave);
            return;
        }
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        String sufixo = chave.length() > 8 ? chave.substring(chave.length() - 8) : chave;
        String prefixo = tipoDoc.equals("NFS-e") ? "nfse" : "nfe";
        String assunto = tipoDoc + " " + empresa.getNome() + " — XML para contabilidade";
        String corpo = """
                Segue em anexo o XML autorizado para escrituracao contabil.

                Documento: %s
                Chave de acesso: %s
                Emitente: %s

                Enviado automaticamente pelo SyncNota.
                """.formatted(tipoDoc, chave, empresa.getNome());

        MimeMessage message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.from(), mailProperties.fromName());
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(corpo, false);
        final byte[] anexo = xmlBytes;
        helper.addAttachment(prefixo + "-" + sufixo + ".xml", () -> new java.io.ByteArrayInputStream(anexo),
                "application/xml");
        mailSender.send(message);
    }

    private static boolean deveEnviar(ConfigContabilidade cfg, boolean tipoHabilitado) {
        return cfg != null
                && cfg.isEnvioAutomatico()
                && tipoHabilitado
                && cfg.getEmailContabilidade() != null
                && cfg.getEmailContabilidade().contains("@");
    }

    private static void adicionarEntradaZip(ZipOutputStream zos, String nome, byte[] conteudo) throws Exception {
        var entry = new ZipEntry(nome);
        zos.putNextEntry(entry);
        zos.write(conteudo);
        zos.closeEntry();
    }

    private static String extrairChaveNfse(String descricao) {
        if (descricao == null) {
            return null;
        }
        var m = CHAVE_NFSE.matcher(descricao);
        return m.find() ? m.group(1) : null;
    }

    private static Map<String, Object> toMap(ConfigContabilidade cfg) {
        var body = new LinkedHashMap<String, Object>();
        body.put("emailContabilidade", cfg.getEmailContabilidade() != null ? cfg.getEmailContabilidade() : "");
        body.put("envioAutomatico", cfg.isEnvioAutomatico());
        body.put("enviarNfse", cfg.isEnviarNfse());
        body.put("enviarNfe", cfg.isEnviarNfe());
        body.put("enviarNfeEntrada", cfg.isEnviarNfeEntrada());
        body.put("updatedAt", cfg.getUpdatedAt() != null ? cfg.getUpdatedAt().toString() : null);
        return body;
    }
}
