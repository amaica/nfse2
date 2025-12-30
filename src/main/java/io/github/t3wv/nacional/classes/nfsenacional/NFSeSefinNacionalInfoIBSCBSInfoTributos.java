package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;

public class NFSeSefinNacionalInfoIBSCBSInfoTributos {
    @Element(name = "gIBSCBS")
    protected NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS gIBSCBS;

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS getgIBSCBS() {
        return gIBSCBS;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributos setgIBSCBS(NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS gIBSCBS) {
        this.gIBSCBS = gIBSCBS;
        return this;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalInfoIBSCBSInfoTributos{" +
                "gIBSCBS=" + gIBSCBS +
                '}';
    }
}
