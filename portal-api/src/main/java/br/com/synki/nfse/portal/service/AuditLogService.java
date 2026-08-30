package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.AuditEvent;
import br.com.synki.nfse.portal.domain.NfseLog;
import br.com.synki.nfse.portal.repository.AuditEventRepository;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private final NfseLogRepository nfseLogRepository;
    private final AuditEventRepository auditEventRepository;
    private final MembershipService membershipService;

    public AuditLogService(
            NfseLogRepository nfseLogRepository,
            AuditEventRepository auditEventRepository,
            MembershipService membershipService) {
        this.nfseLogRepository = nfseLogRepository;
        this.auditEventRepository = auditEventRepository;
        this.membershipService = membershipService;
    }

    public void log(Long empresaId, Long usuarioId, String acao, String descricao) {
        nfseLogRepository.save(NfseLog.of(empresaId, usuarioId, acao, descricao));
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId != null) {
            auditEventRepository.save(AuditEvent.of(
                    contaId,
                    empresaId,
                    usuarioId,
                    acao,
                    null,
                    descricao,
                    ipAtual()));
        }
    }

    public void logConta(Long contaId, Long empresaId, Long usuarioId, String acao, String recurso, String detalhe) {
        auditEventRepository.save(AuditEvent.of(
                contaId, empresaId, usuarioId, acao, recurso, detalhe, ipAtual()));
    }

    public Map<String, Object> listarConta(Long contaId, int pagina, int limite) {
        int size = limite <= 0 ? 50 : Math.min(limite, 200);
        int page = Math.max(0, pagina);
        var pageable = PageRequest.of(page, size);
        var itens = auditEventRepository.findByContaIdOrderByCreatedAtDesc(contaId, pageable).stream()
                .map(this::toMap)
                .toList();
        var body = new LinkedHashMap<String, Object>();
        body.put("pagina", page);
        body.put("limite", size);
        body.put("itens", itens);
        return body;
    }

    public List<Map<String, Object>> eventosRecentesExport(Long contaId, Instant desde) {
        return auditEventRepository
                .findByContaIdAndCreatedAtAfterOrderByCreatedAtDesc(
                        contaId, desde, PageRequest.of(0, 500))
                .stream()
                .map(this::toMap)
                .toList();
    }

    public long contarUltimos30Dias(Long contaId) {
        return auditEventRepository.countByContaIdAndCreatedAtAfter(
                contaId, Instant.now().minusSeconds(30L * 86400));
    }

    private Map<String, Object> toMap(AuditEvent e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("empresaId", e.getEmpresaId());
        m.put("usuarioId", e.getUsuarioId());
        m.put("acao", e.getAcao());
        m.put("recurso", e.getRecurso());
        m.put("detalhe", e.getDetalhe());
        m.put("ip", e.getIp());
        m.put("createdAt", e.getCreatedAt().toString());
        return m;
    }

    private static String ipAtual() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            HttpServletRequest req = servlet.getRequest();
            var forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }
        return null;
    }
}
