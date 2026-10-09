package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.NfeEvento;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.NfeEventoRepository;
import com.fincatto.documentofiscal.DFModelo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NfeEventoService {

    private static final int MAX_PAGE = 50;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Set<String> CANCELAMENTO_OK = Set.of("101", "135", "136", "155");
    private static final Pattern NPROT = Pattern.compile("<nProt>(\\d+)</nProt>");

    private final NfeEventoRepository repository;
    private final NfeEmissaoRepository emissaoRepository;
    private final EmpresaRepository empresaRepository;
    private final MembershipService membershipService;

    public NfeEventoService(
            NfeEventoRepository repository,
            NfeEmissaoRepository emissaoRepository,
            EmpresaRepository empresaRepository,
            MembershipService membershipService) {
        this.repository = repository;
        this.emissaoRepository = emissaoRepository;
        this.empresaRepository = empresaRepository;
        this.membershipService = membershipService;
    }

    @Transactional
    public void registrarCancelamento(Long empresaId, DFModelo modelo, String chave, String status, String motivo) {
        var chaveNorm = soDigitos(chave);
        var evento = base(empresaId, modelo, NfeEvento.CANCELAMENTO, status, motivo);
        evento.setChave(chaveNorm);
        evento.setSerie(serieDaChave(chaveNorm));
        evento.setNumeroInicial(numeroDaChave(chaveNorm));
        evento.setNumeroFinal(evento.getNumeroInicial());
        evento.setTexto(motivo);
        repository.save(evento);
        if (CANCELAMENTO_OK.contains(status) && !chaveNorm.isBlank()) {
            emissaoRepository.findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(empresaId, chaveNorm)
                    .ifPresent(nota -> {
                        nota.setStatusProtocolo("101");
                        if (motivo != null && !motivo.isBlank()) {
                            nota.setMotivoProtocolo(cortar(motivo, 500));
                        }
                    });
        }
    }

    @Transactional
    public void registrarCarta(
            Long empresaId, DFModelo modelo, String chave, int sequencial, String texto, String status, String motivo) {
        var chaveNorm = soDigitos(chave);
        var evento = base(empresaId, modelo, NfeEvento.CCE, status, motivo);
        evento.setChave(chaveNorm);
        evento.setSerie(serieDaChave(chaveNorm));
        evento.setNumeroInicial(numeroDaChave(chaveNorm));
        evento.setNumeroFinal(evento.getNumeroInicial());
        evento.setSequencial(sequencial);
        evento.setTexto(texto);
        repository.save(evento);
    }

    @Transactional
    public void registrarInutilizacao(
            Long empresaId,
            DFModelo modelo,
            String serie,
            String numeroInicial,
            String numeroFinal,
            String justificativa,
            String status,
            String motivo,
            String protocolo) {
        var evento = base(empresaId, modelo, NfeEvento.INUTILIZACAO, status, motivo);
        evento.setSerie(serie);
        evento.setNumeroInicial(parseLong(numeroInicial));
        evento.setNumeroFinal(parseLong(numeroFinal));
        evento.setProtocolo(protocolo);
        evento.setTexto(justificativa);
        repository.save(evento);
    }

    /** Dados da nota para preencher cancelamento e carta de correção, sem o usuário digitar a chave. */
    public Map<String, Object> detalheParaEvento(Long usuarioId, String chave) {
        var chaveNorm = soDigitos(chave);
        var ids = idsFiltrados(empresasPermitidas(usuarioId), null);
        if (ids.isEmpty() || chaveNorm.isBlank()) {
            throw new IllegalArgumentException("NF-e não encontrada");
        }
        var nota = emissaoRepository.findFirstByEmpresaIdInAndChaveOrderByCreatedAtDesc(ids, chaveNorm)
                .orElseThrow(() -> new IllegalArgumentException("NF-e não encontrada"));
        var empresa = empresaRepository.findById(nota.getEmpresaId()).orElse(null);
        var body = new LinkedHashMap<String, Object>();
        body.put("empresaId", nota.getEmpresaId());
        body.put("emitenteNome", empresa != null ? empresa.getNome() : null);
        body.put("chave", nota.getChave());
        body.put("serie", nota.getSerie());
        body.put("numero", nota.getNumero());
        body.put("status", nota.getStatusProtocolo());
        body.put("ambiente", nota.getAmbiente());
        body.put("protocolo", primeiroProtocolo(nota.getXmlProc()));
        body.put("proximaCarta", repository.maxSequencialHomologado(nota.getChave(), NfeEvento.CCE) + 1);
        body.put("createdAt", nota.getCreatedAt() != null ? nota.getCreatedAt().toString() : null);
        return body;
    }

    public List<Map<String, Object>> seriesDoEmitente(Long usuarioId, Long emitenteId) {
        var empresa = empresasPermitidas(usuarioId).stream()
                .filter(e -> emitenteId != null && emitenteId.equals(e.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selecione o emitente"));
        var linhas = new ArrayList<Map<String, Object>>();
        for (Object[] row : emissaoRepository.resumoSeries(empresa.getId(), DFModelo.NFE.getCodigo())) {
            var item = new LinkedHashMap<String, Object>();
            item.put("serie", row[0] != null ? row[0].toString() : "");
            item.put("ultimoNumero", row[1] instanceof Number n ? n.longValue() : 0L);
            linhas.add(item);
        }
        return linhas;
    }

    public Map<String, Object> listarVisiveis(
            Long usuarioId,
            Long emitenteId,
            String tipo,
            LocalDate de,
            LocalDate ate,
            String q,
            int page,
            int size) {
        var empresas = empresasPermitidas(usuarioId);
        var ids = idsFiltrados(empresas, emitenteId);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE);
        int pageIdx = Math.max(page, 0);
        var body = new LinkedHashMap<String, Object>();
        if (pageIdx == 0) {
            body.put("emitentes", empresas.stream().map(this::mapEmitente).toList());
        }
        if (ids.isEmpty()) {
            body.put("itens", List.of());
            body.put("page", 0);
            body.put("size", pageSize);
            body.put("totalElements", 0);
            body.put("hasMore", false);
            return body;
        }
        var pageable = PageRequest.of(pageIdx, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var resultado = repository.findLista(
                ids,
                DFModelo.NFE.getCodigo(),
                normalizarTipo(tipo),
                inicio(de),
                fim(ate),
                q == null || q.isBlank() ? null : q.trim(),
                pageable);
        var nomes = empresaRepository.findAllById(
                        resultado.getContent().stream().map(NfeEvento::getEmpresaId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Empresa::getId, Function.identity(), (a, b) -> a));
        body.put("itens", resultado.getContent().stream().map(e -> toMap(e, nomes)).toList());
        body.put("page", resultado.getNumber());
        body.put("size", resultado.getSize());
        body.put("totalElements", resultado.getTotalElements());
        body.put("hasMore", resultado.hasNext());
        return body;
    }

    private NfeEvento base(Long empresaId, DFModelo modelo, String tipo, String status, String motivo) {
        var evento = new NfeEvento();
        evento.setEmpresaId(empresaId);
        evento.setModelo(modelo.getCodigo());
        evento.setTipo(tipo);
        evento.setStatus(cortar(status, 8));
        evento.setMotivo(cortar(motivo, 500));
        return evento;
    }

    private Map<String, Object> toMap(NfeEvento e, Map<Long, Empresa> empresas) {
        var emp = e.getEmpresaId() == null ? null : empresas.get(e.getEmpresaId());
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("empresaId", e.getEmpresaId());
        m.put("emitenteNome", emp != null ? emp.getNome() : null);
        m.put("tipo", e.getTipo());
        m.put("tipoLabel", label(e.getTipo()));
        m.put("chave", e.getChave());
        m.put("serie", e.getSerie());
        m.put("numeroInicial", e.getNumeroInicial());
        m.put("numeroFinal", e.getNumeroFinal());
        m.put("sequencial", e.getSequencial());
        m.put("protocolo", e.getProtocolo());
        m.put("status", e.getStatus());
        m.put("motivo", e.getMotivo());
        m.put("texto", e.getTexto());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return m;
    }

    private static String label(String tipo) {
        if (NfeEvento.CANCELAMENTO.equals(tipo)) return "Cancelamento";
        if (NfeEvento.CCE.equals(tipo)) return "Carta de correção";
        if (NfeEvento.INUTILIZACAO.equals(tipo)) return "Inutilização";
        return tipo;
    }

    private static String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return null;
        var t = tipo.trim().toUpperCase();
        if (t.equals("TODOS") || t.equals("ALL") || t.equals("*")) return null;
        if (t.equals("CANCELAMENTO") || t.equals("CCE") || t.equals("INUTILIZACAO") || t.equals("INUTILIZAÇÃO")) {
            return t.equals("INUTILIZAÇÃO") ? NfeEvento.INUTILIZACAO : t;
        }
        return null;
    }

    private static String primeiroProtocolo(String xml) {
        if (xml == null || xml.isBlank()) return "";
        var matcher = NPROT.matcher(xml);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String soDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private static String serieDaChave(String chave) {
        if (chave == null || chave.length() < 25) return null;
        return String.valueOf(Integer.parseInt(chave.substring(22, 25)));
    }

    private static Long numeroDaChave(String chave) {
        if (chave == null || chave.length() < 34) return null;
        return Long.parseLong(chave.substring(25, 34));
    }

    private static Long parseLong(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String cortar(String valor, int max) {
        if (valor == null) return null;
        var t = valor.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static Instant inicio(LocalDate data) {
        return data == null ? null : data.atStartOfDay(FUSO).toInstant();
    }

    private static Instant fim(LocalDate data) {
        return data == null ? null : data.plusDays(1).atStartOfDay(FUSO).toInstant();
    }

    private List<Empresa> empresasPermitidas(Long usuarioId) {
        return membershipService.listarTodasEmpresasPermitidas(usuarioId).stream()
                .filter(Empresa::isAtivo)
                .toList();
    }

    private List<Long> idsFiltrados(List<Empresa> empresas, Long emitenteId) {
        var ids = empresas.stream().map(Empresa::getId).toList();
        if (emitenteId == null) return ids;
        return ids.contains(emitenteId) ? List.of(emitenteId) : List.of();
    }

    private Map<String, Object> mapEmitente(Empresa empresa) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", empresa.getId());
        m.put("nome", empresa.getNome());
        m.put("documento", empresa.getCnpj());
        return m;
    }
}
