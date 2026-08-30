package br.com.synki.nfse.portal.fiscal.livrocaixa;

import br.com.synki.nfse.portal.domain.NfeEmissao;
import br.com.synki.nfse.portal.domain.NfseLog;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import br.com.synki.nfse.portal.service.NfseLibService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class XmlNotasColetorService {

    private static final Pattern CHAVE_NFSE = Pattern.compile("(\\d{50})");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final NfseLibService nfseLibService;
    private final NfseLogRepository nfseLogRepository;
    private final NfeEmissaoRepository nfeEmissaoRepository;
    private final NfeEntradaRepository nfeEntradaRepository;

    public XmlNotasColetorService(
            NfseLibService nfseLibService,
            NfseLogRepository nfseLogRepository,
            NfeEmissaoRepository nfeEmissaoRepository,
            NfeEntradaRepository nfeEntradaRepository) {
        this.nfseLibService = nfseLibService;
        this.nfseLogRepository = nfseLogRepository;
        this.nfeEmissaoRepository = nfeEmissaoRepository;
        this.nfeEntradaRepository = nfeEntradaRepository;
    }

    public List<LancamentoLivroCaixa> coletar(
            Long empresaId,
            LocalDate de,
            LocalDate ate,
            boolean incluirNfse,
            boolean incluirNfe) throws Exception {
        if (de == null || ate == null || ate.isBefore(de)) {
            throw new IllegalArgumentException("Periodo invalido");
        }
        Instant inicio = de.atStartOfDay(FUSO).toInstant();
        Instant fim = ate.plusDays(1).atStartOfDay(FUSO).toInstant();

        var lancamentos = new ArrayList<LancamentoLivroCaixa>();
        if (incluirNfse) {
            lancamentos.addAll(coletarNfse(empresaId, inicio, fim));
        }
        if (incluirNfe) {
            lancamentos.addAll(coletarNfe(empresaId, inicio, fim));
            lancamentos.addAll(coletarNfeEntrada(empresaId, de, ate));
        }
        lancamentos.sort(Comparator.comparing(LancamentoLivroCaixa::data).thenComparing(LancamentoLivroCaixa::origem));
        return lancamentos;
    }

    private List<LancamentoLivroCaixa> coletarNfse(Long empresaId, Instant inicio, Instant fim) throws Exception {
        List<NfseLog> logs = nfseLogRepository.findByEmpresaIdAndAcaoAndCreatedAtBetweenOrderByCreatedAtAsc(
                empresaId, "EMISSAO", inicio, fim);
        var out = new ArrayList<LancamentoLivroCaixa>();
        for (NfseLog item : logs) {
            String chave = extrairChaveNfse(item.getDescricao());
            if (chave == null) {
                continue;
            }
            try {
                String xml = nfseLibService.downloadXml(empresaId, chave);
                NotaXmlExtrator.extrairNfse(xml, chave).ifPresent(out::add);
            } catch (Exception ignored) {
                // ignora nota indisponivel
            }
        }
        return out;
    }

    private List<LancamentoLivroCaixa> coletarNfe(Long empresaId, Instant inicio, Instant fim) {
        List<NfeEmissao> notas = nfeEmissaoRepository.findByEmpresaIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                empresaId, inicio, fim);
        var out = new ArrayList<LancamentoLivroCaixa>();
        for (NfeEmissao nota : notas) {
            if (nota.getXmlProc() == null || nota.getXmlProc().isBlank()) {
                continue;
            }
            NotaXmlExtrator.extrairNfe(nota.getXmlProc(), nota.getChave()).ifPresent(out::add);
        }
        return out;
    }

    private List<LancamentoLivroCaixa> coletarNfeEntrada(Long empresaId, LocalDate de, LocalDate ate) {
        var out = new ArrayList<LancamentoLivroCaixa>();
        for (var nota : nfeEntradaRepository.findByEmpresaIdAndDataEmissaoBetweenOrderByDataEmissaoAscIdAsc(
                empresaId, de, ate)) {
            if (nota.getXml() == null || nota.getXml().isBlank()) {
                continue;
            }
            NotaXmlExtrator.extrairNfeDespesa(nota.getXml(), nota.getChave()).ifPresent(out::add);
        }
        return out;
    }

    private static String extrairChaveNfse(String descricao) {
        if (descricao == null) {
            return null;
        }
        var m = CHAVE_NFSE.matcher(descricao);
        return m.find() ? m.group(1) : null;
    }
}
