package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;

public class NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS {
    @Element(name = "CST")
    protected String CST;
    @Element(name = "cClassTrib")
    protected String cClassTrib;
    @Element(name = "cCredPres", required = false)
    protected String cCredPres;
    @Element(name = "gTribRegular", required = false)
    protected NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGTribRegular gTribRegular;
    @Element(name = "gDif", required = false)
    protected NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif gDif;

    /**
     * Código de Situação Tributária do IBS e da CBS
     * @return
     */
    public String getCST() {
        return CST;
    }

    /**
     * Código de Situação Tributária do IBS e da CBS
     * @param CST
     * @return NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS
     */
    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS setCST(String CST) {
        this.CST = CST;
        return this;
    }

    /**
     * Código de Classificação Tributária do IBS e da CBS
     * @return
     */
    public String getcClassTrib() {
        return cClassTrib;
    }

    /**
     * Código de Classificação Tributária do IBS e da CBS
     * @param cClassTrib
     * @return NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS
     */
    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS setcClassTrib(String cClassTrib) {
        this.cClassTrib = cClassTrib;
        return this;
    }

    public String getcCredPres() {
        return cCredPres;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS setcCredPres(String cCredPres) {
        this.cCredPres = cCredPres;
        return this;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGTribRegular getgTribRegular() {
        return gTribRegular;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS setgTribRegular(NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGTribRegular gTribRegular) {
        this.gTribRegular = gTribRegular;
        return this;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif getgDif() {
        return gDif;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS setgDif(NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif gDif) {
        this.gDif = gDif;
        return this;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS{" +
                "CST='" + CST + '\'' +
                ", cClassTrib='" + cClassTrib + '\'' +
                ", cCredPres='" + cCredPres + '\'' +
                ", gTribRegular=" + gTribRegular +
                ", gDif=" + gDif +
                '}';
    }
}
