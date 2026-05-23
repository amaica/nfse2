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

    public record Metadados(String documento, boolean pessoaFisica, String titular) {}

    public Optional<Metadados> lerMetadados(Long empresaId) {
        return certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).flatMap(cert -> {
            try {
                var ks = KeyStore.getInstance("PKCS12");
                ks.load(new ByteArrayInputStream(cert.getArquivo()), cert.getSenha().toCharArray());
                var alias = ks.aliases().nextElement();
                var x509 = (X509Certificate) ks.getCertificate(alias);
                return Optional.of(parseSubject(x509.getSubjectX500Principal().getName()));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    public static Metadados parseSubject(String subject) {
        Matcher cnpj = CNPJ.matcher(subject);
        if (cnpj.find()) {
            return new Metadados(cnpj.group(1), false, extrairNome(subject, cnpj.group(1)));
        }
        Matcher cpf = CPF.matcher(subject);
        if (cpf.find()) {
            return new Metadados(cpf.group(1), true, extrairNome(subject, cpf.group(1)));
        }
        throw new IllegalArgumentException("Nao foi possivel identificar CPF/CNPJ no certificado digital");
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
        return cnValue.isBlank() ? "Prestador" : cnValue;
    }
}
