package io.github.t3wv.nfse;

import io.github.t3wv.nfse.nacional.WSFacade;
import io.github.t3wv.nfse.utils.NFSeCadeiaCertificados;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Executável de demonstração: gera cacerts (se necessário) e consulta a API nacional.
 */
public final class DemoRun {

    private DemoRun() {
    }

    public static void main(final String[] args) throws Exception {
        final var config = new NFSeConfigProperties();
        validarConfig(config);
        garantirCadeiaCertificados(config);

        final var municipio = args.length > 0 && StringUtils.isNotBlank(args[0])
            ? args[0]
            : StringUtils.defaultIfBlank(config.getCodigoMunicipioIbge(), "4216602");
        final var servico = args.length > 1 ? args[1] : "01.01.01.000";

        System.out.println("Ambiente: " + (config.isTeste() ? "HOMOLOGACAO" : "PRODUCAO"));
        System.out.println("Certificado: " + config.getCertificadoPath());
        System.out.println();

        final var facade = new WSFacade(config);

        System.out.println("--- Convenio municipio " + municipio + " ---");
        final var convenio = facade.consultaConvenioMunicipio(municipio);
        System.out.println(convenio);

        System.out.println();
        System.out.println("--- Aliquota servico " + servico + " ---");
        try {
            final var aliquota = facade.consultaAliquotaMunicipioServicoCompetencia(municipio, servico);
            System.out.println("Aliquota: " + aliquota);
        } catch (final Exception e) {
            System.out.println("Aliquota indisponivel neste ambiente: " + e.getMessage());
        }

        System.out.println();
        System.out.println("OK - biblioteca operacional.");
    }

    private static void validarConfig(final NFSeConfigProperties config) {
        if (StringUtils.isBlank(config.getCertificadoPath())) {
            System.err.println("""
                Configuracao NFS-e nao encontrada.

                1) Copie o exemplo:
                   cp src/main/resources/nfse.properties.example src/main/resources/nfse.properties

                2) Edite certificado, senha e nfse.municipio.codigo-ibge

                3) No Spring Tool Suite: Run As -> Java Application (NAO use Spring Boot App)
                   Classe: io.github.t3wv.nfse.DemoRun
                   Ou importe a launch: DemoRun.launch
                """);
            System.exit(1);
        }
    }

    private static void garantirCadeiaCertificados(final NFSeConfigProperties config) throws Exception {
        final var path = Path.of(config.getCadeiaCertificadosPath());
        if (Files.exists(path) && Files.size(path) > 0) {
            return;
        }
        System.out.println("Gerando cadeia de certificados em " + path + " ...");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, NFSeCadeiaCertificados.geraCadeiaCertificados(config));
    }
}
