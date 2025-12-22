package io.github.t3wv.nacional.transformers;

import io.github.t3wv.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSTpOper;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalInfoIBSCBSTipoChaveDFeTransformer implements Transform<NFSeSefinNacionalInfoIBSCBSTpOper>{
    @Override
    public NFSeSefinNacionalInfoIBSCBSTpOper read(String value) {
        return NFSeSefinNacionalInfoIBSCBSTpOper.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalInfoIBSCBSTpOper value) {
        return value.getCodigo();
    }
}

