package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.domain.Certificado;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.service.CertificadoLeituraService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Order(2)
public class DemoCertificadoLoader implements ApplicationRunner {

    @Value("${nfse.demo.certificado-path:}")
    private String certificadoPath;

    @Value("${nfse.demo.certificado-senha:}")
    private String certificadoSenha;

    private final CertificadoRepository certificadoRepository;
    private final EmpresaRepository empresaRepository;

    public DemoCertificadoLoader(CertificadoRepository certificadoRepository, EmpresaRepository empresaRepository) {
        this.certificadoRepository = certificadoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(1L).isPresent()) {
            return;
        }
        if (certificadoPath == null || certificadoPath.isBlank()) {
            return;
        }
        var path = Path.of(certificadoPath);
        if (!Files.isRegularFile(path)) {
            return;
        }
        var bytes = Files.readAllBytes(path);
        certificadoRepository.save(Certificado.of(1L, bytes, certificadoSenha));
        try {
            var ks = java.security.KeyStore.getInstance("PKCS12");
            ks.load(new java.io.ByteArrayInputStream(bytes), certificadoSenha.toCharArray());
            var alias = ks.aliases().nextElement();
            var x509 = (java.security.cert.X509Certificate) ks.getCertificate(alias);
            var meta = CertificadoLeituraService.parseSubject(x509.getSubjectX500Principal().getName());
            if (!meta.pessoaFisica()) {
                empresaRepository.findById(1L).ifPresent(e -> {
                    e.setCnpj(meta.documento());
                    if (meta.titular() != null && !meta.titular().isBlank()) {
                        e.setNome(meta.titular());
                    }
                    empresaRepository.save(e);
                });
            }
        } catch (Exception ignored) {
        }
    }
}
