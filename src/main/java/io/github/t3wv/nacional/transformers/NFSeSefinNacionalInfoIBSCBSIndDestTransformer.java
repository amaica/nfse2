package io.github.t3wv.nacional.transformers;

import io.github.t3wv.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSIndDest;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalInfoIBSCBSIndDestTransformer implements Transform<NFSeSefinNacionalInfoIBSCBSIndDest>{
    @Override
    public NFSeSefinNacionalInfoIBSCBSIndDest read(String value) {
        return NFSeSefinNacionalInfoIBSCBSIndDest.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalInfoIBSCBSIndDest value) {
        return value.getCodigo();
    }
}

