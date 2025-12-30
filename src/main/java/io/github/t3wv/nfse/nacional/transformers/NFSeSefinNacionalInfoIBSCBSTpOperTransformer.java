package io.github.t3wv.nfse.nacional.transformers;

import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSTpOper;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalInfoIBSCBSTpOperTransformer implements Transform<NFSeSefinNacionalInfoIBSCBSTpOper>{
    @Override
    public NFSeSefinNacionalInfoIBSCBSTpOper read(String value) {
        return NFSeSefinNacionalInfoIBSCBSTpOper.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalInfoIBSCBSTpOper value) {
        return value.getCodigo();
    }
}

