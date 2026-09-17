package br.com.synki.nfse.portal.web.dto.nfe;

public record NfeDestinatarioRequest(
        String nome,
        String documento,
        String email,
        String inscricaoEstadual,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String municipio,
        String uf,
        String cep,
        String codigoMunicipioIbge
) {
    public NfeDestinatarioRequest(String nome, String documento, String email, String inscricaoEstadual) {
        this(nome, documento, email, inscricaoEstadual, null, null, null, null, null, null, null, null);
    }
}
