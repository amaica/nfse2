package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cest;
import br.com.synki.nfse.portal.domain.fiscal.ProdutoGrupo;
import br.com.synki.nfse.portal.domain.fiscal.ProdutoSubgrupo;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.fiscal.CestRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoGrupoRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoSubgrupoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProdutoClassificacaoService {

    private static final Logger log = LoggerFactory.getLogger(ProdutoClassificacaoService.class);

    private static final Map<String, List<String>> PADRAO_RURAL = new LinkedHashMap<>();

    static {
        PADRAO_RURAL.put("Grãos e cereais", List.of(
                "Soja", "Milho", "Trigo", "Arroz", "Feijão", "Aveia", "Cevada", "Sorgo"));
        PADRAO_RURAL.put("Animais e produção", List.of(
                "Bovinos", "Suínos", "Aves", "Leite", "Ovos"));
        PADRAO_RURAL.put("Insumos agrícolas", List.of(
                "Fertilizantes", "Defensivos", "Sementes", "Calcário"));
        PADRAO_RURAL.put("Nutrição animal", List.of(
                "Ração", "Farelos", "Núcleo / premix", "Sal mineral"));
        PADRAO_RURAL.put("Combustíveis e lubrificantes", List.of(
                "Diesel", "Gasolina", "Lubrificantes"));
        PADRAO_RURAL.put("Peças e máquinas", List.of(
                "Peças", "Implementos", "Manutenção"));
        PADRAO_RURAL.put("Uso e consumo", List.of(
                "Material de uso", "Expediente"));
        PADRAO_RURAL.put("Ativo imobilizado", List.of(
                "Máquinas", "Veículos", "Benfeitorias"));
        PADRAO_RURAL.put("Outros", List.of("Diversos"));
    }

    private final EmpresaRepository empresaRepository;
    private final ProdutoGrupoRepository grupoRepository;
    private final ProdutoSubgrupoRepository subgrupoRepository;
    private final CestRepository cestRepository;

    public ProdutoClassificacaoService(
            EmpresaRepository empresaRepository,
            ProdutoGrupoRepository grupoRepository,
            ProdutoSubgrupoRepository subgrupoRepository,
            CestRepository cestRepository) {
        this.empresaRepository = empresaRepository;
        this.grupoRepository = grupoRepository;
        this.subgrupoRepository = subgrupoRepository;
        this.cestRepository = cestRepository;
    }

    public List<ProdutoGrupo> listarGrupos(Long empresaId) {
        return grupoRepository.findByEmpresaIdOrderByNomeAsc(empresaId);
    }

    public List<ProdutoSubgrupo> listarSubgrupos(Long empresaId, Long grupoId) {
        if (grupoId == null) {
            return subgrupoRepository.findByEmpresaIdOrderByNomeAsc(empresaId);
        }
        return subgrupoRepository.findByEmpresaIdAndProdutoGrupoIdOrderByNomeAsc(empresaId, grupoId);
    }

    public List<Cest> buscarCest(String q, String ncm) {
        String raw = q == null ? "" : q.trim();
        String digits = raw.replaceAll("\\D", "");
        String query = digits.length() >= 2 ? digits : raw;
        String ncmDigits = ncm == null ? "" : ncm.replaceAll("\\D", "");
        return cestRepository.buscar(query, ncmDigits, PageRequest.of(0, 80));
    }

    @Transactional
    public int garantirTodasEmpresas() {
        int n = 0;
        for (var empresa : empresaRepository.findAll()) {
            if (empresa.isAtivo()) {
                n += garantirPadrao(empresa.getId());
            }
        }
        return n;
    }

    @Transactional
    public int garantirPadrao(Long empresaId) {
        int criados = 0;
        for (var entry : PADRAO_RURAL.entrySet()) {
            var grupo = grupoRepository.findFirstByEmpresaIdAndNomeIgnoreCase(empresaId, entry.getKey())
                    .orElseGet(() -> {
                        var g = new ProdutoGrupo();
                        g.setEmpresaId(empresaId);
                        g.setNome(entry.getKey());
                        return grupoRepository.save(g);
                    });
            for (String subNome : entry.getValue()) {
                var existente = subgrupoRepository.findFirstByEmpresaIdAndProdutoGrupoIdAndNomeIgnoreCase(
                        empresaId, grupo.getId(), subNome);
                if (existente.isEmpty()) {
                    var s = new ProdutoSubgrupo();
                    s.setEmpresaId(empresaId);
                    s.setProdutoGrupoId(grupo.getId());
                    s.setNome(subNome);
                    subgrupoRepository.save(s);
                    criados++;
                }
            }
        }
        if (criados > 0) {
            log.info("Seed grupo/subgrupo produto empresa {}: {} item(ns)", empresaId, criados);
        }
        return criados;
    }
}
