package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.web.dto.EmissaoCompletaRequest;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalNFSePostResponseErro;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalNFSePostResponseSucesso;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class EmissaoDpsService {

    private final NfseLibService nfseLibService;
    private final DpsMontadorService dpsMontadorService;

    public EmissaoDpsService(NfseLibService nfseLibService, DpsMontadorService dpsMontadorService) {
        this.nfseLibService = nfseLibService;
        this.dpsMontadorService = dpsMontadorService;
    }

    public NFSeSefinNacionalNFSePostResponseSucesso emitir(Long empresaId, EmissaoCompletaRequest req) throws Exception {
        var cfg = nfseLibService.configOrThrow(empresaId);
        var dps = dpsMontadorService.montar(empresaId, cfg, req);
        var result = nfseLibService.facadeForEmpresa(empresaId).emitirNFSe(dps);
        if (result.getKey() == java.net.HttpURLConnection.HTTP_CREATED) {
            return (NFSeSefinNacionalNFSePostResponseSucesso) result.getValue();
        }
        var erro = (NFSeSefinNacionalNFSePostResponseErro) result.getValue();
        var mensagens = erro.getErros() == null ? "" : erro.getErros().stream()
                .map(e -> e.getCodigo() + " - " + e.getDescricao()
                        + (e.getComplemento() != null && !e.getComplemento().isBlank() ? " (" + e.getComplemento() + ")" : ""))
                .collect(Collectors.joining("; "));
        throw new IllegalArgumentException(mensagens.isBlank() ? "SEFIN rejeitou a emissao" : mensagens);
    }
}
