package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;

public class NFSeSefinNacionalInfoIBSCBSDocumentoOutro extends NFSeSefinNacionalInfoIBSCBSDocumento {
    @Element(name = "nDoc")
    protected String nDoc;
    @Element(name = "xDoc")
    protected String xDoc;

    public String getnDoc() {
        return nDoc;
    }

    public NFSeSefinNacionalInfoIBSCBSDocumentoOutro setnDoc(String nDoc) {
        this.nDoc = nDoc;
        return this;
    }

    public String getxDoc() {
        return xDoc;
    }

    public NFSeSefinNacionalInfoIBSCBSDocumentoOutro setxDoc(String xDoc) {
        this.xDoc = xDoc;
        return this;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalInfoIBSCBSDocumentoOutro{" +
                "nDoc='" + nDoc + '\'' +
                ", xDoc='" + xDoc + '\'' +
                '}';
    }
}
