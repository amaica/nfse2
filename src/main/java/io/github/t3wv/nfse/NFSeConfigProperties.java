package io.github.t3wv.nfse;

import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Configuração via {@code nfse.properties} / {@code application.properties} (classpath ou arquivo externo),
 * com fallback para variáveis de ambiente.
 *
 * <p>Precedência (maior vence): variável de ambiente → System property → arquivo properties.
 */
public class NFSeConfigProperties implements NFSeConfig {

    public static final String PROP_CERTIFICADO_PATH = "nfse.certificado.path";
    public static final String PROP_CERTIFICADO_SENHA = "nfse.certificado.senha";
    public static final String PROP_CADEIA_PATH = "nfse.cadeia-certificados.path";
    public static final String PROP_CADEIA_SENHA = "nfse.cadeia-certificados.senha";
    public static final String PROP_AMBIENTE = "nfse.ambiente";
    public static final String PROP_MUNICIPIO_IBGE = "nfse.municipio.codigo-ibge";

    public static final String ENV_CERTIFICADO_PATH = "CERTIFICADO_PATH";
    public static final String ENV_CERTIFICADO_SENHA = "CERTIFICADO_SENHA";
    public static final String ENV_CADEIA_PATH = "CADEIA_CERTIFICADOS_PATH";
    public static final String ENV_CADEIA_SENHA = "CADEIA_CERTIFICADOS_SENHA";
    public static final String ENV_PRODUCAO = "NFSE_PRODUCAO";
    public static final String SYS_CONFIG_FILE = "nfse.config";

    private final Properties properties = new Properties();
    private final String certificadoPath;
    private final String certificadoSenha;
    private final String cadeiaCertificadosPath;
    private final String cadeiaCertificadosSenha;
    private final boolean teste;
    private final String codigoMunicipioIbge;

    private KeyStore keyStoreCertificado;
    private KeyStore keyStoreCadeia;

    public NFSeConfigProperties() {
        carregarArquivos();
        this.certificadoPath = resolver(PROP_CERTIFICADO_PATH, ENV_CERTIFICADO_PATH);
        this.certificadoSenha = resolver(PROP_CERTIFICADO_SENHA, ENV_CERTIFICADO_SENHA);
        this.cadeiaCertificadosPath = resolver(PROP_CADEIA_PATH, ENV_CADEIA_PATH);
        this.cadeiaCertificadosSenha = resolver(PROP_CADEIA_SENHA, ENV_CADEIA_SENHA);
        this.teste = !isProducao(resolver(PROP_AMBIENTE, ENV_PRODUCAO));
        this.codigoMunicipioIbge = trimToNull(properties.getProperty(PROP_MUNICIPIO_IBGE));
    }

    public NFSeConfigProperties(final Properties properties) {
        this.properties.putAll(properties);
        this.certificadoPath = resolver(PROP_CERTIFICADO_PATH, ENV_CERTIFICADO_PATH);
        this.certificadoSenha = resolver(PROP_CERTIFICADO_SENHA, ENV_CERTIFICADO_SENHA);
        this.cadeiaCertificadosPath = resolver(PROP_CADEIA_PATH, ENV_CADEIA_PATH);
        this.cadeiaCertificadosSenha = resolver(PROP_CADEIA_SENHA, ENV_CADEIA_SENHA);
        this.teste = !isProducao(resolver(PROP_AMBIENTE, ENV_PRODUCAO));
        this.codigoMunicipioIbge = trimToNull(this.properties.getProperty(PROP_MUNICIPIO_IBGE));
    }

    /** Código IBGE de 7 dígitos (opcional, para uso na aplicação). */
    public String getCodigoMunicipioIbge() {
        return codigoMunicipioIbge;
    }

    public String getCertificadoPath() {
        return certificadoPath;
    }

    public String getCadeiaCertificadosPath() {
        return cadeiaCertificadosPath;
    }

    private void carregarArquivos() {
        final var configPath = trimToNull(System.getProperty(SYS_CONFIG_FILE));
        if (configPath != null) {
            carregarPath(Path.of(configPath));
            return;
        }
        carregarClasspath("nfse.properties");
        carregarClasspath("application.properties");
        // Eclipse/STS: nfse.properties em src/main/resources nao vai ao classpath ate Maven build
        final var devProps = Path.of(System.getProperty("user.dir", "."), "src/main/resources/nfse.properties");
        if (Files.isRegularFile(devProps)) {
            carregarPath(devProps);
        }
    }

    private void carregarClasspath(final String nome) {
        try (InputStream in = NFSeConfigProperties.class.getClassLoader().getResourceAsStream(nome)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler " + nome + " do classpath", e);
        }
    }

    private void carregarPath(final Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Arquivo de configuracao nao encontrado: " + path);
        }
        try (InputStream in = new FileInputStream(path.toFile())) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler " + path, e);
        }
    }

    private String resolver(final String propKey, final String envKey) {
        final var env = trimToNull(System.getenv(envKey));
        if (env != null) {
            return env;
        }
        final var sys = trimToNull(System.getProperty(propKey));
        if (sys != null) {
            return sys;
        }
        return trimToNull(properties.getProperty(propKey));
    }

    private static boolean isProducao(final String valor) {
        if (valor == null) {
            return false;
        }
        return switch (valor.trim().toLowerCase()) {
            case "true", "1", "producao", "produção", "prod" -> true;
            case "false", "0", "homologacao", "homologação", "hml", "teste" -> false;
            default -> Boolean.parseBoolean(valor);
        };
    }

    private static String trimToNull(final String value) {
        if (value == null) {
            return null;
        }
        final var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String getCertificadoSenha() {
        return certificadoSenha;
    }

    @Override
    public String getCadeiaCertificadosSenha() {
        return cadeiaCertificadosSenha;
    }

    @Override
    public KeyStore getKeyStoreCertificado() throws KeyStoreException {
        if (this.keyStoreCertificado == null && StringUtils.isNotBlank(this.certificadoPath)) {
            this.keyStoreCertificado = KeyStore.getInstance("PKCS12");
            try (InputStream certificadoStream = new FileInputStream(this.certificadoPath)) {
                this.keyStoreCertificado.load(certificadoStream, this.getCertificadoSenha().toCharArray());
            } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
                this.keyStoreCertificado = null;
                throw new KeyStoreException("Nao foi possivel carregar o certificado A1: " + this.certificadoPath, e);
            }
        }
        if (this.keyStoreCertificado == null) {
            throw new KeyStoreException(PROP_CERTIFICADO_PATH + " nao definido (properties ou " + ENV_CERTIFICADO_PATH + ")");
        }
        return this.keyStoreCertificado;
    }

    @Override
    public KeyStore getKeyStoreCadeia() throws KeyStoreException {
        if (this.keyStoreCadeia == null && StringUtils.isNotBlank(this.cadeiaCertificadosPath)) {
            this.keyStoreCadeia = KeyStore.getInstance("JKS");
            try (InputStream cadeia = new FileInputStream(this.cadeiaCertificadosPath)) {
                this.keyStoreCadeia.load(cadeia, this.getCadeiaCertificadosSenha().toCharArray());
            } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
                this.keyStoreCadeia = null;
                throw new KeyStoreException("Nao foi possivel carregar a cadeia: " + this.cadeiaCertificadosPath, e);
            }
        }
        if (this.keyStoreCadeia == null) {
            throw new KeyStoreException(PROP_CADEIA_PATH + " nao definido (properties ou " + ENV_CADEIA_PATH + ")");
        }
        return this.keyStoreCadeia;
    }

    @Override
    public boolean isTeste() {
        return teste;
    }
}
