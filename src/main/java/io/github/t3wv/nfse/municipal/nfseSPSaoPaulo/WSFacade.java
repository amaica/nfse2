package io.github.t3wv.nfse.municipal.nfseSPSaoPaulo;

import io.github.t3wv.nfse.NFSeConfig;
import io.github.t3wv.nfse.municipal.nfseSPSaoPaulo.requests.*;

public class WSFacade {
    private final WSLoteNFe wsLoteNFe;

    public WSFacade(NFSeConfig config) {
        this.wsLoteNFe = new WSLoteNFe(config);
    }

    public NFSeSPSaoPauloRetornoEnvioLoteRPS enviarTesteLoteRPS(NFSeSPSaoPauloRequestEnvioRPS pedidoEnvioRPS) throws Exception{
        return wsLoteNFe.enviarTesteLoteRPS(pedidoEnvioRPS);
    }
    public NFSeSPSaoPauloRetornoEnvioLoteRPS enviarRPS(NFSeSPSaoPauloRequestEnvioRPS pedidoEnvioRPS) throws Exception{
        return wsLoteNFe.enviarRPS(pedidoEnvioRPS);
    }
    public NFSeSPSaoPauloRetornoConsultaNFe enviarPedidoConsultaNFe(NFSeSPSaoPauloRequestConsultaNFe pedidoConsultaNFe) throws Exception{
        return wsLoteNFe.enviarPedidoConsultaNFe(pedidoConsultaNFe);
    }
    public NFSeSPSaoPauloRetornoCancelamentoNFe enviarPedidoCancelamentoNFe(NFSeSPSaoPauloRequestCancelamentoNFe pedidoCancelamentoNFe) throws Exception{
        return wsLoteNFe.enviarPedidoCancelamentoNFe(pedidoCancelamentoNFe);
    }


}
