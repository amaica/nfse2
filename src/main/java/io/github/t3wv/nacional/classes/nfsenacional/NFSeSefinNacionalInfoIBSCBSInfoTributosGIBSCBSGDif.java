package io.github.t3wv.nacional.classes.nfsenacional;

import org.simpleframework.xml.Element;

import java.math.BigDecimal;

public class NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif {
    @Element(name="pDifUF")
    protected BigDecimal pDifUF;
    @Element(name="pDifMun")
    protected BigDecimal pDifMun;
    @Element(name="pDifCBS")
    protected BigDecimal pDifCBS;

    public BigDecimal getpDifUF() {
        return pDifUF;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif setpDifUF(BigDecimal pDifUF) {
        this.pDifUF = pDifUF;
        return this;
    }

    public BigDecimal getpDifMun() {
        return pDifMun;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif setpDifMun(BigDecimal pDifMun) {
        this.pDifMun = pDifMun;
        return this;
    }

    public BigDecimal getpDifCBS() {
        return pDifCBS;
    }

    public NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif setpDifCBS(BigDecimal pDifCBS) {
        this.pDifCBS = pDifCBS;
        return this;
    }

    @Override
    public String toString() {
        return "NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBSGDif{" +
                "pDifUF=" + pDifUF +
                ", pDifMun=" + pDifMun +
                ", pDifCBS=" + pDifCBS +
                '}';
    }
}
