package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Certificado;
import br.com.synki.nfse.portal.domain.ConfiguracaoNfse;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import io.github.t3wv.nfse.NFSeConfig;
import io.github.t3wv.nfse.nacional.WSFacade;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Map;

@Service
public class NfseLibService {

    private final CertificadoRepository certificadoRepository;
    private final ConfiguracaoNfseRepository configuracaoRepository;

    public NfseLibService(CertificadoRepository certificadoRepository, ConfiguracaoNfseRepository configuracaoRepository) {
        this.certificadoRepository = certificadoRepository;
        this.configuracaoRepository = configuracaoRepository;
    }

    public WSFacade facadeForEmpresa(Long empresaId) throws Exception {
        return new WSFacade(configForEmpresa(empresaId));
    }

    public ConfiguracaoNfse configOrThrow(Long empresaId) {
        return configuracaoRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new IllegalStateException("Configure NFS-e da empresa antes de emitir"));
    }

    public Object consultaConvenio(Long empresaId, String codigoMunicipio) throws Exception {
        return facadeForEmpresa(empresaId).consultaConvenioMunicipio(codigoMunicipio);
    }

    public BigDecimal consultaAliquota(Long empresaId, String codigoMunicipio, String codigoServico) throws Exception {
        return facadeForEmpresa(empresaId).consultaAliquotaMunicipioServicoCompetencia(codigoMunicipio, codigoServico);
    }

    public Map.Entry<Integer, Object> buscarPorChave(Long empresaId, String chave) throws Exception {
        return facadeForEmpresa(empresaId).buscarNFSeByChaveAcesso(chave);
    }

    public byte[] downloadPdf(Long empresaId, String chave) throws Exception {
        return facadeForEmpresa(empresaId).downloadNotaPdf(chave);
    }

    public String downloadXml(Long empresaId, String chave) throws Exception {
        return facadeForEmpresa(empresaId).downloadNotaXml(chave);
    }

    public Map.Entry<Integer, Object> emitir(Long empresaId, io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalDPS dps) throws Exception {
        return facadeForEmpresa(empresaId).emitirNFSe(dps);
    }

    public boolean temCertificado(Long empresaId) {
        return certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).isPresent();
    }

    private NFSeConfig configForEmpresa(Long empresaId) throws Exception {
        var config = configuracaoRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new IllegalStateException("Configuracao NFS-e nao encontrada"));
        var cert = certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElseThrow(() -> new IllegalStateException("Certificado A1 nao cadastrado"));

        Path pfx = Files.createTempFile("nfse-cert-" + empresaId + "-", ".pfx");
        Path cacerts = Path.of(System.getProperty("java.io.tmpdir"), "nfse-cacerts-" + empresaId + ".jks");
        try {
            Files.write(pfx, cert.getArquivo());
            if (!Files.exists(cacerts) || Files.size(cacerts) == 0) {
                var bootstrap = tempConfig(pfx, cert.getSenha(), cacerts, config.isProducao());
                Files.write(cacerts, io.github.t3wv.nfse.utils.NFSeCadeiaCertificados.geraCadeiaCertificados(bootstrap));
            }
            return tempConfig(pfx, cert.getSenha(), cacerts, config.isProducao());
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao preparar certificado", e);
        }
    }

    private static NFSeConfig tempConfig(Path pfx, String senha, Path cacerts, boolean producao) {
        return new NFSeConfig() {
            @Override
            public String getCertificadoSenha() { return senha; }
            @Override
            public String getCadeiaCertificadosSenha() { return "senha"; }
            @Override
            public KeyStore getKeyStoreCertificado() throws KeyStoreException {
                try {
                    var ks = KeyStore.getInstance("PKCS12");
                    try (var in = Files.newInputStream(pfx)) {
                        ks.load(in, senha.toCharArray());
                    }
                    return ks;
                } catch (Exception e) {
                    throw new KeyStoreException(e);
                }
            }
            @Override
            public KeyStore getKeyStoreCadeia() throws KeyStoreException {
                try {
                    var ks = KeyStore.getInstance("JKS");
                    try (var in = Files.newInputStream(cacerts)) {
                        ks.load(in, "senha".toCharArray());
                    }
                    return ks;
                } catch (Exception e) {
                    throw new KeyStoreException(e);
                }
            }
            @Override
            public boolean isTeste() { return !producao; }
        };
    }
}
