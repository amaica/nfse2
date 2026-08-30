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
public class CnpjConsultaPublicaService {

    private static final Logger log = LoggerFactory.getLogger(CnpjConsultaPublicaService.class);

    public record CnaeItem(String codigo, String descricao, boolean principal) {}

    public record DadosCnpj(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String email,
            String telefone,
            String situacaoCadastral,
            boolean optanteSimples,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String municipio,
            String uf,
            String codigoMunicipioIbge,
            String prefeitura,
            String cnaePrincipalCodigo,
            String cnaePrincipalDescricao,
            List<CnaeItem> cnaes
    ) {}

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public DadosCnpj consultar(String cnpj) {
        var doc = apenasDigitos(cnpj);
        if (doc.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve ter 14 digitos");
        }
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://brasilapi.com.br/api/cnpj/v1/" + doc))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404) {
                throw new IllegalArgumentException("CNPJ nao encontrado na Receita Federal");
            }
            if (res.statusCode() != 200) {
                throw new IllegalStateException("Consulta CNPJ indisponivel (HTTP " + res.statusCode() + ")");
            }
            return parse(mapper.readTree(res.body()), doc);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Falha ao consultar CNPJ {}: {}", doc, e.toString());
            throw new IllegalStateException(
                    "Nao foi possivel consultar o CNPJ agora. Verifique sua conexao e tente novamente em instantes.");
        }
    }

    public Map<String, Object> consultarMapa(String cnpj) {
        var d = consultar(cnpj);
        var map = new LinkedHashMap<String, Object>();
        map.put("cnpj", d.cnpj());
        map.put("razaoSocial", d.razaoSocial());
        map.put("nomeFantasia", d.nomeFantasia());
        map.put("email", d.email());
        map.put("telefone", d.telefone());
        map.put("situacaoCadastral", d.situacaoCadastral());
        map.put("optanteSimples", d.optanteSimples());
        map.put("endereco", Map.of(
                "cep", d.cep(),
                "logradouro", d.logradouro(),
                "numero", d.numero(),
                "complemento", d.complemento(),
                "bairro", d.bairro(),
                "municipio", d.municipio(),
                "uf", d.uf(),
                "codigoMunicipioIbge", d.codigoMunicipioIbge()));
        map.put("prefeitura", d.prefeitura());
        map.put("codigoMunicipioIbge", d.codigoMunicipioIbge());
        map.put("cnaePrincipal", Map.of(
                "codigo", d.cnaePrincipalCodigo(),
                "descricao", d.cnaePrincipalDescricao()));
        map.put("cnaes", d.cnaes().stream()
                .map(c -> Map.of(
                        "codigo", c.codigo(),
                        "descricao", c.descricao(),
                        "principal", c.principal()))
                .toList());
        map.put("sugestaoEmailIntegracao", sugerirEmailIntegracao(d));
        return map;
    }

    private DadosCnpj parse(JsonNode root, String doc) {
        var razao = texto(root, "razao_social");
        var fantasia = primeiroNaoVazio(texto(root, "nome_fantasia"), razao);
        var municipio = texto(root, "municipio");
        var uf = texto(root, "uf");
        var ibge = root.path("codigo_municipio_ibge").asText("").replaceAll("\\D", "");
        var cnaeCod = root.path("cnae_fiscal").asText("").replaceAll("\\D", "");
        var cnaeDesc = texto(root, "cnae_fiscal_descricao");

        var cnaes = new ArrayList<CnaeItem>();
        if (!cnaeCod.isEmpty()) {
            cnaes.add(new CnaeItem(cnaeCod, cnaeDesc, true));
        }
        for (JsonNode sec : root.path("cnaes_secundarios")) {
            var cod = sec.path("codigo").asText("").replaceAll("\\D", "");
            var desc = sec.path("descricao").asText("").trim();
            if (!cod.isEmpty() && cnaes.stream().noneMatch(c -> c.codigo().equals(cod))) {
                cnaes.add(new CnaeItem(cod, desc, false));
            }
        }

        var ddd = texto(root, "ddd_telefone_1");
        var tel = ddd.isBlank() ? "" : ddd.replaceAll("\\D", "");

        return new DadosCnpj(
                doc,
                razao,
                fantasia,
                texto(root, "email"),
                tel,
                texto(root, "descricao_situacao_cadastral"),
                root.path("opcao_pelo_simples").asBoolean(false),
                texto(root, "cep").replaceAll("\\D", ""),
                texto(root, "logradouro"),
                texto(root, "numero"),
                texto(root, "complemento"),
                texto(root, "bairro"),
                municipio,
                uf,
                ibge,
                municipio.isBlank() ? "" : municipio + "/" + uf,
                cnaeCod,
                cnaeDesc,
                List.copyOf(cnaes));
    }

    private static String sugerirEmailIntegracao(DadosCnpj d) {
        if (d.email() != null && !d.email().isBlank()) {
            return d.email().trim().toLowerCase();
        }
        return "nfse." + d.cnpj() + "@integracao.local";
    }

    private static String texto(JsonNode n, String campo) {
        return n.path(campo).asText("").trim();
    }

    private static String primeiroNaoVazio(String... vals) {
        for (var v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
