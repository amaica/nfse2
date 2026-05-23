package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Certificado;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CertificadoService {

    private final CertificadoRepository repository;
    private final EmpresaRepository empresaRepository;

    public CertificadoService(CertificadoRepository repository, EmpresaRepository empresaRepository) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
    }

    public Map<String, Object> salvar(Long empresaId, MultipartFile arquivo, String senha) throws Exception {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo PFX obrigatorio");
        }
        var bytes = arquivo.getBytes();
        var cert = Certificado.of(empresaId, bytes, senha);
        repository.save(cert);
        sincronizarCnpjEmpresa(empresaId, bytes, senha);
        return Map.of("ok", true, "empresaId", empresaId, "tamanhoBytes", arquivo.getSize());
    }

    private void sincronizarCnpjEmpresa(Long empresaId, byte[] pfx, String senha) {
        try {
            var ks = java.security.KeyStore.getInstance("PKCS12");
            ks.load(new java.io.ByteArrayInputStream(pfx), senha.toCharArray());
            var alias = ks.aliases().nextElement();
            var x509 = (java.security.cert.X509Certificate) ks.getCertificate(alias);
            var meta = CertificadoLeituraService.parseSubject(x509.getSubjectX500Principal().getName());
            if (!meta.pessoaFisica()) {
                empresaRepository.findById(empresaId).ifPresent(e -> {
                    e.setCnpj(meta.documento());
                    empresaRepository.save(e);
                });
            }
        } catch (Exception ignored) {
        }
    }

    public boolean possuiCertificado(Long empresaId) {
        return repository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).isPresent();
    }
}
