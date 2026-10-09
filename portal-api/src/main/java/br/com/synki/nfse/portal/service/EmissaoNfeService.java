package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.NfeEmissao;
import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final MembershipService membershipService;
    private final EmpresaRepository empresaRepository;

    public EmissaoNfeService(
            NfeLibService nfeLibService,
            NfeMontadorService montadorService,
            NumeracaoNfeService numeracaoService,
            NfeEmissaoRepository emissaoRepository,
            NfeEntradaRepository entradaRepository,
            MembershipService membershipService,
            EmpresaRepository empresaRepository) {
        this.nfeLibService = nfeLibService;
        this.montadorService = montadorService;
        this.numeracaoService = numeracaoService;
        this.emissaoRepository = emissaoRepository;
        this.entradaRepository = entradaRepository;
        this.membershipService = membershipService;
        this.empresaRepository = empresaRepository;
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
            registro.setAmbiente(nfeLibService.ambiente(empresaId).getCodigo());
            var resumo = NfeXmlResumo.extrair(xmlProc);
            registro.setValorTotal(resumo.valorTotal());
            registro.setDestinatarioNome(resumo.destinatarioNome());
            registro.setCfop(resumo.cfop());
            registro.setTipoOperacao(resumo.tipoOperacao());
            registro.setProdutosResumo(resumo.produtosResumo());
            emissaoRepository.save(registro);
        }

        ajustarNumeracaoSeDuplicidade(empresaId, modelo, reserva, body);
        return body;
    }

    private void ajustarNumeracaoSeDuplicidade(
            Long empresaId,
            DFModelo modelo,
            NumeracaoNfeService.ReservaNumeracao reserva,
            Map<String, Object> body) {
        var statusProt = String.valueOf(body.getOrDefault("statusProtocolo", ""));
        var motivo = String.valueOf(body.getOrDefault("motivoProtocolo", body.getOrDefault("motivo", "")));
        boolean duplicidade = "539".equals(statusProt)
                || "204".equals(statusProt)
                || motivo.toLowerCase().contains("duplicidade");
        if (!duplicidade) {
            return;
        }
        long sefazNum = numeroDaChaveNoMotivo(motivo);
        long alvo = Math.max(reserva.numero(), sefazNum);
        var tipo = modelo == DFModelo.NFCE ? ConfiguracaoDocumento.TIPO_NFCE : ConfiguracaoDocumento.TIPO_NFE;
        numeracaoService.garantirMinimo(empresaId, tipo, reserva.enderecoId(), reserva.serie(), alvo);
        long proximo = alvo + 1;
        body.put("numeracaoAjustada", true);
        body.put("proximoNumeroSugerido", proximo);
        body.put(
                "mensagem",
                "Número " + alvo + " já existe na SEFAZ (duplicidade). "
                        + "Numeração ajustada — a próxima NF-e será " + proximo
                        + ". Corrija se precisar e transmita de novo.");
    }

    private static final Pattern CHAVE_NO_MOTIVO = Pattern.compile("chNFe[:\\s]*(\\d{44})", Pattern.CASE_INSENSITIVE);

    private static long numeroDaChaveNoMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return 0L;
        }
        var m = CHAVE_NO_MOTIVO.matcher(motivo);
        if (!m.find()) {
            return 0L;
        }
        var chave = m.group(1);
        try {
            return Long.parseLong(chave.substring(25, 34));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPORT = 5000;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\D");

    public Map<String, Object> listarNotas(Long empresaId, DFModelo modelo, int page, int size) {
        return listarNotasFiltradas(empresaId, modelo, page, size, null, null, null, null, null, null, null);
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
            String status,
            String ambiente) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NfeEmissao> resultado = emissaoRepository.findFiltradas(
                empresaId,
                modelo.getCodigo(),
                toInstantInicio(dataDe),
                toInstantFimExclusivo(dataAte),
                numero,
                blankToNull(serie),
                blankToNull(status),
                normalizarAmbiente(ambiente),
                normalizarBusca(q),
                pageable);
        return mapPagina(resultado);
    }

    /** Listagem de NF-e de todos os emitentes que o usuário pode ver. `emitenteId` nulo = todos. */
    public Map<String, Object> listarNotasVisiveis(
            Long usuarioId,
            Long emitenteId,
            DFModelo modelo,
            int page,
            int size,
            LocalDate dataDe,
            LocalDate dataAte,
            String q,
            Long numero,
            String serie,
            String status,
            String ambiente) {
        var empresas = empresasPermitidas(usuarioId);
        var ids = idsFiltrados(empresas, emitenteId);
        var resposta = new LinkedHashMap<String, Object>();
        if (page <= 0) {
            resposta.put("emitentes", empresas.stream().map(this::mapEmitente).toList());
        }
        if (ids.isEmpty()) {
            resposta.put("itens", List.of());
            resposta.put("page", 0);
            resposta.put("size", size);
            resposta.put("totalElements", 0);
            resposta.put("hasMore", false);
            resposta.put("valorTotalSomado", java.math.BigDecimal.ZERO);
            return resposta;
        }
        var ambienteNorm = normalizarAmbiente(ambiente);
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NfeListaItem> resultado = emissaoRepository.findLista(
                ids,
                modelo.getCodigo(),
                toInstantInicio(dataDe),
                toInstantFimExclusivo(dataAte),
                numero,
                blankToNull(serie),
                blankToNull(status),
                ambienteNorm,
                normalizarBusca(q),
                pageable);
        var nomes = nomesEmpresas(resultado.getContent().stream().map(NfeListaItem::getEmpresaId).distinct().toList());
        resposta.put("itens", resultado.getContent().stream().map(item -> mapLista(item, nomes)).toList());
        resposta.put("page", resultado.getNumber());
        resposta.put("size", resultado.getSize());
        resposta.put("totalElements", resultado.getTotalElements());
        resposta.put("hasMore", resultado.hasNext());
        var soma = emissaoRepository.somarValorLista(
                ids,
                modelo.getCodigo(),
                toInstantInicio(dataDe),
                toInstantFimExclusivo(dataAte),
                numero,
                blankToNull(serie),
                blankToNull(status),
                ambienteNorm,
                normalizarBusca(q));
        resposta.put("valorTotalSomado", soma != null ? soma : java.math.BigDecimal.ZERO);
        if (page <= 0 && "1".equals(ambienteNorm) && resultado.getTotalElements() == 0) {
            resposta.put("totalHomologacao", emissaoRepository.contarAmbiente(
                    ids, modelo.getCodigo(), "2", toInstantInicio(dataDe), toInstantFimExclusivo(dataAte)));
        }
        return resposta;
    }

    public NfeEmissao notaPermitida(Long usuarioId, String chave) {
        var chaveNorm = chave == null ? "" : chave.replace("NFe", "").replaceAll("\\D", "");
        var ids = idsFiltrados(empresasPermitidas(usuarioId), null);
        if (ids.isEmpty() || chaveNorm.isBlank()) {
            throw new IllegalArgumentException("NF-e não encontrada");
        }
        return emissaoRepository.findFirstByEmpresaIdInAndChaveOrderByCreatedAtDesc(ids, chaveNorm)
                .orElseThrow(() -> new IllegalArgumentException("NF-e não encontrada"));
    }

    public byte[] exportarXmlZipVisivel(
            Long usuarioId,
            Long emitenteId,
            DFModelo modelo,
            LocalDate dataDe,
            LocalDate dataAte,
            String q,
            Long numero,
            String serie,
            String status,
            String ambiente,
            List<String> chaves) throws Exception {
        var ids = idsFiltrados(empresasPermitidas(usuarioId), emitenteId);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Nenhum XML encontrado para exportacao");
        }
        List<NfeEmissao> notas;
        if (chaves != null && !chaves.isEmpty()) {
            var chavesNorm = chaves.stream()
                    .map(c -> c.replace("NFe", "").replaceAll("\\D", ""))
                    .filter(c -> !c.isBlank())
                    .distinct()
                    .toList();
            notas = emissaoRepository.findByEmpresaIdInAndModeloAndChaveIn(ids, modelo.getCodigo(), chavesNorm);
        } else {
            var pageable = PageRequest.of(0, MAX_EXPORT, Sort.by(Sort.Direction.DESC, "createdAt"));
            notas = emissaoRepository.findFiltradasEm(
                    ids,
                    modelo.getCodigo(),
                    toInstantInicio(dataDe),
                    toInstantFimExclusivo(dataAte),
                    numero,
                    blankToNull(serie),
                    blankToNull(status),
                    normalizarAmbiente(ambiente),
                    normalizarBusca(q),
                    pageable).getContent();
        }
        return zipXml(notas);
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
            String ambiente,
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
            var pageable = PageRequest.of(0, MAX_EXPORT, Sort.by(Sort.Direction.DESC, "createdAt"));
            notas = emissaoRepository.findFiltradas(
                    empresaId,
                    modelo.getCodigo(),
                    toInstantInicio(dataDe),
                    toInstantFimExclusivo(dataAte),
                    numero,
                    blankToNull(serie),
                    blankToNull(status),
                    normalizarAmbiente(ambiente),
                    normalizarBusca(q),
                    pageable).getContent();
        }
        return zipXml(notas);
    }

    private byte[] zipXml(List<NfeEmissao> notas) throws Exception {
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

    private List<Empresa> empresasPermitidas(Long usuarioId) {
        return membershipService.listarTodasEmpresasPermitidas(usuarioId).stream()
                .filter(Empresa::isAtivo)
                .toList();
    }

    private List<Long> idsFiltrados(List<Empresa> empresas, Long emitenteId) {
        var ids = empresas.stream().map(Empresa::getId).toList();
        if (emitenteId == null) {
            return ids;
        }
        return ids.contains(emitenteId) ? List.of(emitenteId) : List.of();
    }

    private Map<String, Object> mapEmitente(Empresa empresa) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", empresa.getId());
        m.put("nome", empresa.getNome());
        m.put("documento", empresa.getCnpj());
        return m;
    }

    private Map<Long, Empresa> nomesEmpresas(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return empresaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Empresa::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Object> mapLista(NfeListaItem e, Map<Long, Empresa> empresas) {
        var m = new LinkedHashMap<String, Object>();
        var emp = e.getEmpresaId() == null ? null : empresas.get(e.getEmpresaId());
        m.put("id", e.getId());
        m.put("empresaId", e.getEmpresaId());
        m.put("emitenteNome", emp != null ? emp.getNome() : null);
        m.put("emitenteDocumento", emp != null ? emp.getCnpj() : null);
        m.put("chave", e.getChave());
        m.put("serie", e.getSerie());
        m.put("numero", e.getNumero());
        m.put("modelo", e.getModelo());
        m.put("statusProtocolo", e.getStatusProtocolo());
        m.put("motivoProtocolo", e.getMotivoProtocolo());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        m.put("temXml", e.isTemXml());
        m.put("ambiente", e.getAmbiente() != null ? e.getAmbiente() : "1");
        m.put("valorTotal", e.getValorTotal());
        m.put("cliente", e.getDestinatarioNome());
        m.put("destinatarioNome", e.getDestinatarioNome());
        m.put("cfop", e.getCfop());
        m.put("tipoOperacao", e.getTipoOperacao());
        m.put("produtos", produtosLista(e.getProdutosResumo()));
        return m;
    }

    private static java.util.List<String> produtosLista(String produtosResumo) {
        if (produtosResumo == null || produtosResumo.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(produtosResumo.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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
        m.put("ambiente", e.getAmbiente() != null ? e.getAmbiente() : "1");
        m.put("valor", e.getValorTotal());
        m.put("valorTotal", e.getValorTotal());
        m.put("contraparte", e.getDestinatarioNome());
        m.put("cliente", e.getDestinatarioNome());
        m.put("destinatarioNome", e.getDestinatarioNome());
        m.put("cfop", e.getCfop());
        m.put("tipoOperacao", e.getTipoOperacao());
        return m;
    }

    private static Instant toInstantInicio(LocalDate data) {
        return data == null ? null : data.atStartOfDay(FUSO).toInstant();
    }

    private static Instant toInstantFimExclusivo(LocalDate data) {
        return data == null ? null : data.plusDays(1).atStartOfDay(FUSO).toInstant();
    }

    /** null = sem filtro. "2" homologação, qualquer outro valor vira produção. "todos" limpa o filtro. */
    public static String normalizarAmbiente(String ambiente) {
        if (ambiente == null || ambiente.isBlank()) {
            return null;
        }
        var a = ambiente.trim().toLowerCase();
        if (a.equals("todos") || a.equals("all") || a.equals("*")) {
            return null;
        }
        if (a.equals("2") || a.equals("homologacao") || a.equals("homologação") || a.equals("h")) {
            return "2";
        }
        return "1";
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
