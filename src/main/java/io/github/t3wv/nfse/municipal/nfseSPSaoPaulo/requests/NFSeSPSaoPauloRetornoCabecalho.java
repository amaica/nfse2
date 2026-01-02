package io.github.t3wv.nfse.municipal.nfseSPSaoPaulo.requests;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class NFSeSPSaoPauloRetornoCabecalho {

    @Element(name = "Sucesso")
    private Boolean sucesso;

    @Element(name = "InformacoesLote", required = false)
    private NFSeSPSaoPauloRetornoEnvioRPSInformacoesLote informacoesLote;

    @Attribute(name = "Versao")
    private String Versao;

}
