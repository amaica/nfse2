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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ServicosLc116Service {

    public record ServicoLc116(String codigo, String codigoNacional, String descricao, String label, List<String> grupos) {}

    private static final Map<String, List<String>> CNAE_PREFIXO_LC116 = Map.ofEntries(
            Map.entry("86305", List.of("04.02.01.000", "04.02.02.000")),
            Map.entry("86304", List.of("04.02.05.000", "04.02.03.000")),
            Map.entry("86303", List.of("04.03.02.000")),
            Map.entry("86402", List.of("04.02.04.000")),
            Map.entry("86101", List.of("04.01.01.000", "04.01.02.000")),
            Map.entry("62015", List.of("01.07.01.000", "01.01.01.000")),
            Map.entry("62023", List.of("01.03.01.000", "01.04.01.000")),
            Map.entry("62031", List.of("01.06.01.000")),
            Map.entry("62040", List.of("01.02.01.000")),
            Map.entry("63119", List.of("01.03.01.000", "01.08.01.000")),
            Map.entry("63194", List.of("01.08.01.000")),
            Map.entry("45200", List.of("14.01.01.000", "14.03.01.000")),
            Map.entry("33147", List.of("14.01.01.000", "14.02.01.000")),
            Map.entry("01610", List.of("07.16.01.000", "07.01.02.000")),
            Map.entry("01113", List.of("07.16.01.000")),
            Map.entry("75001", List.of("05.01.01.000")),
            Map.entry("85996", List.of("08.02.01.000")),
            Map.entry("70204", List.of("17.01.01.000")),
            Map.entry("69117", List.of("17.14.01.000")),
            Map.entry("82199", List.of("17.02.01.000")),
            Map.entry("69206", List.of("17.19.01.000"))
    );

    private final CnaeService cnaeService;
    private List<ServicoLc116> catalogo = List.of();
    private int totalAgro;
    private int totalMecanico;

    public ServicosLc116Service(CnaeService cnaeService) {
        this.cnaeService = cnaeService;
    }

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
        return buscar(termo, limite, grupo, List.of());
    }

    public List<ServicoLc116> buscar(String termo, int limite, String grupo, List<String> cnaes) {
        int max = limite <= 0 ? 400 : Math.min(limite, 400);
        var poolCnae = cnaes != null && !cnaes.isEmpty() ? sugerirPorCnaes(cnaes, 120) : List.<ServicoLc116>of();
        var base = poolCnae.isEmpty()
                ? filtrarPorGrupo(catalogo, grupo)
                : poolCnae;
        List<ServicoLc116> candidatos;
        if (termo == null || termo.isBlank()) {
            candidatos = poolCnae.isEmpty() ? ordenarComPrioridade(base, grupo) : base;
        } else {
            final var tokens = normalizar(termo).split("\\s+");
            var fonte = poolCnae.isEmpty() ? filtrarPorGrupo(catalogo, grupo) : poolCnae;
            candidatos = new ArrayList<>();
            for (var s : fonte) {
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

    public List<ServicoLc116> sugerirPorCnaes(List<String> cnaes, int limite) {
        if (cnaes == null || cnaes.isEmpty()) {
            return List.of();
        }
        var codigosLc = new LinkedHashSet<String>();
        for (var cnae : cnaes) {
            var digits = cnae.replaceAll("\\D", "");
            if (digits.length() < 4) {
                continue;
            }
            for (var entry : CNAE_PREFIXO_LC116.entrySet()) {
                if (digits.startsWith(entry.getKey())) {
                    codigosLc.addAll(entry.getValue());
                }
            }
            var descricao = cnaeService.porCodigo(digits)
                    .map(CnaeService.CnaeItem::descricao)
                    .orElse("");
            if (descricao.isBlank() && digits.length() >= 7) {
                descricao = cnae;
            }
            if (!descricao.isBlank()) {
                correlacionarPorTexto(descricao).stream()
                        .limit(6)
                        .map(ServicoLc116::codigo)
                        .forEach(codigosLc::add);
            }
        }
        if (codigosLc.isEmpty()) {
            return List.of();
        }
        var mapa = catalogo.stream().collect(Collectors.toMap(ServicoLc116::codigo, s -> s, (a, b) -> a));
        var out = new ArrayList<ServicoLc116>();
        for (var cod : codigosLc) {
            var s = mapa.get(cod);
            if (s != null) {
                out.add(s);
            }
            if (out.size() >= limite) {
                break;
            }
        }
        return out;
    }

    private List<ServicoLc116> correlacionarPorTexto(String descricaoCnae) {
        var tokens = extrairTokens(descricaoCnae);
        if (tokens.isEmpty()) {
            return List.of();
        }
        var scored = new ArrayList<Scored>();
        for (var s : catalogo) {
            var hay = normalizar(s.descricao());
            int score = 0;
            for (var t : tokens) {
                if (hay.contains(t)) {
                    score += t.length() >= 5 ? 25 : 12;
                }
            }
            if (score > 0) {
                scored.add(new Scored(s, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed().thenComparing(s -> s.item().codigo()));
        return scored.stream().map(Scored::item).toList();
    }

    private static List<String> extrairTokens(String texto) {
        var stop = Set.of("de", "da", "do", "das", "dos", "e", "em", "na", "no", "para", "por", "com", "sem", "ou", "aos", "as");
        var out = new ArrayList<String>();
        for (var t : normalizar(texto).split("\\s+")) {
            if (t.length() >= 4 && !stop.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private record Scored(ServicoLc116 item, int score) {}

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
