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
        String defaultSenhaUsuario,
        /** E-mail do operador da plataforma que recebe acesso a todos os emitentes importados. */
        String adminPlataformaEmail,
        /** Conta unica para os emitentes importados (ex.: Cereais Werlang). Vazio = uma conta por emitente. */
        String contaNome,
        /** Dono da conta (gestor). Operadores respondem a este usuario. */
        String ownerEmail,
        String ownerNome,
        /** Operadores no formato email|Nome;email|Nome */
        String operadores
) {
    public FluxoImportProperties {
        if (enabled && (defaultSenhaUsuario == null || defaultSenhaUsuario.isBlank())) {
            throw new IllegalStateException(
                    "FLUXO_DEFAULT_SENHA e obrigatorio quando FLUXO_IMPORT_ENABLED=true "
                            + "(evita criar usuarios importados com senha padrao previsivel).");
        }
        if (adminPlataformaEmail == null || adminPlataformaEmail.isBlank()) {
            adminPlataformaEmail = "admin@synki.demo";
        }
        if (clienteEmpresaFluxoId <= 0) {
            clienteEmpresaFluxoId = 4L;
        }
        if (replicarCadastrosEmpresaDemoId < 0) {
            replicarCadastrosEmpresaDemoId = 1L;
        }
        if (contaNome == null) {
            contaNome = "";
        }
        if (ownerEmail == null) {
            ownerEmail = "";
        }
        if (ownerNome == null) {
            ownerNome = "";
        }
        if (operadores == null) {
            operadores = "";
        }
    }
}
