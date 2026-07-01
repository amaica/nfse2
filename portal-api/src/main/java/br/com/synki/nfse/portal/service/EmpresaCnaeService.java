package br.com.synki.nfse.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmpresaCnaeService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaCnaeService.class);

    public record CnaeEmpresa(String codigo, String descricao, boolean principal) {}

    private final Map<String, List<CnaeEmpresa>> cache = new LinkedHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<CnaeEmpresa> obterCnaes(String cnpj) {
        var doc = cnpj == null ? "" : cnpj.replaceAll("\\D", "");
        if (doc.length() != 14) {
            return List.of();
        }
        synchronized (cache) {
            if (cache.containsKey(doc)) {
                return cache.get(doc);
            }
        }
        var lista = consultar(doc);
        synchronized (cache) {
            cache.put(doc, lista);
        }
        return lista;
    }

    private List<CnaeEmpresa> consultar(String cnpj) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://brasilapi.com.br/api/cnpj/v1/" + cnpj))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return List.of();
            }
            var root = mapper.readTree(res.body());
            var out = new ArrayList<CnaeEmpresa>();
            var principal = root.path("cnae_fiscal").asText("").replaceAll("\\D", "");
            var descPrincipal = root.path("cnae_fiscal_descricao").asText("").trim();
            if (!principal.isEmpty()) {
                out.add(new CnaeEmpresa(principal, descPrincipal, true));
            }
            for (JsonNode sec : root.path("cnaes_secundarios")) {
                var cod = sec.path("codigo").asText("").replaceAll("\\D", "");
                var desc = sec.path("descricao").asText("").trim();
                if (!cod.isEmpty() && out.stream().noneMatch(c -> c.codigo().equals(cod))) {
                    out.add(new CnaeEmpresa(cod, desc, false));
                }
            }
            return List.copyOf(out);
        } catch (Exception e) {
            log.warn("Falha ao consultar CNAEs do CNPJ {}: {}", cnpj, e.getMessage());
            return List.of();
        }
    }
}
