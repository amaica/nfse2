package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.UsoMensalRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ContaMetricasService {

    private static final ZoneId TZ = ZoneId.of("America/Sao_Paulo");

    private final UsoMensalRepository usoMensalRepository;
    private final AssinaturaService assinaturaService;
    private final AuditLogService auditLogService;

    public ContaMetricasService(
            UsoMensalRepository usoMensalRepository,
            AssinaturaService assinaturaService,
            AuditLogService auditLogService) {
        this.usoMensalRepository = usoMensalRepository;
        this.assinaturaService = assinaturaService;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> painel(Long contaId) {
        var resumo = assinaturaService.statusConta(contaId);
        var historico = usoMensalRepository.findByContaIdOrderByAnoMesDesc(contaId, PageRequest.of(0, 12))
                .stream()
                .map(u -> Map.<String, Object>of(
                        "anoMes", u.getAnoMes(),
                        "nfse", u.getNfseCount(),
                        "nfe", u.getNfeCount()))
                .toList();

        var mesAtual = YearMonth.now(TZ).toString();
        int nfseMes = historico.stream()
                .filter(h -> mesAtual.equals(h.get("anoMes")))
                .map(h -> (Integer) h.get("nfse"))
                .findFirst()
                .orElse(0);
        int nfeMes = historico.stream()
                .filter(h -> mesAtual.equals(h.get("anoMes")))
                .map(h -> (Integer) h.get("nfe"))
                .findFirst()
                .orElse(0);

        var body = new LinkedHashMap<String, Object>(resumo);
        body.put("historicoMensal", historico);
        body.put("eventosAudit30Dias", auditLogService.contarUltimos30Dias(contaId));
        body.put("nfseMesAtual", nfseMes);
        body.put("nfeMesAtual", nfeMes);
        body.put("geradoEm", Instant.now().toString());
        return body;
    }
}
