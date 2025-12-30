package io.github.t3wv.nfse.nacional.transformers;

import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalBeneficioMunicipalTipoBeneficio;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalBeneficioMunicipalTipoBeneficioTransformer implements Transform<NFSeSefinNacionalBeneficioMunicipalTipoBeneficio>{
    @Override
    public NFSeSefinNacionalBeneficioMunicipalTipoBeneficio read(String value) {
        return NFSeSefinNacionalBeneficioMunicipalTipoBeneficio.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalBeneficioMunicipalTipoBeneficio value) {
        return value.getCodigo();
    }
}