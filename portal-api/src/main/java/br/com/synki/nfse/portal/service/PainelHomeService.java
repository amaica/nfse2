package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.NfeEmissao;
import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import com.fincatto.documentofiscal.DFModelo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resumo do painel sempre do emitente ativo (empresa da sessão) —
 * NF-e emitidas autorizadas e NF-e recebidas (DF-e), sem misturar outros CNPJs.
 */
@Service
public class PainelHomeService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final NfeEntradaService nfeEntradaService;
    private final NfeEmissaoRepository nfeEmissaoRepository;
    private final NfeEntradaRepository nfeEntradaRepository;

    public PainelHomeService(
            NfeEntradaService nfeEntradaService,
            NfeEmissaoRepository nfeEmissaoRepository,
            NfeEntradaRepository nfeEntradaRepository) {
        this.nfeEntradaService = nfeEntradaService;
        this.nfeEmissaoRepository = nfeEmissaoRepository;
        this.nfeEntradaRepository = nfeEntradaRepository;
    }

    public Map<String, Object> home(Long usuarioId, Long empresaId, Integer anoParam) {
        int ano = anoParam != null && anoParam >= 2000 && anoParam <= 2100
                ? anoParam
                : Year.now().getValue();
        LocalDate de = LocalDate.of(ano, 1, 1);
        LocalDate ate = LocalDate.of(ano, 12, 31);
        if (ate.isAfter(LocalDate.now(FUSO))) {
            ate = LocalDate.now(FUSO);
        }
        var inicio = de.atStartOfDay(FUSO).toInstant();
        var fimExcl = ate.plusDays(1).atStartOfDay(FUSO).toInstant();

        List<NfeEmissao> autorizadas = empresaId == null
                ? List.of()
                : nfeEmissaoRepository.findAutorizadasProducaoNoPeriodo(
                        empresaId, DFModelo.NFE.getCodigo(), inicio, fimExcl);

        BigDecimal receitas = BigDecimal.ZERO;
        BigDecimal[] recMes = new BigDecimal[12];
        BigDecimal[] desMes = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            recMes[i] = BigDecimal.ZERO;
            desMes[i] = BigDecimal.ZERO;
        }
        for (var n : autorizadas) {
            var valor = n.getValorTotal() != null ? n.getValorTotal() : BigDecimal.ZERO;
            receitas = receitas.add(valor);
            if (n.getCreatedAt() != null) {
                int mi = n.getCreatedAt().atZone(FUSO).getMonthValue() - 1;
                if (mi >= 0 && mi < 12) {
                    recMes[mi] = recMes[mi].add(valor);
                }
            }
        }

        BigDecimal despesas = BigDecimal.ZERO;
        List<NfeEntrada> recebidas = empresaId == null
                ? List.of()
                : nfeEntradaRepository.findByEmpresaIdAndDataEmissaoBetweenOrderByDataEmissaoAscIdAsc(
                        empresaId, de, ate);
        for (var r : recebidas) {
            var valor = r.getValor() != null ? r.getValor() : BigDecimal.ZERO;
            despesas = despesas.add(valor);
            if (r.getDataEmissao() != null) {
                int mi = r.getDataEmissao().getMonthValue() - 1;
                if (mi >= 0 && mi < 12) {
                    desMes[mi] = desMes[mi].add(valor);
                }
            }
        }

        var mensal = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 12; i++) {
            var row = new LinkedHashMap<String, Object>();
            row.put("mes", i + 1);
            row.put("receitas", recMes[i].setScale(2, RoundingMode.HALF_UP));
            row.put("despesas", desMes[i].setScale(2, RoundingMode.HALF_UP));
            mensal.add(row);
        }

        List<Map<String, Object>> ultimasReceitas = autorizadas.stream()
                .sorted(Comparator.comparing(NfeEmissao::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(this::notaCurta)
                .toList();

        var monitor = empresaId == null
                ? Map.<String, Object>of("itens", List.of(), "totalElements", 0)
                : nfeEntradaService.listar(empresaId, null, null, "", 0, 8);
        @SuppressWarnings("unchecked")
        var monitorItens = (List<Map<String, Object>>) monitor.getOrDefault("itens", List.of());

        var body = new LinkedHashMap<String, Object>();
        body.put("ano", ano);
        body.put("de", de.toString());
        body.put("ate", ate.toString());
        body.put("empresaId", empresaId);
        body.put("receitas", receitas.setScale(2, RoundingMode.HALF_UP));
        body.put("despesas", despesas.setScale(2, RoundingMode.HALF_UP));
        body.put("resultado", receitas.subtract(despesas).setScale(2, RoundingMode.HALF_UP));
        body.put("totalLancamentos", autorizadas.size());
        body.put("mensal", mensal);
        body.put("ultimasNotas", ultimasReceitas);
        body.put("monitorFiscal", monitorItens);
        body.put("monitorTotal", monitor.getOrDefault("totalElements", 0));
        body.put("notasEmitidasTotal", autorizadas.size());
        body.put("nfeAutorizadasValor", receitas.setScale(2, RoundingMode.HALF_UP));
        body.put("nfeAutorizadasQtd", autorizadas.size());
        body.put("nfeRecebidasValor", despesas.setScale(2, RoundingMode.HALF_UP));
        body.put("nfeRecebidasQtd", recebidas.size());
        body.put("emitentesNoEscopo", empresaId == null ? 0 : 1);
        return body;
    }

    private Map<String, Object> notaCurta(NfeEmissao n) {
        var m = new LinkedHashMap<String, Object>();
        m.put("data", n.getCreatedAt() != null
                ? n.getCreatedAt().atZone(FUSO).toLocalDate().toString()
                : null);
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        m.put("origem", "NF-e");
        m.put("numero", n.getNumero());
        m.put("chave", n.getChave());
        m.put("contraparte", n.getDestinatarioNome());
        m.put("valor", n.getValorTotal());
        m.put("movimento", "RECEITA");
        m.put("statusProtocolo", n.getStatusProtocolo() != null ? n.getStatusProtocolo() : "100");
        m.put("statusLabel", "Autorizada");
        return m;
    }
}
