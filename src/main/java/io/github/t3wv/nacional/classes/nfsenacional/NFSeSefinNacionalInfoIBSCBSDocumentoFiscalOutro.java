package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;

public class NFSeSefinNacionalInfoIBSCBSDocumentoFiscalOutro extends NFSeSefinNacionalInfoIBSCBSDocumento {
    @Element(name = "cMunDocFiscal")
    protected String cMunDocFiscal;
    @Element(name = "nDocFiscal")
    protected String nDocFiscal;
    @Element(name = "xDocFiscal")
    protected String xDocFiscal;

    public String getcMunDocFiscal() {
        return cMunDocFiscal;
    }

    public NFSeSefinNacionalInfoIBSCBSDocumentoFiscalOutro setcMunDocFiscal(String cMunDocFiscal) {
        this.cMunDocFiscal = cMunDocFiscal;
        return this;
    }

    public String getnDocFiscal() {
        return nDocFiscal;
    }

    public NFSeSefinNacionalInfoIBSCBSDocumentoFiscalOutro setnDocFiscal(String nDocFiscal) {
        this.nDocFiscal = nDocFiscal;
        return this;
    }

    public String getxDocFiscal() {
        return xDocFiscal;
    }

    public NFSeSefinNacionalInfoIBSCBSDocumentoFiscalOutro setxDocFiscal(String xDocFiscal) {
        this.xDocFiscal = xDocFiscal;
        return this;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalInfoIBSCBSDocumentoFiscalOutro{" +
                "cMunDocFiscal='" + cMunDocFiscal + '\'' +
                ", nDocFiscal='" + nDocFiscal + '\'' +
                ", xDocFiscal='" + xDocFiscal + '\'' +
                '}';
    }
}
