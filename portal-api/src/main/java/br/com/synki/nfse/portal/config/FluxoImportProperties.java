package br.com.synki.nfse.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nfse.fluxo")
public record FluxoImportProperties(
        boolean enabled,
        boolean importOnStartup,
        String url,
        String username,
        String password,
        /** Empresa legada (fluxo) que recebe clientes e tributação compartilhados (ex.: Clovis = 4). */
        long clienteEmpresaFluxoId,
        /** Empresa do portal que também recebe cadastros compartilhados (ex.: demo Esatta = 1). 0 = desligado. */
        long replicarCadastrosEmpresaDemoId,
        String defaultSenhaUsuario
) {
    public FluxoImportProperties {
        if (defaultSenhaUsuario == null || defaultSenhaUsuario.isBlank()) {
            defaultSenhaUsuario = "fluxo123";
        }
        if (clienteEmpresaFluxoId <= 0) {
            clienteEmpresaFluxoId = 4L;
        }
        if (replicarCadastrosEmpresaDemoId < 0) {
            replicarCadastrosEmpresaDemoId = 1L;
        }
    }
}
