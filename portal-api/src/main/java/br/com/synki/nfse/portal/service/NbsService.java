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
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NbsService {

    public record NbsItem(String codigo, String codigoNacional, String descricao, String label) {}

    private List<NbsItem> catalogo = List.of();

    private static final Map<String, String> LC116_PREFIXO_NBS = Map.ofEntries(
            Map.entry("0101", "114061"),
            Map.entry("0107", "114061"),
            Map.entry("0103", "114061"),
            Map.entry("0105", "114062"),
            Map.entry("0106", "114051"),
            Map.entry("0501", "114055"),
            Map.entry("0701", "101011"),
            Map.entry("0702", "101011"),
            Map.entry("0716", "101011"),
            Map.entry("1401", "112011"),
            Map.entry("1402", "112011"),
            Map.entry("1403", "112011"),
            Map.entry("1412", "112011"),
            Map.entry("1706", "114061"),
            Map.entry("3101", "118031")
    );

    @PostConstruct
    void carregar() throws Exception {
        try (InputStream in = new ClassPathResource("nbs-servicos.json").getInputStream()) {
            catalogo = new ObjectMapper().readValue(in, new TypeReference<>() {});
        }
    }

    public List<NbsItem> buscar(String termo, int limite, String lc116) {
        int max = limite <= 0 ? 30 : Math.min(limite, 50);
        var termoLimpo = termo != null ? termo.trim() : "";

        if (termoLimpo.isEmpty()) {
            if (lc116 != null && !lc116.isBlank()) {
                return sugerirPorLc116(lc116, max);
            }
            return List.of();
        }

        var termoNorm = normalizar(termoLimpo);
        var soDigitos = termoLimpo.replaceAll("\\D", "");
        if (termoNorm.length() < 2 && soDigitos.length() < 4) {
            return List.of();
        }

        var scored = new ArrayList<Scored>();
        for (var n : catalogo) {
            int score = pontuar(n, termoNorm, soDigitos);
            if (score > 0) {
                scored.add(new Scored(n, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(s -> s.item().descricao()));
        return scored.stream().limit(max).map(Scored::item).toList();
    }

    private record Scored(NbsItem item, int score) {}

    private static int pontuar(NbsItem n, String termoNorm, String soDigitos) {
        var desc = normalizar(n.descricao());
        var cod = n.codigoNacional();
        int score = 0;
        if (!soDigitos.isEmpty() && cod.contains(soDigitos)) {
            score += 50 + (cod.equals(soDigitos) ? 100 : 0);
        }
        for (var token : termoNorm.split("\\s+")) {
            if (token.length() < 2) {
                continue;
            }
            if (desc.contains(token)) {
                score += 30;
                if (desc.startsWith(token)) {
                    score += 20;
                }
            }
        }
        return score;
    }

    public List<NbsItem> sugerirPorLc116(String lc116, int limite) {
        var digits = lc116.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return List.of();
        }
        var prefix4 = digits.substring(0, 4);
        var nbs6 = LC116_PREFIXO_NBS.get(prefix4);
        var result = new ArrayList<NbsItem>();
        if (nbs6 != null) {
            for (var n : catalogo) {
                if (n.codigoNacional().startsWith(nbs6)) {
                    result.add(n);
                    if (result.size() >= limite) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public int total() {
        return catalogo.size();
    }

    private static String normalizar(String s) {
        var n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9.\\s]", " ");
    }
}
