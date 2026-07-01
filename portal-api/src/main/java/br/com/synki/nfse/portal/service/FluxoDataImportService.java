package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.FluxoImportProperties;
import br.com.synki.nfse.portal.domain.ConfiguracaoNfse;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.fiscal.Cfop;
import br.com.synki.nfse.portal.domain.fiscal.Ncm;
import br.com.synki.nfse.portal.domain.fiscal.Pessoa;
import br.com.synki.nfse.portal.domain.fiscal.Produto;
import br.com.synki.nfse.portal.domain.fiscal.TributConfiguraOfGt;
import br.com.synki.nfse.portal.domain.fiscal.TributGrupoTributario;
import br.com.synki.nfse.portal.domain.fiscal.TributIcmsUf;
import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.domain.fiscal.Veiculo;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.repository.fiscal.CfopRepository;
import br.com.synki.nfse.portal.repository.fiscal.NcmRepository;
import br.com.synki.nfse.portal.repository.fiscal.PessoaRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributConfiguraOfGtRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributGrupoTributarioRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributOperacaoFiscalRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributNfseServicoRepository;
import br.com.synki.nfse.portal.repository.fiscal.VeiculoRepository;
import br.com.synki.nfse.portal.service.fiscal.NfseServicoSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class FluxoDataImportService {

    private static final Logger log = LoggerFactory.getLogger(FluxoDataImportService.class);

    private final FluxoImportProperties props;
    private final EmpresaRepository empresaRepository;
    private final EmpresaEnderecoRepository enderecoRepository;
    private final ConfiguracaoNfseRepository configuracaoNfseRepository;
    private final UsuarioRepository usuarioRepository;
    private final TributGrupoTributarioRepository grupoRepository;
    private final TributOperacaoFiscalRepository operacaoRepository;
    private final TributConfiguraOfGtRepository configuraRepository;
    private final PessoaRepository pessoaRepository;
    private final CfopRepository cfopRepository;
    private final NcmRepository ncmRepository;
    private final ProdutoRepository produtoRepository;
    private final VeiculoRepository veiculoRepository;
    private final TributNfseServicoRepository nfseServicoRepository;
    private final NfseServicoSeedService nfseServicoSeedService;
    private final PasswordEncoder passwordEncoder;

    public FluxoDataImportService(
            FluxoImportProperties props,
            EmpresaRepository empresaRepository,
            EmpresaEnderecoRepository enderecoRepository,
            ConfiguracaoNfseRepository configuracaoNfseRepository,
            UsuarioRepository usuarioRepository,
            TributGrupoTributarioRepository grupoRepository,
            TributOperacaoFiscalRepository operacaoRepository,
            TributConfiguraOfGtRepository configuraRepository,
            PessoaRepository pessoaRepository,
            CfopRepository cfopRepository,
            NcmRepository ncmRepository,
            ProdutoRepository produtoRepository,
            VeiculoRepository veiculoRepository,
            TributNfseServicoRepository nfseServicoRepository,
            NfseServicoSeedService nfseServicoSeedService,
            PasswordEncoder passwordEncoder) {
        this.props = props;
        this.empresaRepository = empresaRepository;
        this.enderecoRepository = enderecoRepository;
        this.configuracaoNfseRepository = configuracaoNfseRepository;
        this.usuarioRepository = usuarioRepository;
        this.grupoRepository = grupoRepository;
        this.operacaoRepository = operacaoRepository;
        this.configuraRepository = configuraRepository;
        this.pessoaRepository = pessoaRepository;
        this.cfopRepository = cfopRepository;
        this.ncmRepository = ncmRepository;
        this.produtoRepository = produtoRepository;
        this.veiculoRepository = veiculoRepository;
        this.nfseServicoRepository = nfseServicoRepository;
        this.nfseServicoSeedService = nfseServicoSeedService;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isEnabled() {
        return props.enabled();
    }

    @Transactional
    public Map<String, Object> importar() {
        return importar(false);
    }

    @Transactional
    public Map<String, Object> importar(boolean force) {
        if (!props.enabled()) {
            throw new IllegalStateException("Importacao fluxo desabilitada (nfse.fluxo.enabled=false)");
        }
        log.info("Iniciando importacao do banco fluxo em {} (force={})", props.url(), force);

        var stats = new LinkedHashMap<String, Object>();
        var empresaMap = new HashMap<Integer, Long>();
        Map<String, Long> ultimoNfePorSerie = new HashMap<>();

        try (Connection fluxo = openFluxo()) {
            ultimoNfePorSerie = carregarUltimosNumerosNfe(fluxo);
            int empresas = importarEmpresas(fluxo, empresaMap);
            int enderecos = importarEnderecos(fluxo, empresaMap, ultimoNfePorSerie);
            stats.put("empresas", empresas);
            stats.put("enderecos", enderecos);

            if (force || cfopRepository.countByEmpresaIdIsNull() < 50) {
                if (force) {
                    cfopRepository.deleteByEmpresaIdIsNull();
                }
                stats.put("cfop", importarCfop(fluxo));
            } else {
                stats.put("cfop", "ja existente — ignorado");
            }

            if (force || ncmRepository.count() < 500) {
                if (force) {
                    ncmRepository.deleteAll();
                }
                stats.put("ncm", importarNcm(fluxo));
            } else {
                stats.put("ncm", "ja existente — ignorado");
            }

            int totalClientes = 0;
            int totalProdutos = 0;
            int totalVeiculos = 0;
            int totalNfseServicos = 0;
            int totalGrupos = 0;
            Set<Long> empresasCadastro = new HashSet<>(empresaMap.values());
            if (props.replicarCadastrosEmpresaDemoId() > 0) {
                empresaRepository.findById(props.replicarCadastrosEmpresaDemoId())
                        .ifPresent(demo -> empresasCadastro.add(demo.getId()));
            }
            for (Long empresaPortalId : empresasCadastro) {
                if (force) {
                    limparCadastrosEmpresa(empresaPortalId);
                }
                Map<Integer, Long> grupoMap;
                if (force || grupoRepository.countByEmpresaId(empresaPortalId) == 0) {
                    var tribut = importarTributacao(fluxo, empresaPortalId);
                    grupoMap = tribut.grupoMap;
                    totalGrupos += (Integer) tribut.stats.getOrDefault("gruposTributarios", 0);
                } else {
                    grupoMap = carregarGrupoMapFluxo(fluxo, empresaPortalId);
                }
                if (force || pessoaRepository.countByEmpresaId(empresaPortalId) == 0) {
                    totalClientes += importarClientes(fluxo, empresaPortalId);
                }
                if (force || produtoRepository.countByEmpresaId(empresaPortalId) == 0) {
                    totalProdutos += importarProdutos(fluxo, empresaPortalId, grupoMap);
                }
                if (force || veiculoRepository.countByEmpresaId(empresaPortalId) == 0) {
                    totalVeiculos += importarVeiculos(fluxo, empresaPortalId);
                }
                totalNfseServicos += nfseServicoSeedService.garantirCadastros(empresaPortalId);
            }
            stats.put("clientes", totalClientes);
            stats.put("produtos", totalProdutos);
            stats.put("veiculos", totalVeiculos);
            stats.put("nfseServicos", totalNfseServicos);
            stats.put("gruposTributarios", totalGrupos);
            stats.put("empresasComCadastros", empresasCadastro.size());
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao conectar no banco fluxo: " + ex.getMessage(), ex);
        }

        stats.put("ok", true);
        log.info("Importacao fluxo concluida: {}", stats);
        return stats;
    }

    private void limparCadastrosEmpresa(Long empresaId) {
        nfseServicoRepository.deleteByEmpresaId(empresaId);
        produtoRepository.deleteByEmpresaId(empresaId);
        veiculoRepository.deleteByEmpresaId(empresaId);
        pessoaRepository.deleteByEmpresaId(empresaId);
        configuraRepository.deleteByEmpresaId(empresaId);
        grupoRepository.deleteByEmpresaId(empresaId);
        operacaoRepository.deleteByEmpresaId(empresaId);
    }

    private Connection openFluxo() throws SQLException {
        return DriverManager.getConnection(props.url(), props.username(), props.password());
    }

    private int importarEmpresas(Connection fluxo, Map<Integer, Long> empresaMap) throws SQLException {
        int count = 0;
        String sql = """
                SELECT ID, RAZAO_SOCIAL, NOME_FANTASIA,
                       COALESCE(NULLIF(TRIM(CNPJ), ''), NULLIF(TRIM(cpf_cnpj), '')) AS DOC,
                       INSCRICAO_ESTADUAL, INSCRICAO_MUNICIPAL, EMAIL, CONTATO,
                       CODIGO_CNAE_PRINCIPAL, CRT, ambiente, uf, CODIGO_IBGE_CIDADE
                FROM empresa
                WHERE ID <> 10000
                  AND COALESCE(NULLIF(TRIM(CNPJ), ''), NULLIF(TRIM(cpf_cnpj), '')) IS NOT NULL
                ORDER BY ID
                """;
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                int fluxoId = rs.getInt("ID");
                String doc = apenasDigitos(rs.getString("DOC"));
                if (doc.length() != 11 && doc.length() != 14) {
                    continue;
                }
                String razaoSocial = truncar(rs.getString("RAZAO_SOCIAL"), 255);
                String emailFluxo = rs.getString("EMAIL");
                Empresa empresa = empresaRepository.findByFluxoLegacyId(fluxoId)
                        .or(() -> empresaRepository.findByCnpj(doc))
                        .orElseGet(() -> Empresa.criar(razaoSocial, doc));
                empresa.setFluxoLegacyId(fluxoId);
                empresa.setNome(razaoSocial);
                empresa.setNomeFantasia(truncar(rs.getString("NOME_FANTASIA"), 255));
                empresa.setInscricaoEstadual(truncar(rs.getString("INSCRICAO_ESTADUAL"), 20));
                empresa.setInscricaoMunicipal(truncar(rs.getString("INSCRICAO_MUNICIPAL"), 20));
                empresa.setEmail(truncar(rs.getString("EMAIL"), 255));
                empresa.setTelefone(truncar(rs.getString("CONTATO"), 30));
                empresa.setCnaePrincipal(truncar(rs.getString("CODIGO_CNAE_PRINCIPAL"), 7));
                empresa.setOptanteSimples("1".equals(rs.getString("CRT")));
                empresa.setUf(truncar(rs.getString("uf"), 2));
                empresa.setAtivo(true);
                empresaRepository.save(empresa);
                empresaMap.put(fluxoId, empresa.getId());
                garantirConfiguracaoNfse(empresa, rs);
                garantirUsuario(empresa, emailFluxo, doc);
                count++;
            }
        }
        return count;
    }

    private void garantirConfiguracaoNfse(Empresa empresa, ResultSet rs) throws SQLException {
        String codigoIbgeEmpresa = codigoIbge(rs.getObject("CODIGO_IBGE_CIDADE"));
        String ambiente = mapAmbiente(rs.getString("ambiente"));
        var cfg = configuracaoNfseRepository.findByEmpresaId(empresa.getId())
                .orElseGet(() -> ConfiguracaoNfse.criar(
                        empresa.getId(),
                        municipioLabel(empresa),
                        codigoIbgeEmpresa.isBlank() ? "4310009" : codigoIbgeEmpresa,
                        ambiente));
        if (empresa.getUf() != null) {
            cfg.setPrefeitura(municipioLabel(empresa));
        }
        String ibge = codigoIbgeEmpresa;
        if (!ibge.isBlank()) {
            cfg.setCodigoMunicipioIbge(ibge);
        }
        cfg.setAmbiente(ambiente);
        configuracaoNfseRepository.save(cfg);
    }

    private void garantirUsuario(Empresa empresa, String emailFluxo, String doc) {
        var existente = usuarioRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(empresa.getId());
        if (existente.isPresent()) {
            return;
        }
        String email = emailUnico(emailFluxo, doc);
        usuarioRepository.save(Usuario.create(
                empresa.getId(),
                truncar(empresa.getNome(), 120),
                email,
                passwordEncoder.encode(props.defaultSenhaUsuario())));
    }

    private String emailUnico(String emailFluxo, String doc) {
        if (emailFluxo != null && emailFluxo.contains("@") && emailFluxo.length() <= 255) {
            String normalizado = emailFluxo.trim().toLowerCase();
            if (usuarioRepository.findByEmailAndAtivoTrue(normalizado).isEmpty()) {
                return normalizado;
            }
        }
        String base = "nfe." + doc + "@fluxo.local";
        if (usuarioRepository.findByEmailAndAtivoTrue(base).isEmpty()) {
            return base;
        }
        return "nfe." + doc + "." + System.nanoTime() + "@fluxo.local";
    }

    private int importarEnderecos(
            Connection fluxo,
            Map<Integer, Long> empresaMap,
            Map<String, Long> ultimoNfePorSerie) throws SQLException {
        int count = 0;
        String sql = """
                SELECT ID, ID_EMPRESA, LOGRADOURO, NUMERO, COMPLEMENTO, BAIRRO, CIDADE, CEP,
                       MUNICIPIO_IBGE, UF, INSCRICAO_ESTADUAL, SERIE, PRINCIPAL
                FROM empresa_endereco
                ORDER BY ID_EMPRESA, ID
                """;
        Map<Long, Boolean> principalDefinido = new HashMap<>();
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                int fluxoEmpresaId = rs.getInt("ID_EMPRESA");
                Long portalEmpresaId = empresaMap.get(fluxoEmpresaId);
                if (portalEmpresaId == null) {
                    continue;
                }
                String serie = normalizarSerie(rs.getString("SERIE"));
                var existentes = enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(portalEmpresaId);
                EmpresaEndereco end = existentes.stream()
                        .filter(e -> serie.equals(e.getSerieNfe()))
                        .findFirst()
                        .orElseGet(EmpresaEndereco::new);
                if (end.getId() == null) {
                    end.setEmpresaId(portalEmpresaId);
                }
                String apelido = truncar(
                        Optional.ofNullable(rs.getString("LOGRADOURO")).filter(s -> !s.isBlank()).orElse("Serie " + serie),
                        80);
                end.setApelido(apelido);
                end.setLogradouro(truncar(rs.getString("LOGRADOURO"), 255));
                end.setNumero(truncar(rs.getString("NUMERO"), 20));
                end.setComplemento(truncar(rs.getString("COMPLEMENTO"), 100));
                end.setBairro(truncar(rs.getString("BAIRRO"), 120));
                end.setMunicipio(truncar(rs.getString("CIDADE"), 120));
                end.setUf(truncar(rs.getString("UF"), 2));
                end.setCep(cepLimpo(rs.getString("CEP")));
                end.setCodigoMunicipioIbge(codigoIbge(rs.getObject("MUNICIPIO_IBGE")));
                end.setInscricaoEstadual(truncar(rs.getString("INSCRICAO_ESTADUAL"), 20));
                end.setSerieNfe(serie);
                String chaveUltimo = fluxoEmpresaId + ":" + serie;
                end.setUltimoNumeroNfe(ultimoNfePorSerie.getOrDefault(chaveUltimo, 0L));
                boolean principal = "S".equalsIgnoreCase(rs.getString("PRINCIPAL"))
                        || !principalDefinido.getOrDefault(portalEmpresaId, false);
                end.setPrincipal(principal);
                end.setAtivo(true);
                enderecoRepository.save(end);
                if (principal) {
                    principalDefinido.put(portalEmpresaId, true);
                }
                count++;
            }
        }
        for (Long empresaId : new HashSet<>(empresaMap.values())) {
            sincronizarEmpresaFromPrincipal(empresaId);
        }
        return count;
    }

    private void sincronizarEmpresaFromPrincipal(Long empresaId) {
        enderecoRepository.findByEmpresaIdAndPrincipalTrue(empresaId).ifPresent(principal -> {
            empresaRepository.findById(empresaId).ifPresent(empresa -> {
                empresa.setCep(principal.getCep());
                empresa.setLogradouro(principal.getLogradouro());
                empresa.setNumero(principal.getNumero());
                empresa.setComplemento(principal.getComplemento());
                empresa.setBairro(principal.getBairro());
                empresa.setMunicipio(principal.getMunicipio());
                empresa.setUf(principal.getUf());
                empresa.setInscricaoEstadual(principal.getInscricaoEstadual());
                empresaRepository.save(empresa);
            });
        });
    }

    private static final class TributacaoImportada {
        final Map<String, Object> stats;
        final Map<Integer, Long> grupoMap;

        TributacaoImportada(Map<String, Object> stats, Map<Integer, Long> grupoMap) {
            this.stats = stats;
            this.grupoMap = grupoMap;
        }
    }

    private TributacaoImportada importarTributacao(Connection fluxo, Long empresaPortalId) throws SQLException {
        var stats = new LinkedHashMap<String, Object>();
        var grupoMap = new HashMap<Integer, Long>();
        var operacaoMap = new HashMap<Integer, Long>();

        String sqlGrupo = """
                SELECT ID, DESCRICAO, ORIGEM_MERCADORIA, OBSERVACAO
                FROM tribut_grupo_tributario ORDER BY ID
                """;
        int grupos = 0;
        try (var st = fluxo.prepareStatement(sqlGrupo); var rs = st.executeQuery()) {
            while (rs.next()) {
                int fluxoId = rs.getInt("ID");
                var grupo = new TributGrupoTributario();
                grupo.setEmpresaId(empresaPortalId);
                grupo.setDescricao(truncar(rs.getString("DESCRICAO"), 255));
                grupo.setOrigemMercadoria(truncar(rs.getString("ORIGEM_MERCADORIA"), 1));
                grupo.setObservacao(rs.getString("OBSERVACAO"));
                grupoRepository.save(grupo);
                grupoMap.put(fluxoId, grupo.getId());
                grupos++;
            }
        }

        String sqlOp = """
                SELECT ID, DESCRICAO, DESCRICAO_NA_NF, CFOP, OBSERVACAO, GERA_FINANCEIRO,
                       TIPO_OPERACAO, FINALIDADE_OPERACAO, CMUN_FG_IBS, P_REDUTOR,
                       TP_ENTE_GOV, TP_NF_CREDITO, TP_NF_DEBITO, TP_OPER_GOV
                FROM tribut_operacao_fiscal ORDER BY ID
                """;
        int operacoes = 0;
        try (var st = fluxo.prepareStatement(sqlOp); var rs = st.executeQuery()) {
            while (rs.next()) {
                int fluxoId = rs.getInt("ID");
                var op = new TributOperacaoFiscal();
                op.setEmpresaId(empresaPortalId);
                op.setDescricao(truncar(rs.getString("DESCRICAO"), 255));
                op.setDescricaoNaNf(truncar(rs.getString("DESCRICAO_NA_NF"), 255));
                op.setCfop(rs.getObject("CFOP") != null ? rs.getInt("CFOP") : null);
                op.setObservacao(rs.getString("OBSERVACAO"));
                op.setGeraFinanceiro(primeiroChar(rs.getString("GERA_FINANCEIRO"), "S"));
                op.setTipoOperacao(primeiroChar(rs.getString("TIPO_OPERACAO"), null));
                op.setFinalidadeOperacao(truncar(rs.getString("FINALIDADE_OPERACAO"), 2));
                op.setCMunFGIBS(truncar(rs.getString("CMUN_FG_IBS"), 7));
                op.setPRedutor(rs.getBigDecimal("P_REDUTOR"));
                op.setTpEnteGov(primeiroChar(rs.getString("TP_ENTE_GOV"), null));
                op.setTpNFCredito(truncar(rs.getString("TP_NF_CREDITO"), 2));
                op.setTpNFDebito(truncar(rs.getString("TP_NF_DEBITO"), 2));
                op.setTpOperGov(primeiroChar(rs.getString("TP_OPER_GOV"), null));
                op.setHabilitarIbsCbs(true);
                operacaoRepository.save(op);
                operacaoMap.put(fluxoId, op.getId());
                operacoes++;
            }
        }

        String sqlCfg = """
                SELECT ID, ID_TRIBUT_GRUPO_TRIBUTARIO, ID_TRIBUT_OPERACAO_FISCAL
                FROM tribut_configura_of_gt ORDER BY ID
                """;
        int configs = 0;
        int icms = 0;
        try (var st = fluxo.prepareStatement(sqlCfg); var rs = st.executeQuery()) {
            while (rs.next()) {
                Long grupoId = grupoMap.get(rs.getInt("ID_TRIBUT_GRUPO_TRIBUTARIO"));
                Long opId = operacaoMap.get(rs.getInt("ID_TRIBUT_OPERACAO_FISCAL"));
                if (grupoId == null || opId == null) {
                    continue;
                }
                var cfg = new TributConfiguraOfGt();
                cfg.setEmpresaId(empresaPortalId);
                cfg.setTributGrupoTributarioId(grupoId);
                cfg.setTributOperacaoFiscalId(opId);
                int fluxoCfgId = rs.getInt("ID");
                icms += importarIcmsUf(fluxo, fluxoCfgId, cfg);
                configuraRepository.save(cfg);
                configs++;
            }
        }

        stats.put("gruposTributarios", grupos);
        stats.put("operacoesFiscais", operacoes);
        stats.put("configuracoesTributarias", configs);
        stats.put("regrasIcmsUf", icms);
        return new TributacaoImportada(stats, grupoMap);
    }

    private int importarIcmsUf(Connection fluxo, int fluxoCfgId, TributConfiguraOfGt cfg) throws SQLException {
        int count = 0;
        String sql = """
                SELECT UF_DESTINO, CFOP, CSOSN, CST, ALIQUOTA, ORIGEM_MERCADORIA
                FROM tribut_icms_uf
                WHERE ID_TRIBUT_CONFIGURA_OF_GT = ?
                """;
        try (PreparedStatement st = fluxo.prepareStatement(sql)) {
            st.setInt(1, fluxoCfgId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    var icms = new TributIcmsUf();
                    icms.setConfiguraOfGt(cfg);
                    icms.setUfDestino(truncar(rs.getString("UF_DESTINO"), 2));
                    icms.setCfop(rs.getObject("CFOP") != null ? rs.getInt("CFOP") : null);
                    icms.setCsosn(truncar(rs.getString("CSOSN"), 3));
                    icms.setCst(truncar(rs.getString("CST"), 3));
                    icms.setAliquota(normalizarAliquota(rs.getBigDecimal("ALIQUOTA")));
                    icms.setOrigemMercadoria(primeiroChar(rs.getString("ORIGEM_MERCADORIA"), null));
                    cfg.getListaIcmsUf().add(icms);
                    count++;
                }
            }
        }
        return count;
    }

    private int importarClientes(Connection fluxo, Long empresaPortalId) throws SQLException {
        int count = 0;
        Set<String> docsImportados = new HashSet<>();
        String sql = """
                SELECT NOME, NOME_FANTASIA, CPF_CNPJ, EMAIL, INSCRICAO_ESTADUAL,
                       LOGRADOURO, NUMERO, BAIRRO, cidade, UF, CEP, COD_MUNICIPIO, TIPO_PESSOA
                FROM cliente
                ORDER BY ID
                """;
        List<Pessoa> lote = new ArrayList<>();
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                String doc = apenasDigitos(rs.getString("CPF_CNPJ"));
                if (doc.length() != 11 && doc.length() != 14) {
                    continue;
                }
                if (!docsImportados.add(doc)) {
                    continue;
                }
                var pessoa = new Pessoa();
                pessoa.setEmpresaId(empresaPortalId);
                pessoa.setNome(truncar(
                        Optional.ofNullable(rs.getString("NOME"))
                                .filter(s -> !s.isBlank())
                                .orElse(rs.getString("NOME_FANTASIA")),
                        255));
                if (pessoa.getNome() == null || pessoa.getNome().isBlank()) {
                    pessoa.setNome("Cliente " + doc);
                }
                pessoa.setTipo(doc.length() == 11 ? "F" : "J");
                pessoa.setCpfCnpj(doc);
                pessoa.setEmail(truncar(rs.getString("EMAIL"), 255));
                pessoa.setInscricaoEstadual(truncar(rs.getString("INSCRICAO_ESTADUAL"), 20));
                pessoa.setLogradouro(truncar(rs.getString("LOGRADOURO"), 255));
                pessoa.setNumero(truncar(rs.getString("NUMERO"), 20));
                pessoa.setBairro(truncar(rs.getString("BAIRRO"), 120));
                pessoa.setMunicipio(truncar(rs.getString("cidade"), 120));
                pessoa.setUf(truncar(rs.getString("UF"), 2));
                pessoa.setCep(cepLimpo(rs.getString("CEP")));
                pessoa.setCodigoMunicipioIbge(codigoIbge(rs.getObject("COD_MUNICIPIO")));
                pessoa.setAtivo(true);
                lote.add(pessoa);
                if (lote.size() >= 200) {
                    pessoaRepository.saveAll(lote);
                    count += lote.size();
                    lote.clear();
                }
            }
        }
        if (!lote.isEmpty()) {
            pessoaRepository.saveAll(lote);
            count += lote.size();
        }
        return count;
    }

    private int importarCfop(Connection fluxo) throws SQLException {
        int count = 0;
        Set<String> importados = new HashSet<>();
        String sql = "SELECT CFOP, APLICACAO, DESCRICAO FROM cfop WHERE CFOP IS NOT NULL ORDER BY ID";
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            List<Cfop> lote = new ArrayList<>();
            while (rs.next()) {
                String codigo = formatarCfop(rs.getObject("CFOP"));
                if (codigo == null || !importados.add(codigo)) {
                    continue;
                }
                if (!forceCfopAusente(codigo)) {
                    continue;
                }
                var cfop = new Cfop();
                cfop.setCfop(codigo);
                cfop.setAplicacao(truncar(rs.getString("APLICACAO"), 500));
                cfop.setDescricao(truncar(
                        Optional.ofNullable(rs.getString("DESCRICAO")).filter(s -> !s.isBlank()).orElse(codigo),
                        500));
                lote.add(cfop);
                if (lote.size() >= 200) {
                    cfopRepository.saveAll(lote);
                    count += lote.size();
                    lote.clear();
                }
            }
            if (!lote.isEmpty()) {
                cfopRepository.saveAll(lote);
                count += lote.size();
            }
        }
        return count;
    }

    private boolean forceCfopAusente(String codigo) {
        return cfopRepository.findByCfopAndEmpresaIdIsNull(codigo).isEmpty();
    }

    private int importarNcm(Connection fluxo) throws SQLException {
        int count = 0;
        String sql = "SELECT CODIGO, DESCRICAO, OBSERVACAO FROM ncm WHERE CODIGO IS NOT NULL ORDER BY ID";
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            List<Ncm> lote = new ArrayList<>();
            while (rs.next()) {
                String codigo = normalizarNcm(rs.getString("CODIGO"));
                if (codigo == null) {
                    continue;
                }
                Ncm ncm = ncmRepository.findByCodigo(codigo).orElseGet(Ncm::new);
                if (ncm.getId() != null) {
                    continue;
                }
                ncm.setCodigo(codigo);
                ncm.setDescricao(truncar(
                        Optional.ofNullable(rs.getString("DESCRICAO")).filter(s -> !s.isBlank()).orElse(codigo),
                        500));
                ncm.setObservacao(truncar(rs.getString("OBSERVACAO"), 1000));
                lote.add(ncm);
                if (lote.size() >= 500) {
                    ncmRepository.saveAll(lote);
                    count += lote.size();
                    lote.clear();
                }
            }
            if (!lote.isEmpty()) {
                ncmRepository.saveAll(lote);
                count += lote.size();
            }
        }
        return count;
    }

    private int importarProdutos(Connection fluxo, Long empresaPortalId, Map<Integer, Long> grupoMap) throws SQLException {
        int count = 0;
        String sql = """
                SELECT ID, CODIGO_INTERNO, NOME, NCM, GTIN, VALOR_VENDA, ID_GRUPO_TRIBUTARIO, INATIVO
                FROM produto ORDER BY ID
                """;
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                int fluxoId = rs.getInt("ID");
                String codigo = codigoProdutoFluxo(fluxoId, rs.getString("CODIGO_INTERNO"));
                if (produtoRepository.findByEmpresaIdAndCodigo(empresaPortalId, codigo).isPresent()) {
                    continue;
                }
                var produto = new Produto();
                produto.setEmpresaId(empresaPortalId);
                produto.setCodigo(codigo);
                produto.setNome(truncar(
                        Optional.ofNullable(rs.getString("NOME")).filter(s -> !s.isBlank()).orElse(codigo),
                        255));
                String gtin = rs.getString("GTIN");
                if (gtin != null && !gtin.isBlank() && !gtin.toUpperCase().contains("SEM")) {
                    produto.setGtin(truncar(apenasDigitos(gtin), 14));
                }
                produto.setCodigoNcm(normalizarNcm(rs.getString("NCM")));
                produto.setValorUnitario(rs.getBigDecimal("VALOR_VENDA"));
                int grupoFluxo = rs.getInt("ID_GRUPO_TRIBUTARIO");
                if (!rs.wasNull() && grupoMap.containsKey(grupoFluxo)) {
                    produto.setGrupoTributarioId(grupoMap.get(grupoFluxo));
                }
                String inativo = rs.getString("INATIVO");
                produto.setAtivo(inativo == null || inativo.isBlank() || "N".equalsIgnoreCase(inativo.trim()));
                produtoRepository.save(produto);
                count++;
            }
        }
        return count;
    }

    private int importarVeiculos(Connection fluxo, Long empresaPortalId) throws SQLException {
        int count = 0;
        String sql = """
                SELECT ID, PLACA, MARCA, MODELO, RENAVAM, TIPO_RODADO, TIPO_CARROCERIA
                FROM veiculo
                WHERE PLACA IS NOT NULL AND TRIM(PLACA) <> ''
                ORDER BY ID
                """;
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                String placa = normalizarPlaca(rs.getString("PLACA"));
                if (placa == null) {
                    continue;
                }
                if (veiculoRepository.findByEmpresaIdAndPlaca(empresaPortalId, placa).isPresent()) {
                    continue;
                }
                var veiculo = new Veiculo();
                veiculo.setEmpresaId(empresaPortalId);
                veiculo.setPlaca(placa);
                veiculo.setMarca(truncar(rs.getString("MARCA"), 100));
                veiculo.setModelo(truncar(rs.getString("MODELO"), 100));
                String renavam = apenasDigitos(rs.getString("RENAVAM"));
                if (!renavam.isBlank()) {
                    veiculo.setRenavam(truncar(renavam, 20));
                }
                veiculo.setTipoRodado(codigoVeiculo2(rs.getString("TIPO_RODADO")));
                veiculo.setTipoCarroceria(codigoVeiculo2(rs.getString("TIPO_CARROCERIA")));
                veiculo.setAtivo(true);
                veiculoRepository.save(veiculo);
                count++;
            }
        }
        return count;
    }

    private Map<Integer, Long> carregarGrupoMapFluxo(Connection fluxo, Long empresaPortalId) throws SQLException {
        var map = new HashMap<Integer, Long>();
        var gruposPortal = grupoRepository.findByEmpresaIdOrderByDescricaoAsc(empresaPortalId);
        String sql = "SELECT ID, DESCRICAO FROM tribut_grupo_tributario ORDER BY ID";
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                int fluxoId = rs.getInt("ID");
                String desc = truncar(rs.getString("DESCRICAO"), 255);
                gruposPortal.stream()
                        .filter(g -> g.getDescricao().equals(desc))
                        .findFirst()
                        .ifPresent(g -> map.put(fluxoId, g.getId()));
            }
        }
        return map;
    }

    private static String codigoProdutoFluxo(int fluxoId, String codigoInterno) {
        if (codigoInterno == null || codigoInterno.isBlank()) {
            return "P" + fluxoId;
        }
        String codigo = codigoInterno.trim();
        if ("SEM CODIGO".equalsIgnoreCase(codigo) || "SEM GTIN".equalsIgnoreCase(codigo)) {
            return "P" + fluxoId;
        }
        return truncar(codigo, 60);
    }

    private static String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            return null;
        }
        String limpa = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (limpa.length() < 7) {
            return null;
        }
        return limpa.substring(0, 7);
    }

    private static String codigoVeiculo2(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String digits = apenasDigitos(valor);
        if (!digits.isBlank()) {
            return digits.length() <= 2 ? digits : digits.substring(0, 2);
        }
        return truncar(valor.trim(), 2);
    }

    private static String formatarCfop(Object valor) {
        if (valor == null) {
            return null;
        }
        String digits = apenasDigitos(String.valueOf(valor));
        if (digits.isBlank()) {
            return null;
        }
        return digits.length() <= 4 ? String.format("%04d", Integer.parseInt(digits)) : digits.substring(0, 4);
    }

    private static String normalizarNcm(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String digits = apenasDigitos(valor);
        if (digits.length() < 4) {
            return null;
        }
        return digits.length() <= 8 ? digits : digits.substring(0, 8);
    }

    private Map<String, Long> carregarUltimosNumerosNfe(Connection fluxo) throws SQLException {
        var map = new HashMap<String, Long>();
        String sql = """
                SELECT ID_EMPRESA, SERIE, MAX(NUMERO) AS ULTIMO
                FROM nfe_numero
                WHERE ID_EMPRESA IS NOT NULL AND SERIE IS NOT NULL
                GROUP BY ID_EMPRESA, SERIE
                """;
        try (var st = fluxo.prepareStatement(sql); var rs = st.executeQuery()) {
            while (rs.next()) {
                String serie = normalizarSerie(rs.getString("SERIE"));
                map.put(rs.getInt("ID_EMPRESA") + ":" + serie, rs.getLong("ULTIMO"));
            }
        }
        return map;
    }

    private static String mapAmbiente(String ambiente) {
        if (ambiente == null) {
            return "homologacao";
        }
        return ambiente.toUpperCase().contains("PROD") ? "producao" : "homologacao";
    }

    private static String municipioLabel(Empresa empresa) {
        if (empresa.getMunicipio() != null && empresa.getUf() != null) {
            return empresa.getMunicipio() + "/" + empresa.getUf();
        }
        if (empresa.getUf() != null) {
            return empresa.getUf();
        }
        return "Municipio";
    }

    private static String codigoIbge(Object valor) {
        if (valor == null) {
            return "";
        }
        String digits = apenasDigitos(String.valueOf(valor));
        return digits.length() >= 7 ? digits.substring(0, 7) : digits;
    }

    private static String normalizarSerie(String serie) {
        if (serie == null || serie.isBlank()) {
            return "1";
        }
        return truncar(serie.trim(), 10);
    }

    private static BigDecimal normalizarAliquota(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        if (valor.compareTo(BigDecimal.ONE) > 0) {
            return valor.movePointLeft(2);
        }
        return valor;
    }

    private static String primeiroChar(String valor, String padrao) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        return truncar(valor.trim(), 1);
    }

    private static String apenasDigitos(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("\\D", "");
    }

    private static String cepLimpo(String valor) {
        String digits = apenasDigitos(valor);
        if (digits.isBlank()) {
            return null;
        }
        return digits.length() <= 8 ? digits : digits.substring(0, 8);
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
