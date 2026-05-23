package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.NfseLog;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final NfseLogRepository repository;

    public AuditLogService(NfseLogRepository repository) {
        this.repository = repository;
    }

    public void log(Long empresaId, Long usuarioId, String acao, String descricao) {
        repository.save(NfseLog.of(empresaId, usuarioId, acao, descricao));
    }
}
