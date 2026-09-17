package br.com.synki.nfse.portal.fiscal.livrocaixa;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulado de IR PF a partir das notas do portal (caixa: entra $$ / sai $$).
 * Não é apuração oficial — apenas estimativa orientativa.
 */
@Service
public class SimuladoIrService {

    private static final String AVISO = "Simulado orientativo com base nas notas do portal "
            + "(emitidas e DF-e). Classifica NF-e pelo tpNF (1=entra $$, 0=sai $$) e DF-e como saída de $$. "
            + "Não substitui contador, deduções legais nem declaração oficial à RFB.";

    /**
     * Tabela progressiva anual aproximada IRPF (faixas 2025/2026 — atualizar quando a RFB mudar).
     * Limite superior inclusivo; última faixa com limite null = sem teto.
     */
    private static final List<FaixaIr> FAIXAS_ANUAIS = List.of(
            new FaixaIr(bd("22847.76"), bd("0"), bd("0")),
            new FaixaIr(bd("33919.80"), bd("0.075"), bd("1713.58")),
            new FaixaIr(bd("45012.60"), bd("0.15"), bd("4257.57")),
            new FaixaIr(bd("55976.16"), bd("0.225"), bd("7633.51")),
            new FaixaIr(null, bd("0.275"), bd("10432.32"))
    );

    private final XmlNotasColetorService coletor;

    public SimuladoIrService(XmlNotasColetorService coletor) {
        this.coletor = coletor;
    }

    public Map<String, Object> simular(Long empresaId, LocalDate de, LocalDate ate, boolean nfse, boolean nfe)
            throws Exception {
        var lancamentos = coletor.coletar(empresaId, de, ate, nfse, nfe);
        var entradas = BigDecimal.ZERO;
        var saidas = BigDecimal.ZERO;
        var itens = new ArrayList<Map<String, Object>>();
        for (var l : lancamentos) {
            boolean entra = l.tipoMovimento() == LancamentoLivroCaixa.TipoMovimento.RECEITA;
            if (entra) {
                entradas = entradas.add(l.valor());
            } else {
                saidas = saidas.add(l.valor());
            }
            itens.add(itemMap(l, entra));
        }
        var resultado = entradas.subtract(saidas).setScale(2, RoundingMode.HALF_UP);
        var base = resultado.signum() > 0 ? resultado : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        var ir = calcularIr(base);

        var body = new LinkedHashMap<String, Object>();
        body.put("de", de.toString());
        body.put("ate", ate.toString());
        body.put("totalNotas", lancamentos.size());
        body.put("entradas", entradas.setScale(2, RoundingMode.HALF_UP));
        body.put("saidas", saidas.setScale(2, RoundingMode.HALF_UP));
        body.put("resultado", resultado);
        body.put("baseCalculo", base);
        body.put("irEstimado", ir.imposto());
        body.put("faixasAplicadas", ir.faixas());
        body.put("tabelaIrVersao", "IRPF-anual-aproximada-2025");
        body.put("aviso", AVISO);
        body.put("fonte", "XMLs de NFS-e/NF-e emitidas (tpNF) e NF-e DF-e contra o emitente");
        body.put("itens", itens);
        return body;
    }

    private static Map<String, Object> itemMap(LancamentoLivroCaixa l, boolean entra) {
        var m = new LinkedHashMap<String, Object>();
        m.put("data", l.data().toString());
        m.put("origem", l.origem());
        m.put("numero", l.numeroDocumento());
        m.put("chave", l.chaveAcesso());
        m.put("historico", l.historico());
        m.put("contraparte", l.contraparteNome());
        m.put("documento", l.contraparteDoc());
        m.put("valor", l.valor());
        m.put("movimento", l.tipoMovimento().name());
        m.put("fluxo", entra ? "ENTRA" : "SAI");
        m.put("tipoNota", l.tipoNota() != null ? l.tipoNota() : "");
        m.put("tipoLabel", labelTipoNota(l));
        return m;
    }

    private static String labelTipoNota(LancamentoLivroCaixa l) {
        if ("NF-e entrada".equals(l.origem())) {
            return "DF-e (compra)";
        }
        if ("NFS-e".equals(l.origem())) {
            return "Serviço";
        }
        if ("0".equals(l.tipoNota())) {
            return "Entrada (tpNF=0)";
        }
        if ("1".equals(l.tipoNota())) {
            return "Saída (tpNF=1)";
        }
        return l.origem();
    }

    static ResultadoIr calcularIr(BigDecimal base) {
        if (base == null || base.signum() <= 0) {
            return new ResultadoIr(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), List.of());
        }
        BigDecimal oficial = impostoOficial(base);
        BigDecimal limiteAnterior = BigDecimal.ZERO;
        var aplicadas = new ArrayList<Map<String, Object>>();
        for (FaixaIr faixa : FAIXAS_ANUAIS) {
            BigDecimal teto = faixa.limiteSuperior();
            boolean encaixa = teto == null || base.compareTo(teto) <= 0;
            if (encaixa) {
                var row = new LinkedHashMap<String, Object>();
                row.put("de", limiteAnterior);
                row.put("ate", teto);
                row.put("aliquota", faixa.aliquota());
                row.put("parcelaDeduzir", faixa.parcelaDeduzir());
                row.put("baseParcial", base);
                row.put("impostoParcial", oficial);
                aplicadas.add(row);
                break;
            }
            limiteAnterior = teto;
        }
        return new ResultadoIr(oficial, aplicadas);
    }

    /** base × alíquota − parcela a deduzir da faixa em que a base se encaixa. */
    private static BigDecimal impostoOficial(BigDecimal base) {
        for (FaixaIr faixa : FAIXAS_ANUAIS) {
            BigDecimal teto = faixa.limiteSuperior();
            if (teto == null || base.compareTo(teto) <= 0) {
                return base.multiply(faixa.aliquota())
                        .subtract(faixa.parcelaDeduzir())
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private record FaixaIr(BigDecimal limiteSuperior, BigDecimal aliquota, BigDecimal parcelaDeduzir) {}

    record ResultadoIr(BigDecimal imposto, List<Map<String, Object>> faixas) {}
}
