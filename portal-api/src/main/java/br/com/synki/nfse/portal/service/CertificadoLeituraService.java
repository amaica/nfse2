package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.CertificadoRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CertificadoLeituraService {

    private static final Pattern CNPJ = Pattern.compile("(\\d{14})");
    private static final Pattern CPF = Pattern.compile("(\\d{11})");

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
