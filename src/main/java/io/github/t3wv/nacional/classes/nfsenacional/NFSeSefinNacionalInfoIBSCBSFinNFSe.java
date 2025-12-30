package io.github.t3wv.nacional.classes.nfsenacional;

/**
 * Indicador da finalidade da emissão de NFS-e
 */
public enum NFSeSefinNacionalInfoIBSCBSFinNFSe {
    REGULAR("0", "NFS-e Regular");

    private final String codigo;
    private final String descricao;

    NFSeSefinNacionalInfoIBSCBSFinNFSe(final String codigo, final String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public static NFSeSefinNacionalInfoIBSCBSFinNFSe valueOfCodigo(final String codigo) {
        for (final NFSeSefinNacionalInfoIBSCBSFinNFSe tipo : NFSeSefinNacionalInfoIBSCBSFinNFSe.values()) {
            if (tipo.getCodigo().equals(codigo)) {
                return tipo;
            }
        }
        return null;
    }
}
