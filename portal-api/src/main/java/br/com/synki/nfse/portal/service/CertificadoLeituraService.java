package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.CertificadoRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CertificadoLeituraService {

    private static final Pattern CNPJ = Pattern.compile("(\\d{14})");
    private static final Pattern CPF = Pattern.compile("(\\d{11})");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final String MENSAGEM_RECUSA_SEFAZ =
            "A SEFAZ recusou o certificado digital deste emitente. "
                    + "Ele pode estar vencido, revogado ou incompleto. "
                    + "Atualize o certificado A1 e tente de novo.";

    private final CertificadoRepository certificadoRepository;

    public CertificadoLeituraService(CertificadoRepository certificadoRepository) {
        this.certificadoRepository = certificadoRepository;
    }

    public record Metadados(
            String documento,
            boolean pessoaFisica,
            String titular,
            java.time.LocalDate validade,
            boolean eCpf,
            String cpfTitular) {
        public Metadados(String documento, boolean pessoaFisica, String titular) {
            this(documento, pessoaFisica, titular, null, pessoaFisica, pessoaFisica ? documento : null);
        }
    }

    /**
     * Mensagem para a tela de emissão quando o A1 impede falar com a SEFAZ.
     * Vazio quando o certificado abre e está dentro da validade.
     */
    public Optional<String> avisoEmissao(Long empresaId) {
        try {
            exigirValido(empresaId);
            return Optional.empty();
        } catch (IllegalStateException e) {
            return Optional.ofNullable(e.getMessage());
        }
    }

    /** Falha com texto que o emitente entende, antes de chamar a SEFAZ. */
    public void exigirValido(Long empresaId) {
        var cert = certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Certificado A1 não cadastrado. Envie o arquivo .pfx do emitente."));
        X509Certificate x509;
        try {
            var ks = KeyStore.getInstance("PKCS12");
            var senha = cert.getSenha() != null ? cert.getSenha() : "";
            ks.load(new ByteArrayInputStream(cert.getArquivo()), senha.toCharArray());
            var alias = ks.aliases().nextElement();
            x509 = (X509Certificate) ks.getCertificate(alias);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não foi possível abrir o certificado A1. Confira o arquivo e a senha.");
        }
        if (x509 == null) {
            throw new IllegalStateException(
                    "Não foi possível abrir o certificado A1. Confira o arquivo e a senha.");
        }
        var agora = Instant.now();
        if (agora.isAfter(x509.getNotAfter().toInstant())) {
            throw new IllegalStateException(mensagemVencido(x509.getNotAfter().toInstant()));
        }
        if (agora.isBefore(x509.getNotBefore().toInstant())) {
            var inicio = x509.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            throw new IllegalStateException(
                    "O certificado digital A1 deste emitente ainda não está válido. A validade começa em "
                            + inicio.format(DATA_BR) + ".");
        }
    }

    public static String mensagemVencido(Instant notAfter) {
        var fim = notAfter.atZone(ZoneId.systemDefault()).toLocalDate();
        return mensagemVencido(fim);
    }

    public static String mensagemVencido(LocalDate validade) {
        return "O certificado digital A1 deste emitente venceu em " + validade.format(DATA_BR)
                + ". A SEFAZ recusa a nota. Cadastre um certificado novo.";
    }

    public Optional<Metadados> lerMetadados(Long empresaId) {
        return certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).flatMap(cert -> {
            try {
                var ks = KeyStore.getInstance("PKCS12");
                ks.load(new ByteArrayInputStream(cert.getArquivo()), cert.getSenha().toCharArray());
                var alias = ks.aliases().nextElement();
                var x509 = (X509Certificate) ks.getCertificate(alias);
                var meta = parseSubject(x509.getSubjectX500Principal().getName());
                var validade = x509.getNotAfter().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                return Optional.of(new Metadados(
                        meta.documento(), meta.pessoaFisica(), meta.titular(), validade, meta.eCpf(), meta.cpfTitular()));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    public static Metadados parseSubject(String subject) {
        String cpfTitular = extrairCpfTitular(subject);
        Matcher cnpj = CNPJ.matcher(subject);
        if (cnpj.find()) {
            boolean eCpf = subject.contains("e-CPF");
            return new Metadados(cnpj.group(1), false, extrairNome(subject, cnpj.group(1)), null, eCpf, cpfTitular);
        }
        Matcher cpf = CPF.matcher(subject);
        if (cpf.find()) {
            String doc = cpf.group(1);
            return new Metadados(doc, true, extrairNome(subject, doc), null, true, doc);
        }
        throw new IllegalArgumentException("Nao foi possivel identificar CPF/CNPJ no certificado digital");
    }

    /** CPF do titular no CN (ex.: NOME:12345678901 em certificado e-CPF). */
    private static String extrairCpfTitular(String subject) {
        int cn = subject.indexOf("CN=");
        if (cn < 0) {
            return null;
        }
        String cnValue = subject.substring(cn + 3);
        int comma = cnValue.indexOf(',');
        if (comma > 0) {
            cnValue = cnValue.substring(0, comma);
        }
        Matcher cpfNoCn = Pattern.compile(":(\\d{11})$").matcher(cnValue.trim());
        if (cpfNoCn.find()) {
            return cpfNoCn.group(1);
        }
        return null;
    }

    private static String extrairNome(String subject, String documento) {
        int cn = subject.indexOf("CN=");
        if (cn < 0) {
            return "Prestador";
        }
        String cnValue = subject.substring(cn + 3);
        int comma = cnValue.indexOf(',');
        if (comma > 0) {
            cnValue = cnValue.substring(0, comma);
        }
        cnValue = cnValue.replace(":" + documento, "").replace(documento + ":", "").trim();
        // e-CNPJ: CN costuma ser "TITULAR:CPF" e o CNPJ vem em outro campo do subject
        cnValue = cnValue.replaceAll(":\\d{11}$", "").trim();
        return cnValue.isBlank() ? "Prestador" : cnValue;
    }
}
