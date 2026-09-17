package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.fiscal.livrocaixa.LancamentoLivroCaixa;
import br.com.synki.nfse.portal.fiscal.livrocaixa.XmlNotasColetorService;
import com.fincatto.documentofiscal.DFModelo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PainelHomeService {

    private final XmlNotasColetorService coletor;
    private final EmissaoNfeService emissaoNfeService;
    private final NfeEntradaService nfeEntradaService;

    public PainelHomeService(
            XmlNotasColetorService coletor,
            EmissaoNfeService emissaoNfeService,
            NfeEntradaService nfeEntradaService) {
        this.coletor = coletor;
        this.emissaoNfeService = emissaoNfeService;
        this.nfeEntradaService = nfeEntradaService;
    }

    public Map<String, Object> home(Long empresaId, Integer anoParam) {
        int ano = anoParam != null && anoParam >= 2000 && anoParam <= 2100
                ? anoParam
                : Year.now().getValue();
        LocalDate de = LocalDate.of(ano, 1, 1);
        LocalDate ate = LocalDate.of(ano, 12, 31);
        if (ate.isAfter(LocalDate.now())) {
            ate = LocalDate.now();
        }

        List<LancamentoLivroCaixa> lancamentos = List.of();
        try {
            lancamentos = coletor.coletar(empresaId, de, ate, true, true);
        } catch (Exception ignored) {
            // período sem XML — zeros
        }

        BigDecimal receitas = BigDecimal.ZERO;
        BigDecimal despesas = BigDecimal.ZERO;
        BigDecimal[] recMes = new BigDecimal[12];
        BigDecimal[] desMes = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            recMes[i] = BigDecimal.ZERO;
            desMes[i] = BigDecimal.ZERO;
        }
        for (var l : lancamentos) {
            int mi = l.data().getMonthValue() - 1;
            if (l.tipoMovimento() == LancamentoLivroCaixa.TipoMovimento.RECEITA) {
                receitas = receitas.add(l.valor());
                if (mi >= 0 && mi < 12) {
                    recMes[mi] = recMes[mi].add(l.valor());
                }
            } else {
                despesas = despesas.add(l.valor());
                if (mi >= 0 && mi < 12) {
                    desMes[mi] = desMes[mi].add(l.valor());
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

        List<Map<String, Object>> ultimasReceitas = new ArrayList<>(lancamentos.stream()
                .filter(l -> l.tipoMovimento() == LancamentoLivroCaixa.TipoMovimento.RECEITA)
                .sorted(Comparator.comparing(LancamentoLivroCaixa::data).reversed())
                .limit(8)
                .map(this::lancamentoCurto)
                .toList());

        if (ultimasReceitas.isEmpty()) {
            var page = emissaoNfeService.listarNotas(empresaId, DFModelo.NFE, 0, 8);
            @SuppressWarnings("unchecked")
            var itens = (List<Map<String, Object>>) page.getOrDefault("itens", List.of());
            ultimasReceitas = new ArrayList<>(itens);
        }

        var monitor = nfeEntradaService.listar(empresaId, null, null, "", 0, 8);
        @SuppressWarnings("unchecked")
        var monitorItens = (List<Map<String, Object>>) monitor.getOrDefault("itens", List.of());

        Object notasTotal = 0;
        try {
            var pageCount = emissaoNfeService.listarNotasFiltradas(
                    empresaId, DFModelo.NFE, 0, 1, de, ate, null, null, null, null);
            notasTotal = pageCount.getOrDefault("totalElements", 0);
        } catch (Exception ignored) {
            notasTotal = 0;
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("ano", ano);
        body.put("de", de.toString());
        body.put("ate", ate.toString());
        body.put("receitas", receitas.setScale(2, RoundingMode.HALF_UP));
        body.put("despesas", despesas.setScale(2, RoundingMode.HALF_UP));
        body.put("resultado", receitas.subtract(despesas).setScale(2, RoundingMode.HALF_UP));
        body.put("totalLancamentos", lancamentos.size());
        body.put("mensal", mensal);
        body.put("ultimasNotas", ultimasReceitas);
        body.put("monitorFiscal", monitorItens);
        body.put("monitorTotal", monitor.getOrDefault("totalElements", 0));
        body.put("notasEmitidasTotal", notasTotal);
        return body;
    }

    private Map<String, Object> lancamentoCurto(LancamentoLivroCaixa l) {
        var m = new LinkedHashMap<String, Object>();
        m.put("data", l.data().toString());
        m.put("origem", l.origem());
        m.put("numero", l.numeroDocumento());
        m.put("chave", l.chaveAcesso());
        m.put("contraparte", l.contraparteNome());
        m.put("valor", l.valor());
        m.put("movimento", l.tipoMovimento().name());
        m.put("statusProtocolo", "100");
        m.put("statusLabel", "Autorizada");
        return m;
    }
}
