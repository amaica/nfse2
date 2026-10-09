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

    /**
     * Ajuste manual: o usuário define a série e o último número já usado.
     * A próxima emissão será {@code ultimoNumero + 1}. Pode avançar ou retroceder.
     */
    @Transactional
    public MapNumeracao ajustar(Long empresaId, String tipo, Long enderecoId, String serie, long ultimoNumero) {
        if (ultimoNumero < 0) {
            throw new IllegalArgumentException("Último número inválido");
        }
        String serieNorm = NfeUltimoNumeroService.normalizarSerie(serie);
        if (enderecoId != null) {
            var end = enderecoRepository.findLockedById(enderecoId)
                    .filter(e -> e.getEmpresaId().equals(empresaId))
                    .orElseThrow(() -> new IllegalArgumentException("Endereco nao encontrado"));
            end.setSerieNfe(serieNorm);
            end.setUltimoNumeroNfe(ultimoNumero);
            enderecoRepository.save(end);
            return new MapNumeracao(serieNorm, ultimoNumero, ultimoNumero + 1);
        }
        var doc = documentoRepository.findLockedByEmpresaIdAndTipo(empresaId, tipo)
                .orElseThrow(() -> new IllegalStateException("Configure " + tipo + " da empresa"));
        doc.setSerie(serieNorm);
        doc.setUltimoNumero(ultimoNumero);
        documentoRepository.save(doc);
        return new MapNumeracao(serieNorm, ultimoNumero, ultimoNumero + 1);
    }

    /** Garante que o último número local seja pelo menos {@code minimo} (ex.: após rejeição 539). */
    @Transactional
    public void garantirMinimo(Long empresaId, String tipo, Long enderecoId, String serie, long minimo) {
        if (minimo <= 0) {
            return;
        }
        String serieNorm = NfeUltimoNumeroService.normalizarSerie(serie);
        long atual = 0;
        if (enderecoId != null) {
            atual = enderecoRepository.findById(enderecoId)
                    .filter(e -> e.getEmpresaId().equals(empresaId))
                    .map(e -> e.getUltimoNumeroNfe())
                    .orElse(0L);
        } else {
            atual = documentoRepository.findByEmpresaIdAndTipo(empresaId, tipo)
                    .map(d -> d.getUltimoNumero())
                    .orElse(0L);
        }
        if (atual < minimo) {
            ajustar(empresaId, tipo, enderecoId, serieNorm, minimo);
        }
    }

    public record MapNumeracao(String serie, long ultimoNumero, long proximoNumero) {}

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
