package br.com.synki.nfse.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CnaeService {

    private static final Logger log = LoggerFactory.getLogger(CnaeService.class);
    private static final URI IBGE_CNAE =
            URI.create("https://servicodados.ibge.gov.br/api/v2/cnae/subclasses");

    public record CnaeItem(String codigo, String codigoFormatado, String descricao, String label) {}

    private List<CnaeItem> catalogo = List.of();

    @PostConstruct
    void carregar() {
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            var req = HttpRequest.newBuilder(IBGE_CNAE).timeout(Duration.ofSeconds(30)).GET().build();
            var res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new IllegalStateException("IBGE retornou HTTP " + res.statusCode());
            }
            var root = new ObjectMapper().readTree(res.body());
            var list = new ArrayList<CnaeItem>();
            for (JsonNode node : root) {
                var id = node.path("id").asText("").trim();
                var desc = node.path("descricao").asText("").trim();
                if (id.isEmpty() || desc.isEmpty()) {
                    continue;
                }
                var fmt = formatar(id);
                var label = fmt + " — " + titulo(desc);
                list.add(new CnaeItem(id, fmt, desc, label));
            }
            catalogo = List.copyOf(list);
            log.info("CNAE IBGE carregado: {} subclasses", catalogo.size());
        } catch (Exception e) {
            log.warn("Falha ao carregar CNAE do IBGE, usando lista reduzida: {}", e.getMessage());
            catalogo = fallback();
        }
    }

    public List<CnaeItem> buscar(String termo, int limite) {
        int max = limite <= 0 ? 40 : Math.min(limite, 80);
        if (termo == null || termo.isBlank()) {
            return catalogo.stream().limit(max).toList();
        }
        var termoLimpo = termo.trim();
        var termoNorm = normalizar(termoLimpo);
        var soDigitos = termoLimpo.replaceAll("\\D", "");
        if (termoNorm.length() < 2 && soDigitos.length() < 3) {
            return List.of();
        }

        var scored = new ArrayList<Scored>();
        for (var c : catalogo) {
            int score = pontuar(c, termoNorm, soDigitos);
            if (score > 0) {
                scored.add(new Scored(c, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(s -> s.item().descricao()));
        return scored.stream().limit(max).map(Scored::item).toList();
    }

    public int total() {
        return catalogo.size();
    }

    private record Scored(CnaeItem item, int score) {}

    private static int pontuar(CnaeItem c, String termoNorm, String soDigitos) {
        var desc = normalizar(c.descricao());
        var cod = c.codigo();
        var fmt = c.codigoFormatado().replaceAll("\\D", "");
        int score = 0;
        if (!soDigitos.isEmpty()) {
            if (cod.contains(soDigitos) || fmt.contains(soDigitos)) {
                score += 50 + (cod.equals(soDigitos) ? 100 : 0);
            }
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

    private static String formatar(String id) {
        var d = id.replaceAll("\\D", "");
        if (d.length() != 7) {
            return id;
        }
        return d.substring(0, 2) + "." + d.substring(2, 4) + "-" + d.substring(4, 5) + "/" + d.substring(5, 7);
    }

    private static String titulo(String s) {
        if (s.length() <= 90) {
            return s;
        }
        return s.substring(0, 87) + "…";
    }

    private static List<CnaeItem> fallback() {
        return List.of(
                new CnaeItem("0111301", "01.11-3/01", "CULTIVO DE ARROZ", "01.11-3/01 — CULTIVO DE ARROZ"),
                new CnaeItem("0115600", "01.15-6/00", "CULTIVO DE SOJA", "01.15-6/00 — CULTIVO DE SOJA"),
                new CnaeItem("0161001", "01.61-0/01", "SERVIÇO DE PULVERIZAÇÃO E CONTROLE DE PRAGAS AGRÍCOLAS", "01.61-0/01 — SERVIÇO DE PULVERIZAÇÃO…"),
                new CnaeItem("3314707", "33.14-7/07", "MANUTENÇÃO E REPARAÇÃO DE MÁQUINAS E EQUIPAMENTOS AGRÍCOLAS", "33.14-7/07 — MANUTENÇÃO… AGRÍCOLAS"),
                new CnaeItem("4520001", "45.20-0/01", "SERVIÇOS DE MANUTENÇÃO E REPARAÇÃO MECÂNICA DE VEÍCULOS AUTOMOTORES", "45.20-0/01 — MANUTENÇÃO MECÂNICA…"),
                new CnaeItem("6201501", "62.01-5/01", "DESENVOLVIMENTO DE PROGRAMAS DE COMPUTADOR SOB ENCOMENDA", "62.01-5/01 — DESENVOLVIMENTO DE SOFTWARE…")
        );
    }

    private static String normalizar(String s) {
        var n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9.\\s]", " ");
    }
}
