package io.github.t3wv.nfse.nacional.transformers;

import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalLocacaoSublocacaoCategoriaServico;
import org.simpleframework.xml.transform.Transform;

public class NFSeSefinNacionalLocacaoSublocacaoCategoriaServicoTransformer implements Transform<NFSeSefinNacionalLocacaoSublocacaoCategoriaServico>{
    @Override
    public NFSeSefinNacionalLocacaoSublocacaoCategoriaServico read(String value) {
        return NFSeSefinNacionalLocacaoSublocacaoCategoriaServico.valueOfCodigo(value);
    }

    @Override
    public String write(NFSeSefinNacionalLocacaoSublocacaoCategoriaServico value) {
        return value.getCodigo();
    }
}

