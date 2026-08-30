package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.NfseLog;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NfseHistoricoService {

    private static final Pattern CHAVE = Pattern.compile("(\\d{50})");

    private final NfseLogRepository logRepository;

    public NfseHistoricoService(NfseLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public List<Map<String, Object>> listarEmitidas(Long empresaId, String q, int limite) {
        int cap = Math.min(Math.max(limite, 1), 500);
        var logs = logRepository.findByEmpresaIdAndAcaoOrderByCreatedAtDesc(
                empresaId, "EMISSAO", PageRequest.of(0, cap));
        var termo = q != null ? q.trim().toLowerCase(Locale.ROOT) : "";
        var resultado = new ArrayList<Map<String, Object>>();
        for (NfseLog log : logs) {
            var item = resumoEmissao(log);
            if (!termo.isEmpty()) {
                var chave = String.valueOf(item.get("chave")).toLowerCase(Locale.ROOT);
                var desc = String.valueOf(item.get("descricao")).toLowerCase(Locale.ROOT);
                if (!chave.contains(termo) && !desc.contains(termo)) {
                    continue;
                }
            }
            resultado.add(item);
        }
        return resultado;
    }

    private static Map<String, Object> resumoEmissao(NfseLog log) {
        var chave = extrairChave(log.getDescricao());
        var body = new LinkedHashMap<String, Object>();
        body.put("id", log.getId());
        body.put("chave", chave != null ? chave : "");
        body.put("descricao", log.getDescricao() != null ? log.getDescricao() : "");
        body.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
        body.put("status", "Emitida");
        return body;
    }

    private static String extrairChave(String descricao) {
        if (descricao == null) {
            return null;
        }
        var m = CHAVE.matcher(descricao);
        return m.find() ? m.group(1) : null;
    }
}
