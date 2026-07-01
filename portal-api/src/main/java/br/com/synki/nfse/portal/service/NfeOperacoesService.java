package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.web.dto.nfe.*;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.nfe.NFTipoEmissao;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvio;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteIndicadorProcessamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNota;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NfeOperacoesService {

    private final NfeLibService nfeLibService;
    private final NfeMontadorService montadorService;
    private final NumeracaoNfeService numeracaoService;

    public NfeOperacoesService(
            NfeLibService nfeLibService,
            NfeMontadorService montadorService,
            NumeracaoNfeService numeracaoService) {
        this.nfeLibService = nfeLibService;
        this.montadorService = montadorService;
        this.numeracaoService = numeracaoService;
    }

    public Map<String, Object> statusServico(Long empresaId, DFModelo modelo) throws Exception {
        var uf = nfeLibService.ufEmitente(empresaId, null);
        var ret = nfeLibService.facadeForEmpresa(empresaId, modelo).consultaStatus(uf, modelo);
        return Map.of(
                "status", ret.getStatus(),
                "motivo", ret.getMotivo(),
                "uf", uf.name(),
                "modelo", modelo.getCodigo(),
                "ambiente", ret.getAmbiente() != null ? ret.getAmbiente().getCodigo() : "");
    }

    public Map<String, Object> consultarLote(Long empresaId, String recibo, DFModelo modelo) throws Exception {
        var ret = nfeLibService.facadeForEmpresa(empresaId, modelo).consultaLote(recibo, modelo);
        var body = new LinkedHashMap<String, Object>();
        body.put("status", ret.getStatus());
        body.put("motivo", ret.getMotivo());
        if (ret.getProtocolos() != null && !ret.getProtocolos().isEmpty()) {
            var prot = ret.getProtocolos().getFirst().getProtocoloInfo();
            if (prot != null) {
                body.put("chaveAcesso", prot.getChave());
                body.put("protocolo", prot.getNumeroProtocolo());
                body.put("statusProtocolo", prot.getStatus());
                body.put("motivoProtocolo", prot.getMotivo());
            }
        }
        return body;
    }

    public Map<String, Object> consultarNota(Long empresaId, String chave, DFModelo modelo) throws Exception {
        var ret = nfeLibService.facadeForEmpresa(empresaId, modelo).consultaNota(chave);
        var body = new LinkedHashMap<String, Object>();
        body.put("status", ret.getStatus());
        body.put("motivo", ret.getMotivo());
        if (ret.getProtocolo() != null && ret.getProtocolo().getProtocoloInfo() != null) {
            var prot = ret.getProtocolo().getProtocoloInfo();
            body.put("chaveAcesso", prot.getChave());
            body.put("protocolo", prot.getNumeroProtocolo());
            body.put("statusProtocolo", prot.getStatus());
            body.put("motivoProtocolo", prot.getMotivo());
        }
        return body;
    }

    public Map<String, Object> cancelar(Long empresaId, DFModelo modelo, NfeCancelarRequest req) throws Exception {
        var ret = nfeLibService.facadeForEmpresa(empresaId, modelo)
                .cancelaNota(req.chave(), req.protocolo(), req.motivo());
        return Map.of(
                "status", String.valueOf(ret.getRetorno().getCodigoStatusReposta()),
                "motivo", ret.getRetorno().getMotivo());
    }

    public Map<String, Object> inutilizar(Long empresaId, DFModelo modelo, NfeInutilizarRequest req) throws Exception {
        var empresa = nfeLibService.empresaOrThrow(empresaId);
        int ano = req.ano() != null ? req.ano() : java.time.Year.now().getValue() % 100;
        var ret = nfeLibService.facadeForEmpresa(empresaId, modelo).inutilizaNota(
                ano,
                empresa.getCnpj(),
                req.serie(),
                req.numeroInicial(),
                req.numeroFinal(),
                req.justificativa(),
                modelo);
        var dados = ret.getDados();
        return Map.of(
                "status", dados != null ? dados.getStatus() : "",
                "motivo", dados != null ? dados.getMotivo() : "",
                "protocolo", dados != null ? dados.getNumeroProtocolo() : "");
    }

    public Map<String, Object> cartaCorrecao(Long empresaId, DFModelo modelo, NfeCartaCorrecaoRequest req) throws Exception {
        int seq = req.sequencial() != null ? req.sequencial() : 1;
        var ret = nfeLibService.facadeForEmpresa(empresaId, modelo)
                .corrigeNota(req.chave(), req.texto(), seq);
        return Map.of(
                "status", String.valueOf(ret.getCodigoStatusReposta()),
                "motivo", ret.getMotivo());
    }

    @Transactional
    public Map<String, Object> enviarEpec(Long empresaId, DFModelo modelo, NfeContingenciaEpecRequest req) throws Exception {
        var tipo = modelo == DFModelo.NFCE ? ConfiguracaoDocumento.TIPO_NFCE : ConfiguracaoDocumento.TIPO_NFE;
        var reserva = numeracaoService.reservar(empresaId, tipo, req.enderecoId());
        var justificativa = req.justificativaContingencia() != null && !req.justificativaContingencia().isBlank()
                ? req.justificativaContingencia()
                : "Contingencia EPEC - indisponibilidade do servico de autorizacao";

        NFNota nota;
        try {
            nota = montadorService.montar(
                    empresaId,
                    req.enderecoId(),
                    modelo,
                    reserva.serie(),
                    reserva.numero(),
                    req.naturezaOperacao(),
                    null,
                    req.destinatario(),
                    req.itens(),
                    NFTipoEmissao.CONTINGENCIA_EPEC,
                    justificativa);
        } catch (RuntimeException ex) {
            numeracaoService.liberar(empresaId, tipo, reserva.enderecoId(), reserva.numero());
            throw ex;
        }

        var lote = new NFLoteEnvio();
        lote.setVersao("4.00");
        lote.setIdLote(String.valueOf(System.currentTimeMillis()));
        lote.setIndicadorProcessamento(NFLoteIndicadorProcessamento.PROCESSAMENTO_SINCRONO);
        lote.setNotas(List.of(nota));

        try {
            var ret = nfeLibService.facadeForEmpresa(empresaId, modelo).enviaLoteEpec(lote);
            return Map.of(
                    "status", String.valueOf(ret.getCodigoStatusReposta()),
                    "motivo", ret.getMotivo(),
                    "serie", reserva.serie(),
                    "numero", reserva.numero(),
                    "chaveAcesso", nota.getInfo().getIdentificador() != null ? nota.getInfo().getIdentificador() : "");
        } catch (Exception ex) {
            numeracaoService.liberar(empresaId, tipo, reserva.enderecoId(), reserva.numero());
            throw ex;
        }
    }
}
