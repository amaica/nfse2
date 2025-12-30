package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;

public class NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGTribRegular {
    @Element(name="CSTReg")
    protected String cSTReg;
    @Element(name="cClassTribReg")
    protected String cClassTribReg;

    public String getcSTReg() {
        return cSTReg;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGTribRegular setcSTReg(String cSTReg) {
        this.cSTReg = cSTReg;
        return this;
    }

    public String getcClassTribReg() {
        return cClassTribReg;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGTribRegular setcClassTribReg(String cClassTribReg) {
        this.cClassTribReg = cClassTribReg;
        return this;
    }


}
