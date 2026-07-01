package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.ConfiguracaoDocumentoRepository;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFeConfig;
import com.fincatto.documentofiscal.nfe400.webservices.WSFacade;
import com.fincatto.documentofiscal.utils.DFCadeiaCertificados;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;

@Service
public class NfeLibService {

    private final CertificadoRepository certificadoRepository;
    private final ConfiguracaoNfseRepository configuracaoRepository;
    private final ConfiguracaoDocumentoRepository documentoRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaEnderecoRepository enderecoRepository;

    public NfeLibService(
            CertificadoRepository certificadoRepository,
            ConfiguracaoNfseRepository configuracaoRepository,
            ConfiguracaoDocumentoRepository documentoRepository,
            EmpresaRepository empresaRepository,
            EmpresaEnderecoRepository enderecoRepository) {
        this.certificadoRepository = certificadoRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.documentoRepository = documentoRepository;
        this.empresaRepository = empresaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    public WSFacade facadeForEmpresa(Long empresaId, DFModelo modelo) throws Exception {
        return new WSFacade(configForEmpresa(empresaId, modelo));
    }

    public WSFacade facadeForEmpresa(Long empresaId) throws Exception {
        return facadeForEmpresa(empresaId, DFModelo.NFE);
    }

    public boolean temCertificado(Long empresaId) {
        return certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).isPresent();
    }

    public Empresa empresaOrThrow(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
    }

    public EmpresaEndereco enderecoPrincipalOrThrow(Long empresaId) {
        return enderecoRepository.findByEmpresaIdAndPrincipalTrue(empresaId)
                .or(() -> enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Cadastre ao menos um endereco para a empresa"));
    }

    public EmpresaEndereco enderecoOrThrow(Long empresaId, Long enderecoId) {
        return enderecoRepository.findById(enderecoId)
                .filter(e -> e.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Endereco nao encontrado"));
    }

    public ConfiguracaoDocumento documentoOrThrow(Long empresaId, String tipo) {
        return documentoRepository.findByEmpresaIdAndTipo(empresaId, tipo)
                .orElseThrow(() -> new IllegalStateException("Configure " + tipo + " da empresa"));
    }

    public DFAmbiente ambiente(Long empresaId) {
        return configuracaoRepository.findByEmpresaId(empresaId)
                .map(c -> c.isProducao() ? DFAmbiente.PRODUCAO : DFAmbiente.HOMOLOGACAO)
                .orElse(DFAmbiente.HOMOLOGACAO);
    }

    public DFUnidadeFederativa ufEmitente(Long empresaId, Long enderecoId) {
        var end = enderecoId != null ? enderecoOrThrow(empresaId, enderecoId) : enderecoPrincipalOrThrow(empresaId);
        var uf = end.getUf();
        if (uf == null || uf.isBlank()) {
            throw new IllegalStateException("UF do endereco emitente obrigatoria");
        }
        return DFUnidadeFederativa.valueOfCodigo(uf.trim().toUpperCase());
    }

    private NFeConfig configForEmpresa(Long empresaId, DFModelo modelo) throws Exception {
        var cert = certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .orElseThrow(() -> new IllegalStateException("Certificado A1 nao cadastrado"));
        var ambiente = ambiente(empresaId);
        var uf = ufEmitente(empresaId, null);

        Path pfx = Files.createTempFile("nfe-cert-" + empresaId + "-", ".pfx");
        Path cacerts = Path.of(System.getProperty("java.io.tmpdir"), "nfe-cacerts-" + empresaId + ".jks");
        try {
            Files.write(pfx, cert.getArquivo());
            if (!Files.exists(cacerts) || Files.size(cacerts) == 0) {
                Files.write(cacerts, DFCadeiaCertificados.geraCadeiaCertificados(ambiente, "senha"));
            }
            return tempConfig(pfx, cert.getSenha(), cacerts, ambiente, uf, modelo);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao preparar certificado NF-e", e);
        }
    }

    private static NFeConfig tempConfig(
            Path pfx, String senha, Path cacerts, DFAmbiente ambiente, DFUnidadeFederativa uf, DFModelo modelo) {
        return new NFeConfig() {
            @Override
            public DFUnidadeFederativa getCUF() {
                return uf;
            }

            @Override
            public DFAmbiente getAmbiente() {
                return ambiente;
            }

            @Override
            public DFModelo getModelo() {
                return modelo;
            }

            @Override
            public String getCertificadoSenha() {
                return senha;
            }

            @Override
            public String getCadeiaCertificadosSenha() {
                return "senha";
            }

            @Override
            public KeyStore getCertificadoKeyStore() throws KeyStoreException {
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
            public KeyStore getCadeiaCertificadosKeyStore() throws KeyStoreException {
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
        };
    }
}
