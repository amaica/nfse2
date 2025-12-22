package io.github.t3wv.nacional.transformers;

import io.github.t3wv.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSTpEnteGov;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalInfoIBSCBSTpEnteGovTransformer implements Transform<NFSeSefinNacionalInfoIBSCBSTpEnteGov>{
    @Override
    public NFSeSefinNacionalInfoIBSCBSTpEnteGov read(String value) {
        return NFSeSefinNacionalInfoIBSCBSTpEnteGov.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalInfoIBSCBSTpEnteGov value) {
        return value.getCodigo();
    }
}

