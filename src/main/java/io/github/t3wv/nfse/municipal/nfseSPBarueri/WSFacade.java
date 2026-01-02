package io.github.t3wv.nfse.municipal.nfseSPBarueri;

import io.github.t3wv.nfse.NFSeConfig;
import io.github.t3wv.nfse.municipal.nfseSPBarueri.classes.*;

public class WSFacade {
    private final WSRPS wSRPS;

    public WSFacade(NFSeConfig config) {
        this.wSRPS = new WSRPS(config);
    }

    public NFSeBarueriLoteEnviarArquivoResponse loteEnviarArquivo(NFSeBarueriLoteEnviarArquivoRequest request) throws Exception {
        return wSRPS.loteEnviarArquivo(request);
    }
    public NFSeBarueriLoteStatusArquivoResponse loteStatusArquivo(final NFSeBarueriLoteStatusArquivoRequest request) throws Exception {
        return wSRPS.loteStatusArquivo(request);
    }
    public NFSeBarueriLoteBaixarArquivoResponse loteBaixarArquivo(NFSeBarueriLoteBaixarArquivoRequest request) throws Exception {
        return wSRPS.loteBaixarArquivo(request);
    }
}
