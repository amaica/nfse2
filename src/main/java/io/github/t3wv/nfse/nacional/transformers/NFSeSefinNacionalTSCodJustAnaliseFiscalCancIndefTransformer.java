package io.github.t3wv.nfse.nacional.transformers;

import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalTSCodJustAnaliseFiscalCancIndef;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalTSCodJustAnaliseFiscalCancIndefTransformer implements Transform<NFSeSefinNacionalTSCodJustAnaliseFiscalCancIndef>{
    @Override
    public NFSeSefinNacionalTSCodJustAnaliseFiscalCancIndef read(String value) {
        return NFSeSefinNacionalTSCodJustAnaliseFiscalCancIndef.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalTSCodJustAnaliseFiscalCancIndef value) {
        return value.getCodigo();
    }
}

