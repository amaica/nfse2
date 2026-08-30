package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.FluxoImportProperties;
import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.repository.ConfiguracaoDocumentoRepository;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class NfeUltimoNumeroService {

    private static final Logger log = LoggerFactory.getLogger(NfeUltimoNumeroService.class);

    private final EmpresaRepository empresaRepository;
    private final EmpresaEnderecoRepository enderecoRepository;
    private final ConfiguracaoDocumentoRepository documentoRepository;
    private final NfeEmissaoRepository emissaoRepository;
    private final FluxoImportProperties fluxoProps;

    public NfeUltimoNumeroService(
            EmpresaRepository empresaRepository,
            EmpresaEnderecoRepository enderecoRepository,
            ConfiguracaoDocumentoRepository documentoRepository,
            NfeEmissaoRepository emissaoRepository,
            FluxoImportProperties fluxoProps) {
        this.empresaRepository = empresaRepository;
        this.enderecoRepository = enderecoRepository;
        this.documentoRepository = documentoRepository;
        this.emissaoRepository = emissaoRepository;
        this.fluxoProps = fluxoProps;
    }

    public record NumeracaoSerie(String serie, long ultimoEmitido, long proximoNumero) {}

    /** Último número efetivo = maior entre cadastro, portal (nfe_emissao) e legado (nfe_cabecalho). */
    public long ultimoEmitido(Long empresaId, String serie, Integer fluxoLegacyId, long cadastroAtual) {
        String serieNorm = normalizarSerie(serie);
        long maxPortal = emissaoRepository.findMaxNumeroByEmpresaIdAndSerie(empresaId, serieNorm);
        long maxFluxo = fluxoLegacyId != null ? maximoFluxoCabecalho(fluxoLegacyId, serieNorm) : 0L;
        return Math.max(cadastroAtual, Math.max(maxPortal, maxFluxo));
    }

    @Transactional
    public void sincronizarEmpresa(Long empresaId) {
        var empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            return;
        }
        var enderecos = enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId);
        for (EmpresaEndereco end : enderecos) {
            sincronizarEndereco(empresa, end);
        }
        documentoRepository.findByEmpresaIdAndTipo(empresaId, ConfiguracaoDocumento.TIPO_NFE).ifPresent(doc -> {
            long ultimo = ultimoEmitido(empresaId, doc.getSerie(), empresa.getFluxoLegacyId(), doc.getUltimoNumero());
            if (ultimo > doc.getUltimoNumero()) {
                doc.setUltimoNumero(ultimo);
                documentoRepository.save(doc);
            }
        });
    }

    @Transactional
    public EmpresaEndereco sincronizarEndereco(Empresa empresa, EmpresaEndereco end) {
        long ultimo = ultimoEmitido(
                empresa.getId(), end.getSerieNfe(), empresa.getFluxoLegacyId(), end.getUltimoNumeroNfe());
        if (ultimo > end.getUltimoNumeroNfe()) {
            end.setUltimoNumeroNfe(ultimo);
            return enderecoRepository.save(end);
        }
        return end;
    }

    @Transactional
    public EmpresaEndereco sincronizarEnderecoPorId(Long empresaId, Long enderecoId) {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        var end = enderecoRepository.findById(enderecoId)
                .filter(e -> e.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Endereco nao encontrado"));
        return sincronizarEndereco(empresa, end);
    }

    public NumeracaoSerie numeracaoSerie(long ultimo) {
        return new NumeracaoSerie("", ultimo, ultimo + 1);
    }

    private long maximoFluxoCabecalho(int fluxoEmpresaId, String serie) {
        if (!fluxoProps.enabled()) {
            return 0L;
        }
        String sql = """
                SELECT COALESCE(MAX(CAST(NUMERO AS UNSIGNED)), 0) AS ULTIMO
                FROM nfe_cabecalho
                WHERE ID_EMPRESA = ? AND SERIE = ?
                  AND NUMERO IS NOT NULL AND NUMERO REGEXP '^[0-9]+$'
                """;
        try (Connection fluxo = DriverManager.getConnection(
                fluxoProps.url(), fluxoProps.username(), fluxoProps.password())) {
            try (var st = fluxo.prepareStatement(sql)) {
                st.setInt(1, fluxoEmpresaId);
                st.setString(2, serie);
                try (var rs = st.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("ULTIMO");
                    }
                }
            }
        } catch (SQLException ex) {
            log.warn("Falha ao consultar nfe_cabecalho (empresa fluxo {} serie {}): {}", fluxoEmpresaId, serie, ex.getMessage());
        }
        return 0L;
    }

    static String normalizarSerie(String serie) {
        if (serie == null || serie.isBlank()) {
            return "1";
        }
        String limpa = serie.trim();
        return limpa.length() <= 10 ? limpa : limpa.substring(0, 10);
    }
}
