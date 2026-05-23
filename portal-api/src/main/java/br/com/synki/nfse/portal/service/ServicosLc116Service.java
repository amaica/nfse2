package br.com.synki.nfse.portal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ServicosLc116Service {

    public record ServicoLc116(String codigo, String codigoNacional, String descricao, String label, List<String> grupos) {}

    private List<ServicoLc116> catalogo = List.of();
    private int totalAgro;
    private int totalMecanico;

    @PostConstruct
    void carregar() throws Exception {
        try (InputStream in = new ClassPathResource("lc116-servicos.json").getInputStream()) {
            var raw = new ObjectMapper().readValue(in, new TypeReference<List<ServicoLc116Json>>() {});
            var list = new ArrayList<ServicoLc116>();
            for (var s : raw) {
                var grupos = classificarGrupos(s.codigo(), s.descricao());
                list.add(new ServicoLc116(s.codigo(), s.codigoNacional(), s.descricao(), s.label(), List.copyOf(grupos)));
            }
            catalogo = List.copyOf(list);
            totalAgro = (int) catalogo.stream().filter(s -> s.grupos().contains("agro")).count();
            totalMecanico = (int) catalogo.stream().filter(s -> s.grupos().contains("mecanico")).count();
        }
    }

    private record ServicoLc116Json(String codigo, String codigoNacional, String descricao, String label) {}

    public List<ServicoLc116> buscar(String termo, int limite, String grupo) {
        int max = limite <= 0 ? 400 : Math.min(limite, 400);
        var base = filtrarPorGrupo(catalogo, grupo);
        List<ServicoLc116> candidatos;
        if (termo == null || termo.isBlank()) {
            candidatos = ordenarComPrioridade(base, grupo);
        } else {
            final var tokens = normalizar(termo).split("\\s+");
            candidatos = new ArrayList<>();
            for (var s : base) {
                var hay = normalizar(s.codigo() + " " + s.codigoNacional() + " " + s.descricao());
                boolean ok = true;
                for (var t : tokens) {
                    if (!t.isEmpty() && !hay.contains(t)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    candidatos.add(s);
                }
            }
        }
        if (candidatos.size() <= max) {
            return candidatos;
        }
        return candidatos.subList(0, max);
    }

    public int total() {
        return catalogo.size();
    }

    public int totalAgro() {
        return totalAgro;
    }

    public int totalMecanico() {
        return totalMecanico;
    }

    private static List<ServicoLc116> filtrarPorGrupo(List<ServicoLc116> lista, String grupo) {
        if (grupo == null || grupo.isBlank() || "todos".equalsIgnoreCase(grupo)) {
            return lista;
        }
        var g = grupo.trim().toLowerCase(Locale.ROOT);
        return lista.stream().filter(s -> s.grupos().contains(g)).toList();
    }

    private static List<ServicoLc116> ordenarComPrioridade(List<ServicoLc116> lista, String grupo) {
        if (grupo != null && !grupo.isBlank() && !"todos".equalsIgnoreCase(grupo)) {
            return lista;
        }
        return lista.stream()
                .sorted(Comparator
                        .comparingInt(ServicosLc116Service::prioridade)
                        .thenComparing(ServicoLc116::codigo))
                .toList();
    }

    private static int prioridade(ServicoLc116 s) {
        if (s.grupos().contains("agro")) return 0;
        if (s.grupos().contains("mecanico")) return 1;
        return 2;
    }

    static Set<String> classificarGrupos(String codigo, String descricao) {
        var grupos = new HashSet<String>();
        var cod = codigo != null ? codigo : "";
        var d = normalizar(descricao != null ? descricao : "");

        if (cod.startsWith("05.")
                || cod.startsWith("07.01.02")
                || cod.startsWith("07.16.")
                || cod.startsWith("30.")
                || contem(d, "agronom", "agropecu", "zootec", "veterin", "pecuar", "rural",
                "colheit", "plantio", "semead", "adubac", "silvicult", "reflorest", "silagem",
                "irrigac", "florest", "agricult", "trator", "implemento agric")) {
            grupos.add("agro");
        }

        if (cod.startsWith("14.")
                || cod.startsWith("31.01.")
                || contem(d, "mecanic", "maquina", "veicul", "motor", "lubrific", "conserto",
                "recondicionamento de motor", "funilar", "lanternag", "pneu", "recauchut",
                "oficina", "reparo", "revisao", "manutencao e conservacao de maquinas",
                "assistencia tecnica", "guincho", "guindaste")) {
            grupos.add("mecanico");
        }

        return grupos;
    }

    private static boolean contem(String texto, String... termos) {
        for (var t : termos) {
            if (texto.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizar(String s) {
        var n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9.\\s]", " ");
    }
}
