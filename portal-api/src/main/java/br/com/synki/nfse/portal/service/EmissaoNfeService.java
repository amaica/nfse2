package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.NfeEmissao;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.web.dto.nfe.NfeEmitirLoteRequest;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.nfe.NFTipoEmissao;
import com.fincatto.documentofiscal.nfe400.classes.NFProtocolo;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvio;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteIndicadorProcessamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNota;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaProcessada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmissaoNfeService {

    private final NfeLibService nfeLibService;
    private final NfeMontadorService montadorService;
    private final NumeracaoNfeService numeracaoService;
    private final NfeEmissaoRepository emissaoRepository;

    public EmissaoNfeService(
            NfeLibService nfeLibService,
            NfeMontadorService montadorService,
            NumeracaoNfeService numeracaoService,
            NfeEmissaoRepository emissaoRepository) {
        this.nfeLibService = nfeLibService;
        this.montadorService = montadorService;
        this.numeracaoService = numeracaoService;
        this.emissaoRepository = emissaoRepository;
    }

    @Transactional
    public Map<String, Object> enviarLote(Long empresaId, DFModelo modelo, NfeEmitirLoteRequest req) throws Exception {
        if (!nfeLibService.temCertificado(empresaId)) {
            throw new IllegalStateException("Certificado A1 nao cadastrado");
        }
        var tipo = modelo == DFModelo.NFCE ? ConfiguracaoDocumento.TIPO_NFCE : ConfiguracaoDocumento.TIPO_NFE;
        var reserva = numeracaoService.reservar(empresaId, tipo, req != null ? req.enderecoId() : null);
        boolean sincrono = req == null || req.sincrono() == null || req.sincrono();

        NFNota nota;
        try {
            nota = montadorService.montar(
                    empresaId,
                    req != null ? req.enderecoId() : null,
                    modelo,
                    reserva.serie(),
                    reserva.numero(),
                    req != null ? req.naturezaOperacao() : null,
                    req != null ? req.operacaoFiscalId() : null,
                    req != null ? req.destinatario() : null,
                    req != null ? req.itens() : null,
                    NFTipoEmissao.EMISSAO_NORMAL,
                    null);
        } catch (RuntimeException ex) {
            numeracaoService.liberar(empresaId, tipo, reserva.enderecoId(), reserva.numero());
            throw ex;
        }

        var lote = new NFLoteEnvio();
        lote.setVersao("4.00");
        lote.setIdLote(String.valueOf(System.currentTimeMillis()));
        lote.setIndicadorProcessamento(sincrono
                ? NFLoteIndicadorProcessamento.PROCESSAMENTO_SINCRONO
                : NFLoteIndicadorProcessamento.PROCESSAMENTO_ASSINCRONO);
        lote.setNotas(List.of(nota));

        try {
            var retorno = nfeLibService.facadeForEmpresa(empresaId, modelo).enviaLote(lote);
            return montarRespostaEnvio(empresaId, retorno, modelo, reserva);
        } catch (Exception ex) {
            numeracaoService.liberar(empresaId, tipo, reserva.enderecoId(), reserva.numero());
            throw ex;
        }
    }

    private Map<String, Object> montarRespostaEnvio(
            Long empresaId,
            com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvioRetornoDados dados,
            DFModelo modelo,
            NumeracaoNfeService.ReservaNumeracao reserva) throws Exception {
        var ret = dados.getRetorno();
        var body = new LinkedHashMap<String, Object>();
        body.put("status", ret.getStatus());
        body.put("motivo", ret.getMotivo());
        body.put("serie", reserva.serie());
        body.put("numero", reserva.numero());
        body.put("modelo", modelo.getCodigo());
        body.put("sucesso", false);

        if (ret.getInfoRecebimento() != null) {
            body.put("recibo", ret.getInfoRecebimento().getRecibo());
            body.put("tempoMedio", ret.getInfoRecebimento().getTempoMedio());
        }

        String chave = null;
        if (ret.getProtocoloInfo() != null) {
            chave = ret.getProtocoloInfo().getChave();
            body.put("chaveAcesso", chave);
            body.put("protocolo", ret.getProtocoloInfo().getNumeroProtocolo());
            body.put("statusProtocolo", ret.getProtocoloInfo().getStatus());
            body.put("motivoProtocolo", ret.getProtocoloInfo().getMotivo());
            body.put("sucesso", "100".equals(ret.getProtocoloInfo().getStatus()));
        }
        if (!dados.getLoteAssinado().getNotas().isEmpty()) {
            var notaAssinada = dados.getLoteAssinado().getNotas().getFirst();
            if (chave == null) {
                chave = notaAssinada.getInfo().getIdentificador();
            }
            body.put("chaveAcesso", chave);
        }

        String xmlProc = null;
        if (ret.getProtocoloInfo() != null && !dados.getLoteAssinado().getNotas().isEmpty()) {
            var proc = new NFNotaProcessada();
            proc.setVersao(new BigDecimal("1.00"));
            proc.setNota(dados.getLoteAssinado().getNotas().getFirst());
            var protocolo = new NFProtocolo();
            protocolo.setVersao("4.00");
            protocolo.setProtocoloInfo(ret.getProtocoloInfo());
            proc.setProtocolo(protocolo);
            xmlProc = proc.toString();
            body.put("xmlProc", xmlProc);
        }

        if (chave != null) {
            var registro = new NfeEmissao();
            registro.setEmpresaId(empresaId);
            registro.setChave(chave.replace("NFe", ""));
            registro.setSerie(reserva.serie());
            registro.setNumero(reserva.numero());
            registro.setModelo(modelo.getCodigo());
            if (ret.getProtocoloInfo() != null) {
                registro.setStatusProtocolo(ret.getProtocoloInfo().getStatus());
                registro.setMotivoProtocolo(ret.getProtocoloInfo().getMotivo());
            }
            registro.setXmlProc(xmlProc);
            emissaoRepository.save(registro);
        }
        return body;
    }

    public List<Map<String, Object>> listarNotas(Long empresaId) {
        return emissaoRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .<Map<String, Object>>map(e -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("id", e.getId());
                    m.put("chave", e.getChave());
                    m.put("serie", e.getSerie());
                    m.put("numero", e.getNumero());
                    m.put("modelo", e.getModelo());
                    m.put("statusProtocolo", e.getStatusProtocolo());
                    m.put("motivoProtocolo", e.getMotivoProtocolo());
                    m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                    return m;
                })
                .toList();
    }
}
