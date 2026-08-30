package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.PortalProperties;
import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.ConfiguracaoNfse;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.ConfiguracaoDocumentoRepository;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import br.com.synki.nfse.portal.repository.NfseLogRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.web.dto.CriarEmpresaRequest;
import br.com.synki.nfse.portal.web.dto.AtualizarEmpresaRequest;
import br.com.synki.nfse.portal.service.fiscal.NfseServicoSeedService;
import br.com.synki.nfse.portal.service.fiscal.ProdutoClassificacaoService;
import br.com.synki.nfse.portal.service.fiscal.TributacaoNfeSeedService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmpresaCadastroService {

    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoNfseRepository configuracaoRepository;
    private final ConfiguracaoDocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CertificadoRepository certificadoRepository;
    private final NfeEmissaoRepository nfeEmissaoRepository;
    private final NfeEntradaRepository nfeEntradaRepository;
    private final NfseLogRepository nfseLogRepository;
    private final EmpresaEnderecoRepository enderecoRepository;
    private final EmpresaLogoService empresaLogoService;
    private final PasswordEncoder passwordEncoder;
    private final PortalProperties portalProperties;
    private final EmpresaEnderecoService enderecoService;
    private final MembershipService membershipService;
    private final AssinaturaService assinaturaService;
    private final TributacaoNfeSeedService tributacaoNfeSeedService;
    private final NfseServicoSeedService nfseServicoSeedService;
    private final ProdutoClassificacaoService produtoClassificacaoService;

    public EmpresaCadastroService(
            EmpresaRepository empresaRepository,
            ConfiguracaoNfseRepository configuracaoRepository,
            ConfiguracaoDocumentoRepository documentoRepository,
            UsuarioRepository usuarioRepository,
            CertificadoRepository certificadoRepository,
            NfeEmissaoRepository nfeEmissaoRepository,
            NfeEntradaRepository nfeEntradaRepository,
            NfseLogRepository nfseLogRepository,
            EmpresaEnderecoRepository enderecoRepository,
            EmpresaLogoService empresaLogoService,
            PasswordEncoder passwordEncoder,
            PortalProperties portalProperties,
            EmpresaEnderecoService enderecoService,
            MembershipService membershipService,
            AssinaturaService assinaturaService,
            TributacaoNfeSeedService tributacaoNfeSeedService,
            NfseServicoSeedService nfseServicoSeedService,
            ProdutoClassificacaoService produtoClassificacaoService) {
        this.empresaRepository = empresaRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.documentoRepository = documentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.certificadoRepository = certificadoRepository;
        this.nfeEmissaoRepository = nfeEmissaoRepository;
        this.nfeEntradaRepository = nfeEntradaRepository;
        this.nfseLogRepository = nfseLogRepository;
        this.enderecoRepository = enderecoRepository;
        this.empresaLogoService = empresaLogoService;
        this.passwordEncoder = passwordEncoder;
        this.portalProperties = portalProperties;
        this.enderecoService = enderecoService;
        this.membershipService = membershipService;
        this.assinaturaService = assinaturaService;
        this.tributacaoNfeSeedService = tributacaoNfeSeedService;
        this.nfseServicoSeedService = nfseServicoSeedService;
        this.produtoClassificacaoService = produtoClassificacaoService;
    }

    public List<Map<String, Object>> listarParaUsuario(Long usuarioId) {
        return membershipService.listarTodasEmpresasPermitidas(usuarioId).stream()
                .map(this::resumoSeguro)
                .toList();
    }

    public List<Map<String, Object>> listar() {
        return empresaRepository.findAllByOrderByNomeAsc().stream().map(this::resumoSeguro).toList();
    }

    private Map<String, Object> resumoSeguro(Empresa empresa) {
        try {
            return resumo(empresa);
        } catch (Exception ex) {
            var body = new LinkedHashMap<String, Object>();
            body.put("id", empresa.getId());
            body.put("nome", empresa.getNome());
            body.put("cnpj", empresa.getCnpj());
            body.put("ativo", empresa.isAtivo());
            body.put("aviso", "Cadastro incompleto — verifique configuracao NFS-e");
            return body;
        }
    }

    public Map<String, Object> obter(Long id) {
        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        return detalhe(empresa);
    }

    public Map<String, Object> obterParaUsuario(Long id, Long usuarioId) {
        membershipService.requireAccess(usuarioId, id);
        return obter(id);
    }

    public Map<String, Object> obterPorCnpj(String cnpj) {
        var doc = apenasDigitos(cnpj);
        var empresa = empresaRepository.findByCnpj(doc)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        return detalhe(empresa);
    }

    public Map<String, Object> obterPorCnpjParaUsuario(String cnpj, Long usuarioId) {
        var doc = apenasDigitos(cnpj);
        var empresa = empresaRepository.findByCnpj(doc)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        membershipService.requireAccess(usuarioId, empresa.getId());
        return detalhe(empresa);
    }

    @Transactional
    public Map<String, Object> criar(CriarEmpresaRequest req) {
        return criarInterno(req, null);
    }

    @Transactional
    public Map<String, Object> criarParaUsuario(
            CriarEmpresaRequest req,
            Long criadorUsuarioId,
            Long empresaSessaoId) {
        membershipService.requireGestao(criadorUsuarioId, empresaSessaoId);
        var contaId = membershipService.contaIdDaEmpresa(empresaSessaoId);
        if (contaId == null) {
            contaId = membershipService.contaIdDoUsuario(criadorUsuarioId, empresaSessaoId);
        }
        if (contaId != null) {
            assinaturaService.requireNovaEmpresa(contaId);
        }
        return criarInterno(req, criadorUsuarioId);
    }

    @Transactional
    public Map<String, Object> criarPrimeiraEmpresaConta(
            CriarEmpresaRequest req,
            Long ownerUsuarioId,
            Long contaId) {
        return criarInterno(req, ownerUsuarioId);
    }

    private Map<String, Object> criarInterno(CriarEmpresaRequest req, Long criadorUsuarioId) {
        var cnpj = apenasDigitos(req.cnpj());
        if (cnpj.length() != 11 && cnpj.length() != 14) {
            throw new IllegalArgumentException("CPF/CNPJ deve ter 11 ou 14 digitos");
        }
        if (empresaRepository.findByCnpj(cnpj).isPresent()) {
            var existente = empresaRepository.findByCnpj(cnpj).orElseThrow();
            throw new IllegalArgumentException(
                    "CPF/CNPJ ja cadastrado como \"" + existente.getNome() + "\" (id " + existente.getId() + ")");
        }
        if (usuarioRepository.findByEmailAndAtivoTrue(req.emailIntegracao().trim().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("E-mail de integracao ja em uso");
        }

        var empresa = Empresa.criar(req.nome(), cnpj);
        aplicarDadosEmpresa(empresa, req);
        empresaRepository.save(empresa);

        var ambiente = req.ambiente() != null && !req.ambiente().isBlank() ? req.ambiente() : "homologacao";
        var cfg = ConfiguracaoNfse.criar(empresa.getId(), req.prefeitura(), req.codigoMunicipioIbge(), ambiente);
        cfg.setSerieRps(primeiroNaoVazio(req.serieRps(), "1"));
        cfg.setUltimoNumeroNfse(req.ultimoNumeroNfse() != null ? Math.max(0, req.ultimoNumeroNfse()) : 0);
        configuracaoRepository.save(cfg);

        salvarDocumentoFuturo(empresa.getId(), ConfiguracaoDocumento.TIPO_NFE, req.serieNfe(), req.ultimoNumeroNfe());
        salvarDocumentoFuturo(empresa.getId(), ConfiguracaoDocumento.TIPO_NFCE, req.serieNfce(), req.ultimoNumeroNfce());

        var nomeUsuario = req.usuarioNome() != null && !req.usuarioNome().isBlank()
                ? req.usuarioNome().trim()
                : "Integracao " + empresa.getNome();
        var integrationUser = usuarioRepository.save(Usuario.create(
                empresa.getId(),
                nomeUsuario,
                req.emailIntegracao().trim().toLowerCase(),
                passwordEncoder.encode(req.senhaIntegracao())));

        provisionarMembership(empresa, integrationUser, criadorUsuarioId);

        if (req.enderecos() != null && !req.enderecos().isEmpty()) {
            enderecoService.sincronizar(empresa.getId(), req.enderecos());
        } else {
            enderecoService.criarMatrizFromEmpresa(
                    empresa.getId(), empresa, req.codigoMunicipioIbge(), req.serieNfe(), req.ultimoNumeroNfe());
        }
        enderecoService.sincronizarEmpresaFromPrincipal(empresa);
        empresaRepository.save(empresa);
        tributacaoNfeSeedService.garantirCadastros(empresa.getId());
        nfseServicoSeedService.garantirCadastros(empresa.getId());
        produtoClassificacaoService.garantirPadrao(empresa.getId());

        var det = detalhe(empresa);
        det.put("embedUrlCnpjComSenha", montarUrlCnpj(cnpj, req.senhaIntegracao()));
        return det;
    }

    @Transactional
    public Map<String, Object> atualizar(Long id, AtualizarEmpresaRequest req) {
        return atualizarInterno(id, req, null);
    }

    @Transactional
    public Map<String, Object> atualizarParaUsuario(Long id, AtualizarEmpresaRequest req, Long usuarioId) {
        membershipService.requireGestao(usuarioId, id);
        return atualizarInterno(id, req, usuarioId);
    }

    private Map<String, Object> atualizarInterno(Long id, AtualizarEmpresaRequest req, Long usuarioId) {
        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        if (req.nome() != null && !req.nome().isBlank()) {
            empresa.setNome(req.nome().trim());
        }
        if (req.ativo() != null) {
            empresa.setAtivo(req.ativo());
        }
        if (req.nomeFantasia() != null) {
            empresa.setNomeFantasia(blankToNull(req.nomeFantasia()));
        }
        if (req.email() != null) {
            empresa.setEmail(blankToNull(req.email()));
        }
        if (req.telefone() != null) {
            empresa.setTelefone(apenasDigitos(req.telefone()));
        }
        if (req.inscricaoEstadual() != null) {
            empresa.setInscricaoEstadual(blankToNull(req.inscricaoEstadual()));
        }
        if (req.inscricaoMunicipal() != null) {
            empresa.setInscricaoMunicipal(blankToNull(req.inscricaoMunicipal()));
        }
        if (req.cep() != null) {
            empresa.setCep(apenasDigitos(req.cep()));
        }
        if (req.logradouro() != null) {
            empresa.setLogradouro(blankToNull(req.logradouro()));
        }
        if (req.numero() != null) {
            empresa.setNumero(blankToNull(req.numero()));
        }
        if (req.complemento() != null) {
            empresa.setComplemento(blankToNull(req.complemento()));
        }
        if (req.bairro() != null) {
            empresa.setBairro(blankToNull(req.bairro()));
        }
        if (req.municipio() != null) {
            empresa.setMunicipio(blankToNull(req.municipio()));
        }
        if (req.uf() != null) {
            empresa.setUf(blankToNull(req.uf()));
        }
        if (req.cnaePrincipal() != null) {
            empresa.setCnaePrincipal(apenasDigitos(req.cnaePrincipal()));
        }
        if (req.cnaePrincipalDescricao() != null) {
            empresa.setCnaePrincipalDescricao(blankToNull(req.cnaePrincipalDescricao()));
        }
        if (req.optanteSimples() != null) {
            empresa.setOptanteSimples(req.optanteSimples());
        }
        if (req.baixarXml() != null) {
            empresa.setBaixarXml(req.baixarXml());
        }
        empresaRepository.save(empresa);

        var cfg = configuracaoRepository.findByEmpresaId(id)
                .orElseThrow(() -> new IllegalStateException("Configuracao NFS-e nao encontrada"));
        if (req.prefeitura() != null && !req.prefeitura().isBlank()) {
            cfg.setPrefeitura(req.prefeitura().trim());
        }
        if (req.codigoMunicipioIbge() != null && !req.codigoMunicipioIbge().isBlank()) {
            cfg.setCodigoMunicipioIbge(apenasDigitos(req.codigoMunicipioIbge()));
        }
        if (req.ambiente() != null && !req.ambiente().isBlank()) {
            cfg.setAmbiente(req.ambiente().trim());
        }
        if (req.serieRps() != null && !req.serieRps().isBlank()) {
            cfg.setSerieRps(req.serieRps().trim());
        }
        if (req.ultimoNumeroNfse() != null) {
            cfg.setUltimoNumeroNfse(Math.max(0, req.ultimoNumeroNfse()));
        }
        configuracaoRepository.save(cfg);

        if (req.serieNfe() != null || req.ultimoNumeroNfe() != null) {
            atualizarDocumentoFuturo(id, ConfiguracaoDocumento.TIPO_NFE, req.serieNfe(), req.ultimoNumeroNfe());
        }
        if (req.serieNfce() != null || req.ultimoNumeroNfce() != null) {
            atualizarDocumentoFuturo(id, ConfiguracaoDocumento.TIPO_NFCE, req.serieNfce(), req.ultimoNumeroNfce());
        }

        if (req.senhaIntegracao() != null && !req.senhaIntegracao().isBlank()) {
            var usuario = usuarioRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario de integracao nao encontrado"));
            usuario.setSenha(passwordEncoder.encode(req.senhaIntegracao()));
            usuarioRepository.save(usuario);
        }

        if (req.enderecos() != null) {
            enderecoService.sincronizar(id, req.enderecos());
            enderecoService.sincronizarEmpresaFromPrincipal(empresa);
            empresaRepository.save(empresa);
        }

        return detalhe(empresa);
    }

    @Transactional
    public void excluir(Long id) {
        excluirInterno(id);
    }

    @Transactional
    public void excluirParaUsuario(Long id, Long usuarioId) {
        membershipService.requireGestao(usuarioId, id);
        excluirInterno(id);
    }

    private void excluirInterno(Long id) {
        if (!empresaRepository.existsById(id)) {
            throw new IllegalArgumentException("Empresa nao encontrada");
        }
        nfseLogRepository.deleteByEmpresaId(id);
        nfeEmissaoRepository.deleteByEmpresaId(id);
        nfeEntradaRepository.deleteByEmpresaId(id);
        certificadoRepository.deleteByEmpresaId(id);
        usuarioRepository.deleteByEmpresaId(id);
        documentoRepository.deleteByEmpresaId(id);
        configuracaoRepository.deleteByEmpresaId(id);
        enderecoRepository.deleteByEmpresaId(id);
        membershipService.removerVinculosEmpresa(id);
        try {
            empresaLogoService.excluir(id);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao remover logo da empresa", ex);
        }
        empresaRepository.deleteById(id);
    }

    private void provisionarMembership(
            Empresa empresa,
            Usuario integrationUser,
            Long criadorUsuarioId) {
        if (criadorUsuarioId == null) {
            membershipService.provisionarContaParaEmpresa(empresa, integrationUser, UsuarioEmpresa.PAPEL_OWNER);
            return;
        }

        var contaId = membershipService.contaIdDoUsuario(criadorUsuarioId);
        if (contaId == null) {
            var conta = membershipService.provisionarContaParaEmpresa(
                    empresa, integrationUser, UsuarioEmpresa.PAPEL_OWNER);
            membershipService.vincularUsuarioEmpresa(
                    criadorUsuarioId, empresa.getId(), conta.getId(), UsuarioEmpresa.PAPEL_ADMIN);
            return;
        }

        membershipService.vincularEmpresaAContaExistente(
                contaId, empresa.getId(), integrationUser, UsuarioEmpresa.PAPEL_OPERADOR);
        var papelCriador = membershipService.precisaOnboarding(criadorUsuarioId)
                ? UsuarioEmpresa.PAPEL_OWNER
                : UsuarioEmpresa.PAPEL_ADMIN;
        membershipService.vincularUsuarioEmpresa(
                criadorUsuarioId, empresa.getId(), contaId, papelCriador);
    }

    private void atualizarDocumentoFuturo(Long empresaId, String tipo, String serie, Long ultimoNumero) {
        var doc = documentoRepository.findByEmpresaIdAndTipo(empresaId, tipo)
                .orElseGet(() -> ConfiguracaoDocumento.criar(empresaId, tipo, false));
        if (serie != null && !serie.isBlank()) {
            doc.setSerie(serie.trim());
        }
        if (ultimoNumero != null) {
            doc.setUltimoNumero(Math.max(0, ultimoNumero));
        }
        documentoRepository.save(doc);
    }

    private void salvarDocumentoFuturo(Long empresaId, String tipo, String serie, Long ultimoNumero) {
        var doc = ConfiguracaoDocumento.criar(empresaId, tipo, false);
        doc.setSerie(primeiroNaoVazio(serie, "1"));
        doc.setUltimoNumero(ultimoNumero != null ? Math.max(0, ultimoNumero) : 0);
        documentoRepository.save(doc);
    }

    private void aplicarDadosEmpresa(Empresa empresa, CriarEmpresaRequest req) {
        empresa.setNomeFantasia(blankToNull(req.nomeFantasia()));
        empresa.setEmail(blankToNull(req.email()));
        empresa.setTelefone(apenasDigitos(req.telefone()));
        empresa.setInscricaoEstadual(blankToNull(req.inscricaoEstadual()));
        empresa.setInscricaoMunicipal(blankToNull(req.inscricaoMunicipal()));
        empresa.setCep(apenasDigitos(req.cep()));
        empresa.setLogradouro(blankToNull(req.logradouro()));
        empresa.setNumero(blankToNull(req.numero()));
        empresa.setComplemento(blankToNull(req.complemento()));
        empresa.setBairro(blankToNull(req.bairro()));
        empresa.setMunicipio(blankToNull(req.municipio()));
        empresa.setUf(blankToNull(req.uf()));
        empresa.setCnaePrincipal(apenasDigitos(req.cnaePrincipal()));
        empresa.setCnaePrincipalDescricao(blankToNull(req.cnaePrincipalDescricao()));
        empresa.setOptanteSimples(Boolean.TRUE.equals(req.optanteSimples()));
        empresa.setBaixarXml(Boolean.TRUE.equals(req.baixarXml()));
        empresa.setSituacaoCadastral(blankToNull(req.situacaoCadastral()));
    }

    private Map<String, Object> resumo(Empresa empresa) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", empresa.getId());
        body.put("nome", empresa.getNome());
        body.put("cnpj", empresa.getCnpj());
        body.put("ativo", empresa.isAtivo());
        body.put("nomeFantasia", empresa.getNomeFantasia());
        body.put("municipio", empresa.getMunicipio());
        body.put("uf", empresa.getUf());
        body.put("cnaePrincipal", empresa.getCnaePrincipal());
        body.put("optanteSimples", empresa.isOptanteSimples());
        body.put("baixarXml", empresa.isBaixarXml());
        body.put("ultimoNsu", empresa.getUltimoNsu());
        body.put("certificadoCadastrado", certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresa.getId()).isPresent());
        body.put("logoCadastrado", empresaLogoService.existe(empresa.getId()));
        configuracaoRepository.findByEmpresaId(empresa.getId()).ifPresent(cfg -> {
            body.put("prefeitura", cfg.getPrefeitura());
            body.put("codigoMunicipioIbge", cfg.getCodigoMunicipioIbge());
            body.put("ambiente", cfg.getAmbiente());
            body.put("serieRps", cfg.getSerieRps());
            body.put("ultimoNumeroNfse", cfg.getUltimoNumeroNfse());
            body.put("proximoNumeroNfse", cfg.getUltimoNumeroNfse() + 1);
        });
        documentoRepository.findByEmpresaIdOrderByTipoAsc(empresa.getId()).forEach(doc -> {
            var prefix = doc.getTipo().toLowerCase();
            body.put(prefix + "Serie", doc.getSerie());
            body.put(prefix + "UltimoNumero", doc.getUltimoNumero());
            body.put(prefix + "Habilitado", doc.isHabilitado());
        });
        usuarioRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(empresa.getId()).ifPresent(u ->
                body.put("emailIntegracao", u.getEmail()));
        body.put("embedUrlCnpj", montarUrlCnpj(empresa.getCnpj(), null));
        return body;
    }

    private Map<String, Object> detalhe(Empresa empresa) {
        var body = resumo(empresa);
        body.put("email", empresa.getEmail());
        body.put("telefone", empresa.getTelefone());
        body.put("inscricaoEstadual", empresa.getInscricaoEstadual());
        body.put("inscricaoMunicipal", empresa.getInscricaoMunicipal());
        body.put("endereco", Map.of(
                "cep", nuloVazio(empresa.getCep()),
                "logradouro", nuloVazio(empresa.getLogradouro()),
                "numero", nuloVazio(empresa.getNumero()),
                "complemento", nuloVazio(empresa.getComplemento()),
                "bairro", nuloVazio(empresa.getBairro())));
        body.put("cnaePrincipalDescricao", empresa.getCnaePrincipalDescricao());
        body.put("situacaoCadastral", empresa.getSituacaoCadastral());
        usuarioRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(empresa.getId()).ifPresent(u -> {
            body.put("usuarioId", u.getId());
            body.put("usuarioNome", u.getNome());
        });
        body.put("embedUrlEmail", usuarioRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(empresa.getId())
                .map(u -> portalProperties.embedBaseUrl() + "/embed?email=" + u.getEmail() + "&senha=***")
                .orElse(null));
        body.put("enderecos", enderecoService.listarMapa(empresa.getId()));
        return body;
    }

    public String montarUrlCnpj(String cnpj, String senhaMascarada) {
        var base = portalProperties.embedBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        var url = base + "/embed/" + apenasDigitos(cnpj);
        if (senhaMascarada != null && !senhaMascarada.isBlank()) {
            url += "?senha=" + URLEncoder.encode(senhaMascarada, StandardCharsets.UTF_8);
        }
        return url;
    }

    private static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nuloVazio(String s) {
        return s == null ? "" : s;
    }

    private static String primeiroNaoVazio(String... vals) {
        for (var v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }
}
