package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.repository.ConfiguracaoDocumentoRepository;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumeracaoNfeService {

    private final ConfiguracaoDocumentoRepository documentoRepository;
    private final EmpresaEnderecoRepository enderecoRepository;
    private final NfeUltimoNumeroService ultimoNumeroService;
    private final EmpresaRepository empresaRepository;

    public NumeracaoNfeService(
            ConfiguracaoDocumentoRepository documentoRepository,
            EmpresaEnderecoRepository enderecoRepository,
            NfeUltimoNumeroService ultimoNumeroService,
            EmpresaRepository empresaRepository) {
        this.documentoRepository = documentoRepository;
        this.enderecoRepository = enderecoRepository;
        this.ultimoNumeroService = ultimoNumeroService;
        this.empresaRepository = empresaRepository;
    }

    public record ReservaNumeracao(String serie, long numero, Long enderecoId) {}

    @Transactional
    public ReservaNumeracao reservar(Long empresaId, String tipo, Long enderecoId) {
        if (enderecoId != null) {
            var end = enderecoRepository.findLockedById(enderecoId)
                    .filter(e -> e.getEmpresaId().equals(empresaId))
                    .orElseThrow(() -> new IllegalArgumentException("Endereco nao encontrado"));
            var empresa = empresaRepository.findById(empresaId).orElseThrow();
            end = ultimoNumeroService.sincronizarEndereco(empresa, end);
            long proximo = end.getUltimoNumeroNfe() + 1;
            end.setUltimoNumeroNfe(proximo);
            enderecoRepository.save(end);
            return new ReservaNumeracao(end.getSerieNfe(), proximo, enderecoId);
        }
        ultimoNumeroService.sincronizarEmpresa(empresaId);
        var doc = documentoRepository.findLockedByEmpresaIdAndTipo(empresaId, tipo)
                .orElseThrow(() -> new IllegalStateException("Configure " + tipo + " da empresa"));
        long proximo = doc.getUltimoNumero() + 1;
        doc.setUltimoNumero(proximo);
        documentoRepository.save(doc);
        return new ReservaNumeracao(doc.getSerie(), proximo, null);
    }

    @Transactional
    public void liberar(Long empresaId, String tipo, Long enderecoId, long numeroReservado) {
        if (enderecoId != null) {
            enderecoRepository.findById(enderecoId).ifPresent(end -> {
                if (end.getEmpresaId().equals(empresaId)
                        && end.getUltimoNumeroNfe() == numeroReservado
                        && numeroReservado > 0) {
                    end.setUltimoNumeroNfe(numeroReservado - 1);
                    enderecoRepository.save(end);
                }
            });
            return;
        }
        documentoRepository.findByEmpresaIdAndTipo(empresaId, tipo).ifPresent(doc -> {
            if (doc.getUltimoNumero() == numeroReservado && numeroReservado > 0) {
                doc.setUltimoNumero(numeroReservado - 1);
                documentoRepository.save(doc);
            }
        });
    }
}
