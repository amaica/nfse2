package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumeracaoNfseService {

    private final ConfiguracaoNfseRepository configuracaoRepository;

    public NumeracaoNfseService(ConfiguracaoNfseRepository configuracaoRepository) {
        this.configuracaoRepository = configuracaoRepository;
    }

    @Transactional
    public long reservarProximoNumero(Long empresaId) {
        var cfg = configuracaoRepository.findByEmpresaId(empresaId, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(() -> new IllegalStateException("Configuracao NFS-e nao encontrada"));
        long proximo = cfg.getUltimoNumeroNfse() + 1;
        cfg.setUltimoNumeroNfse(proximo);
        configuracaoRepository.save(cfg);
        return proximo;
    }

    @Transactional
    public void liberarNumero(Long empresaId, long numeroReservado) {
        var cfg = configuracaoRepository.findByEmpresaId(empresaId).orElse(null);
        if (cfg != null && cfg.getUltimoNumeroNfse() == numeroReservado && numeroReservado > 0) {
            cfg.setUltimoNumeroNfse(numeroReservado - 1);
            configuracaoRepository.save(cfg);
        }
    }
}
