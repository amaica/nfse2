package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Certificado;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
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
        empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo PFX obrigatorio");
        }
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha do PFX obrigatoria");
        }
        var bytes = arquivo.getBytes();
        var meta = validarPfx(bytes, senha.trim());
        var cert = Certificado.of(empresaId, bytes, senha.trim());
        repository.save(cert);
        sincronizarEmpresaComCertificado(empresaId, meta);

        var body = new LinkedHashMap<String, Object>();
        body.put("ok", true);
        body.put("empresaId", empresaId);
        body.put("tamanhoBytes", arquivo.getSize());
        body.put("documento", meta.documento());
        body.put("titular", meta.titular());
        body.put("pessoaFisica", meta.pessoaFisica() || meta.eCpf());
        body.put("podeEmitir", true);
        if (meta.validade() != null) {
            body.put("validade", meta.validade().toString());
        }
        return body;
    }

    private CertificadoLeituraService.Metadados validarPfx(byte[] bytes, String senha) {
        try {
            var ks = KeyStore.getInstance("PKCS12");
            ks.load(new ByteArrayInputStream(bytes), senha.toCharArray());
            var alias = ks.aliases().nextElement();
            var x509 = (X509Certificate) ks.getCertificate(alias);
            var meta = CertificadoLeituraService.parseSubject(x509.getSubjectX500Principal().getName());
            var validade = x509.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return new CertificadoLeituraService.Metadados(
                    meta.documento(), meta.pessoaFisica(), meta.titular(), validade,
                    meta.eCpf(), meta.cpfTitular());
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Senha do PFX invalida ou arquivo corrompido");
        } catch (Exception e) {
            throw new IllegalArgumentException("PFX invalido: " + e.getMessage());
        }
    }

    private void sincronizarEmpresaComCertificado(Long empresaId, CertificadoLeituraService.Metadados meta) {
        empresaRepository.findById(empresaId).ifPresent(e -> {
            var docAtual = e.getCnpj() != null ? e.getCnpj().replaceAll("\\D", "") : "";
            if (meta.eCpf() && meta.cpfTitular() != null) {
                if (docAtual.isEmpty()) {
                    e.setCnpj(meta.cpfTitular());
                }
            } else if (!meta.pessoaFisica() && docAtual.length() != 11) {
                e.setCnpj(meta.documento());
            }
            if (meta.titular() != null && !meta.titular().isBlank()) {
                e.setNome(meta.titular());
            }
            empresaRepository.save(e);
        });
    }

    public boolean possuiCertificado(Long empresaId) {
        return repository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).isPresent();
    }
}
