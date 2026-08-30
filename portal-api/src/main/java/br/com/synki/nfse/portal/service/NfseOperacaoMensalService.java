package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.NfseOperacaoMensal;
import br.com.synki.nfse.portal.repository.NfseOperacaoMensalRepository;
import br.com.synki.nfse.portal.web.dto.EmissaoCompletaRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NfseOperacaoMensalService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final NfseOperacaoMensalRepository repository;
    private final EmissaoDpsService emissaoDpsService;
    private final AssinaturaService assinaturaService;
    private final AuditLogService auditLogService;
    private final ContabilidadeService contabilidadeService;

    public NfseOperacaoMensalService(
            NfseOperacaoMensalRepository repository,
            EmissaoDpsService emissaoDpsService,
            AssinaturaService assinaturaService,
            AuditLogService auditLogService,
            ContabilidadeService contabilidadeService) {
        this.repository = repository;
        this.emissaoDpsService = emissaoDpsService;
        this.assinaturaService = assinaturaService;
        this.auditLogService = auditLogService;
        this.contabilidadeService = contabilidadeService;
    }

    public List<Map<String, Object>> listar(Long empresaId) {
        return repository.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId).stream()
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> obter(Long empresaId, Long id) {
        return toMap(require(empresaId, id));
    }

    @Transactional
    public Map<String, Object> salvar(Long empresaId, NfseOperacaoMensal body) {
        if (body.getId() != null) {
            var atual = require(empresaId, body.getId());
            copiar(body, atual);
            return toMap(repository.save(atual));
        }
        body.setEmpresaId(empresaId);
        return toMap(repository.save(body));
    }

    @Transactional
    public Map<String, Object> emitir(Long empresaId, Long usuarioId, Long id, LocalDate competencia) throws Exception {
        var op = require(empresaId, id);
        if (!op.isAtivo()) {
            throw new IllegalArgumentException("Operação inativa");
        }
        assinaturaService.requireEmissaoNfse(empresaId);
        var req = montarPayload(op, competencia != null ? competencia : LocalDate.now());
        var sucesso = emissaoDpsService.emitir(empresaId, req);
        op.setUltimaEmissaoChave(sucesso.getChaveAcesso());
        op.setUltimaEmissaoEm(Instant.now());
        repository.save(op);
        auditLogService.log(empresaId, usuarioId, "EMISSAO",
                "NFSe " + sucesso.getChaveAcesso() + " (mensal: " + op.getNome() + ")");
        assinaturaService.registrarNfseEmitida(empresaId);
        contabilidadeService.enviarNfseAposEmissao(empresaId, sucesso.getChaveAcesso());
        var out = toMap(op);
        out.put("chaveAcesso", sucesso.getChaveAcesso());
        out.put("sucesso", true);
        return out;
    }

    public EmissaoCompletaRequest montarPayload(NfseOperacaoMensal op, LocalDate competencia) {
        var valor = op.getValorServicos().setScale(2, RoundingMode.HALF_UP);
        var aliq = op.getAliquotaIss() != null ? op.getAliquotaIss() : new BigDecimal("2.0000");
        var iss = valor.multiply(aliq).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        var liquido = valor.subtract(iss);
        var ibgePrest = op.getMunicipioPrestacaoIbge() != null ? op.getMunicipioPrestacaoIbge() : "4310009";
        var ibgeTom = op.getCodigoMunicipioIbge() != null ? op.getCodigoMunicipioIbge() : ibgePrest;
        var descricao = op.getDescricaoServico();
        if (op.getObservacoes() != null && !op.getObservacoes().isBlank()) {
            descricao = descricao + " — " + op.getObservacoes();
        }
        descricao = descricao + " (competência "
                + competencia.format(DateTimeFormatter.ofPattern("MM/yyyy")) + ")";

        return new EmissaoCompletaRequest(
                new EmissaoCompletaRequest.Identificacao(
                        null,
                        op.getSerieRps() != null ? op.getSerieRps() : "1",
                        "1",
                        LocalDateTime.now().format(DT),
                        competencia.toString()),
                new EmissaoCompletaRequest.Regime(
                        op.getTributacaoIssqn(),
                        op.getRegimeEspecial(),
                        op.getSimplesNacional(),
                        op.getIssRetido(),
                        false),
                new EmissaoCompletaRequest.Prestador(""),
                new EmissaoCompletaRequest.Tomador(
                        op.getTomadorCnpj(),
                        op.getTomadorRazao(),
                        op.getTomadorEmail(),
                        op.getTomadorTelefone(),
                        null,
                        null),
                new EmissaoCompletaRequest.EnderecoTomador(
                        op.getCep(),
                        op.getLogradouro(),
                        op.getNumero(),
                        op.getComplemento(),
                        op.getBairro(),
                        op.getCidade(),
                        op.getUf(),
                        ibgeTom,
                        "BR"),
                new EmissaoCompletaRequest.Servico(
                        op.getItemListaServico(),
                        null,
                        op.getCnae(),
                        op.getNbs(),
                        descricao,
                        ibgePrest,
                        ibgePrest,
                        ibgePrest),
                new EmissaoCompletaRequest.Valores(
                        valor,
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        valor,
                        aliq,
                        iss,
                        liquido,
                        "1"),
                null,
                new EmissaoCompletaRequest.TributacaoFederalDetalhada(
                        "08", "0",
                        valor,
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2),
                        false),
                null,
                null,
                null,
                null,
                new EmissaoCompletaRequest.InformacoesAdicionais(
                        "Operação mensal: " + op.getNome(),
                        null));
    }

    private NfseOperacaoMensal require(Long empresaId, Long id) {
        return repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new AccessDeniedException("Operação mensal não encontrada"));
    }

    private void copiar(NfseOperacaoMensal src, NfseOperacaoMensal dst) {
        dst.setNome(src.getNome());
        dst.setTomadorCnpj(src.getTomadorCnpj());
        dst.setTomadorRazao(src.getTomadorRazao());
        dst.setTomadorEmail(src.getTomadorEmail());
        dst.setTomadorTelefone(src.getTomadorTelefone());
        dst.setCep(src.getCep());
        dst.setLogradouro(src.getLogradouro());
        dst.setNumero(src.getNumero());
        dst.setComplemento(src.getComplemento());
        dst.setBairro(src.getBairro());
        dst.setCidade(src.getCidade());
        dst.setUf(src.getUf());
        dst.setCodigoMunicipioIbge(src.getCodigoMunicipioIbge());
        dst.setValorServicos(src.getValorServicos());
        dst.setDescricaoServico(src.getDescricaoServico());
        dst.setItemListaServico(src.getItemListaServico());
        dst.setNbs(src.getNbs());
        dst.setCnae(src.getCnae());
        dst.setMunicipioPrestacaoIbge(src.getMunicipioPrestacaoIbge());
        dst.setAliquotaIss(src.getAliquotaIss());
        dst.setTributacaoIssqn(src.getTributacaoIssqn());
        dst.setIssRetido(src.getIssRetido());
        dst.setSimplesNacional(src.getSimplesNacional());
        dst.setRegimeEspecial(src.getRegimeEspecial());
        dst.setSerieRps(src.getSerieRps());
        dst.setObservacoes(src.getObservacoes());
        dst.setAtivo(src.isAtivo());
    }

    private Map<String, Object> toMap(NfseOperacaoMensal op) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", op.getId());
        m.put("empresaId", op.getEmpresaId());
        m.put("nome", op.getNome());
        m.put("tomadorCnpj", op.getTomadorCnpj());
        m.put("tomadorRazao", op.getTomadorRazao());
        m.put("tomadorEmail", op.getTomadorEmail());
        m.put("tomadorTelefone", op.getTomadorTelefone());
        m.put("cep", op.getCep());
        m.put("logradouro", op.getLogradouro());
        m.put("numero", op.getNumero());
        m.put("complemento", op.getComplemento());
        m.put("bairro", op.getBairro());
        m.put("cidade", op.getCidade());
        m.put("uf", op.getUf());
        m.put("codigoMunicipioIbge", op.getCodigoMunicipioIbge());
        m.put("valorServicos", op.getValorServicos());
        m.put("descricaoServico", op.getDescricaoServico());
        m.put("itemListaServico", op.getItemListaServico());
        m.put("nbs", op.getNbs());
        m.put("cnae", op.getCnae());
        m.put("municipioPrestacaoIbge", op.getMunicipioPrestacaoIbge());
        m.put("aliquotaIss", op.getAliquotaIss());
        m.put("ativo", op.isAtivo());
        m.put("ultimaEmissaoChave", op.getUltimaEmissaoChave());
        m.put("ultimaEmissaoEm", op.getUltimaEmissaoEm() != null ? op.getUltimaEmissaoEm().toString() : null);
        return m;
    }
}
