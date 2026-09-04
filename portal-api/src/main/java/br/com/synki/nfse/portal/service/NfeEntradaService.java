package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class NfeEntradaService {

    private static final int MAX_PAGE = 200;
    private static final int MAX_EXPORT = 2000;

    private final NfeEntradaRepository repository;
    private final EmpresaRepository empresaRepository;

    public NfeEntradaService(NfeEntradaRepository repository, EmpresaRepository empresaRepository) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
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
}
