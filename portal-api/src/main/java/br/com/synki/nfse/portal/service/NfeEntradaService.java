package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class NfeEntradaService {

    private static final int MAX_PAGE = 200;
    private static final int MAX_EXPORT = 2000;

    private final NfeEntradaRepository repository;
    private final EmpresaRepository empresaRepository;
    private final MembershipService membershipService;

    public NfeEntradaService(
            NfeEntradaRepository repository,
            EmpresaRepository empresaRepository,
            MembershipService membershipService) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.membershipService = membershipService;
    }

    public Map<String, Object> listar(
            Long empresaId,
            LocalDate de,
            LocalDate ate,
            String q,
            int page,
            int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE);
        int pageIdx = Math.max(page, 0);
        String busca = q == null ? "" : q.trim();
        var pageable = PageRequest.of(pageIdx, pageSize);
        List<NfeEntrada> itens = repository.filtrar(empresaId, de, ate, busca, pageable);
        long total = repository.contarFiltradas(empresaId, de, ate, busca);
        var empresa = empresaRepository.findById(empresaId).orElse(null);

        var body = new LinkedHashMap<String, Object>();
        body.put("itens", itens.stream().map(this::toMap).toList());
        body.put("page", pageIdx);
        body.put("size", pageSize);
        body.put("totalElements", total);
        body.put("hasMore", (long) (pageIdx + 1) * pageSize < total);
        body.put("baixarXml", empresa != null && empresa.isBaixarXml());
        body.put("ultimoNsu", empresa != null ? empresa.getUltimoNsu() : null);
        body.put(
                "ultimoNsuBaixadoEm",
                empresa != null && empresa.getUltimoNsuBaixadoEm() != null
                        ? empresa.getUltimoNsuBaixadoEm().toString()
                        : null);
        return body;
    }

    /** Lista DF-e de todos os emitentes permitidos. `emitenteId` nulo = todos. Não carrega o XML. */
    public Map<String, Object> listarVisiveis(
            Long usuarioId,
            Long emitenteId,
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
            body.put("baixarXml", false);
            return body;
        }
        String busca = q == null ? "" : q.trim();
        var pageable = PageRequest.of(pageIdx, pageSize, Sort.by(Sort.Direction.DESC, "dataEmissao", "id"));
        Page<NfeEntradaListaItem> resultado = repository.findLista(ids, de, ate, busca, pageable);
        var nomes = empresaRepository.findAllById(
                        resultado.getContent().stream().map(NfeEntradaListaItem::getEmpresaId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Empresa::getId, Function.identity(), (a, b) -> a));
        body.put("itens", resultado.getContent().stream().map(item -> toMapLista(item, nomes)).toList());
        body.put("page", resultado.getNumber());
        body.put("size", resultado.getSize());
        body.put("totalElements", resultado.getTotalElements());
        body.put("hasMore", resultado.hasNext());
        if (emitenteId != null) {
            var empresa = nomes.get(emitenteId);
            if (empresa == null) {
                empresa = empresaRepository.findById(emitenteId).orElse(null);
            }
            body.put("baixarXml", empresa != null && empresa.isBaixarXml());
            body.put("ultimoNsu", empresa != null ? empresa.getUltimoNsu() : null);
        } else {
            body.put("baixarXml", false);
        }
        return body;
    }

    public byte[] exportarZipVisivel(
            Long usuarioId,
            Long emitenteId,
            LocalDate de,
            LocalDate ate,
            String q,
            Collection<Long> ids) throws Exception {
        var permitidos = idsFiltrados(empresasPermitidas(usuarioId), emitenteId);
        if (permitidos.isEmpty()) {
            throw new IllegalStateException("Nenhum XML de entrada para exportar");
        }
        List<NfeEntrada> notas;
        if (ids != null && !ids.isEmpty()) {
            notas = repository.findByEmpresaIdInAndIdIn(permitidos, ids);
        } else {
            String busca = q == null ? "" : q.trim();
            notas = repository.filtrarEm(permitidos, de, ate, busca, PageRequest.of(0, MAX_EXPORT));
        }
        return zip(notas);
    }

    public String xmlPorIdVisivel(Long usuarioId, Long id) {
        var permitidos = idsFiltrados(empresasPermitidas(usuarioId), null);
        var e = repository.findById(id)
                .filter(n -> permitidos.contains(n.getEmpresaId()))
                .orElseThrow(() -> new NoSuchElementException("Nota de entrada nao encontrada"));
        if (e.getXml() == null || e.getXml().isBlank()) {
            throw new IllegalStateException("XML nao disponivel");
        }
        return e.getXml();
    }

    public Empresa exigirEmitente(Long usuarioId, Long emitenteId) {
        if (emitenteId == null) {
            throw new IllegalArgumentException("Selecione o emitente");
        }
        return empresasPermitidas(usuarioId).stream()
                .filter(e -> emitenteId.equals(e.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Emitente não permitido"));
    }

    public byte[] exportarZip(
            Long empresaId,
            LocalDate de,
            LocalDate ate,
            String q,
            Collection<Long> ids) throws Exception {
        List<NfeEntrada> notas;
        if (ids != null && !ids.isEmpty()) {
            notas = repository.findByEmpresaIdAndIdIn(empresaId, ids);
        } else {
            String busca = q == null ? "" : q.trim();
            notas = repository.filtrar(empresaId, de, ate, busca, PageRequest.of(0, MAX_EXPORT));
        }
        return zip(notas);
    }

    public String xmlPorId(Long empresaId, Long id) {
        var e = repository.findById(id)
                .filter(n -> empresaId.equals(n.getEmpresaId()))
                .orElseThrow(() -> new NoSuchElementException("Nota de entrada nao encontrada"));
        if (e.getXml() == null || e.getXml().isBlank()) {
            throw new IllegalStateException("XML nao disponivel");
        }
        return e.getXml();
    }

    public Map<String, Object> toMap(NfeEntrada e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("chave", e.getChave());
        m.put("nsu", e.getNsu());
        m.put("numero", e.getNumero());
        m.put("serie", e.getSerie());
        m.put("nomeEmitente", e.getNomeEmitente());
        m.put("cnpjEmitente", e.getCnpjEmitente());
        m.put("dataEmissao", e.getDataEmissao() != null ? e.getDataEmissao().toString() : null);
        m.put("natureza", e.getNatureza());
        m.put("valor", e.getValor());
        m.put("temXml", e.getXml() != null && !e.getXml().isBlank());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toMapLista(NfeEntradaListaItem e, Map<Long, Empresa> empresas) {
        var emp = e.getEmpresaId() == null ? null : empresas.get(e.getEmpresaId());
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("empresaId", e.getEmpresaId());
        m.put("destinatarioNome", emp != null ? emp.getNome() : null);
        m.put("destinatarioDocumento", emp != null ? emp.getCnpj() : null);
        m.put("chave", e.getChave());
        m.put("nsu", e.getNsu());
        m.put("numero", e.getNumero());
        m.put("serie", e.getSerie());
        m.put("nomeEmitente", e.getNomeEmitente());
        m.put("cnpjEmitente", e.getCnpjEmitente());
        m.put("dataEmissao", e.getDataEmissao() != null ? e.getDataEmissao().toString() : null);
        m.put("natureza", e.getNatureza());
        m.put("valor", e.getValor());
        m.put("temXml", e.isTemXml());
        return m;
    }

    private byte[] zip(List<NfeEntrada> notas) throws Exception {
        var baos = new ByteArrayOutputStream();
        int count = 0;
        try (var zos = new ZipOutputStream(baos)) {
            for (NfeEntrada e : notas) {
                if (e.getXml() == null || e.getXml().isBlank()) {
                    continue;
                }
                String nome = (e.getChave() != null && !e.getChave().isBlank() ? e.getChave() : "nfe-" + e.getId())
                        + "-proc.xml";
                zos.putNextEntry(new ZipEntry(nome));
                zos.write(e.getXml().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                count++;
            }
        }
        if (count == 0) {
            throw new IllegalStateException("Nenhum XML de entrada para exportar");
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
}
