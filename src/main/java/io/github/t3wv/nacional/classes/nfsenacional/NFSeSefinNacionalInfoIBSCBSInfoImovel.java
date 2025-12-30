package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "imovel")
public class NFSeSefinNacionalInfoIBSCBSInfoImovel {
    @Element(name="inscImobFisc", required = false)
    protected String inscImobFisc;
    @Element(name="cCIB", required = false)
    protected String cCIB;
    @Element(name="end", required = false)
    protected NFSeSefinNacionalEnderObraEvento end;

    public String getInscImobFisc() {
        return inscImobFisc;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoImovel setInscImobFisc(String inscImobFisc) {
        this.inscImobFisc = inscImobFisc;
        return this;
    }

    public String getcCIB() {
        return cCIB;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoImovel setcCIB(String cCIB) {
        this.cCIB = cCIB;
        return this;
    }

    public NFSeSefinNacionalEnderObraEvento getEnd() {
        return end;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoImovel setEnd(NFSeSefinNacionalEnderObraEvento end) {
        this.end = end;
        return this;
    }
}
