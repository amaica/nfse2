package io.github.t3wv.nacional.transformers;

import io.github.t3wv.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSFinNFSe;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalInfoIBSCBSFinNFSeTransformer implements Transform<NFSeSefinNacionalInfoIBSCBSFinNFSe>{
    @Override
    public NFSeSefinNacionalInfoIBSCBSFinNFSe read(String value) {
        return NFSeSefinNacionalInfoIBSCBSFinNFSe.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalInfoIBSCBSFinNFSe value) {
        return value.getCodigo();
    }
}

