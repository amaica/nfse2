package io.github.t3wv.nacional.transformers;

import io.github.t3wv.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSTpReeRepRes;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalInfoIBSCBSTpReeRepResTransformer implements Transform<NFSeSefinNacionalInfoIBSCBSTpReeRepRes>{
    @Override
    public NFSeSefinNacionalInfoIBSCBSTpReeRepRes read(String value) {
        return NFSeSefinNacionalInfoIBSCBSTpReeRepRes.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalInfoIBSCBSTpReeRepRes value) {
        return value.getCodigo();
    }
}

