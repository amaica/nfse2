package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.NfeEmissao;
import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import br.com.synki.nfse.portal.web.dto.nfe.NfeEmitirLoteRequest;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.nfe.NFTipoEmissao;
import com.fincatto.documentofiscal.nfe400.classes.NFProtocolo;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteEnvio;
import com.fincatto.documentofiscal.nfe400.classes.lote.envio.NFLoteIndicadorProcessamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNota;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaProcessada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class EmissaoNfeService {

    private final NfeLibService nfeLibService;
    private final NfeMontadorService montadorService;
    private final NumeracaoNfeService numeracaoService;
    private final NfeEmissaoRepository emissaoRepository;
    private final NfeEntradaRepository entradaRepository;

    public EmissaoNfeService(
            NfeLibService nfeLibService,
            NfeMontadorService montadorService,
            NumeracaoNfeService numeracaoService,
            NfeEmissaoRepository emissaoRepository,
            NfeEntradaRepository entradaRepository) {
        this.nfeLibService = nfeLibService;
        this.montadorService = montadorService;
        this.numeracaoService = numeracaoService;
        this.emissaoRepository = emissaoRepository;
        this.entradaRepository = entradaRepository;
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
                    null,
                    req);
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

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPORT = 5000;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\D");

    public Map<String, Object> listarNotas(Long empresaId, DFModelo modelo, int page, int size) {
        return listarNotasFiltradas(empresaId, modelo, page, size, null, null, null, null, null, null);
    }

    public Map<String, Object> listarNotasFiltradas(
            Long empresaId,
            DFModelo modelo,
            int page,
            int size,
            LocalDate dataDe,
            LocalDate dataAte,
            String q,
            Long numero,
            String serie,
            String status) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<NfeEmissao> resultado = emissaoRepository.findFiltradas(
                empresaId,
                modelo.getCodigo(),
                toInstantInicio(dataDe),
                toInstantFimExclusivo(dataAte),
                numero,
                blankToNull(serie),
                blankToNull(status),
                normalizarBusca(q),
                pageable);
        return mapPagina(resultado);
    }

    public List<Map<String, Object>> listarEntradas(Long empresaId, String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        return entradaRepository.buscar(empresaId, q.trim(), PageRequest.of(0, 15)).stream()
                .map(this::mapEntrada)
                .toList();
    }

    private Map<String, Object> mapEntrada(NfeEntrada e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("chave", e.getChave());
        m.put("serie", e.getSerie());
        m.put("numero", e.getNumero());
        m.put("nomeEmitente", e.getNomeEmitente());
        m.put("cnpjEmitente", e.getCnpjEmitente());
        m.put("dataEmissao", e.getDataEmissao() != null ? e.getDataEmissao().toString() : null);
        m.put("valor", e.getValor());
        m.put("origem", "ENTRADA");
        return m;
    }

    public byte[] exportarXmlZip(
            Long empresaId,
            DFModelo modelo,
            LocalDate dataDe,
            LocalDate dataAte,
            String q,
            Long numero,
            String serie,
            String status,
            List<String> chaves) throws Exception {
        List<NfeEmissao> notas;
        if (chaves != null && !chaves.isEmpty()) {
            var chavesNorm = chaves.stream()
                    .map(c -> c.replace("NFe", "").replaceAll("\\D", ""))
                    .filter(c -> !c.isBlank())
                    .distinct()
                    .toList();
            notas = emissaoRepository.findByEmpresaIdAndModeloAndChaveIn(empresaId, modelo.getCodigo(), chavesNorm);
        } else {
            var pageable = PageRequest.of(0, MAX_EXPORT);
            notas = emissaoRepository.findFiltradas(
                    empresaId,
                    modelo.getCodigo(),
                    toInstantInicio(dataDe),
                    toInstantFimExclusivo(dataAte),
                    numero,
                    blankToNull(serie),
                    blankToNull(status),
                    normalizarBusca(q),
                    pageable).getContent();
        }
        var baos = new ByteArrayOutputStream();
        int count = 0;
        try (var zos = new ZipOutputStream(baos)) {
            for (NfeEmissao nota : notas) {
                if (nota.getXmlProc() == null || nota.getXmlProc().isBlank()) {
                    continue;
                }
                var entry = new ZipEntry(nota.getChave() + "-proc.xml");
                zos.putNextEntry(entry);
                zos.write(nota.getXmlProc().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                count++;
            }
        }
        if (count == 0) {
            throw new IllegalStateException("Nenhum XML encontrado para exportacao");
        }
        return baos.toByteArray();
    }

    private Map<String, Object> mapPagina(Page<NfeEmissao> resultado) {
        var itens = resultado.getContent().stream()
                .<Map<String, Object>>map(this::mapNota)
                .toList();
        var resposta = new LinkedHashMap<String, Object>();
        resposta.put("itens", itens);
        resposta.put("page", resultado.getNumber());
        resposta.put("size", resultado.getSize());
        resposta.put("totalElements", resultado.getTotalElements());
        resposta.put("hasMore", resultado.hasNext());
        return resposta;
    }

    private Map<String, Object> mapNota(NfeEmissao e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("chave", e.getChave());
        m.put("serie", e.getSerie());
        m.put("numero", e.getNumero());
        m.put("modelo", e.getModelo());
        m.put("statusProtocolo", e.getStatusProtocolo());
        m.put("motivoProtocolo", e.getMotivoProtocolo());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        m.put("temXml", e.getXmlProc() != null && !e.getXmlProc().isBlank());
        return m;
    }

    private static Instant toInstantInicio(LocalDate data) {
        return data == null ? null : data.atStartOfDay(FUSO).toInstant();
    }

    private static Instant toInstantFimExclusivo(LocalDate data) {
        return data == null ? null : data.plusDays(1).atStartOfDay(FUSO).toInstant();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String normalizarBusca(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        var trimmed = q.trim();
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed;
        }
        return trimmed;
    }

    public static Long parseNumeroFiltro(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        var digits = SOMENTE_DIGITOS.matcher(q.trim()).replaceAll("");
        if (digits.isEmpty() || digits.length() > 9) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
