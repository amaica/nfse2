package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.domain.fiscal.*;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.repository.fiscal.*;
import br.com.synki.nfse.portal.service.MembershipService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CadastroFiscalService {

    /** Teto de itens para listagens sem paginacao real na UI (evita full scan em bases grandes). */
    private static final PageRequest LIMITE_LISTAGEM = PageRequest.of(0, 1000);

    private final CfopRepository cfopRepo;
    private final NcmRepository ncmRepo;
    private final PessoaRepository pessoaRepo;
    private final ProdutoRepository produtoRepo;
    private final VeiculoRepository veiculoRepo;
    private final UsuarioRepository usuarioRepo;
    private final UsuarioEmpresaRepository usuarioEmpresaRepo;
    private final PasswordEncoder passwordEncoder;
    private final MembershipService membershipService;

    public CadastroFiscalService(
            CfopRepository cfopRepo,
            NcmRepository ncmRepo,
            PessoaRepository pessoaRepo,
            ProdutoRepository produtoRepo,
            VeiculoRepository veiculoRepo,
            UsuarioRepository usuarioRepo,
            UsuarioEmpresaRepository usuarioEmpresaRepo,
            PasswordEncoder passwordEncoder,
            MembershipService membershipService) {
        this.cfopRepo = cfopRepo;
        this.ncmRepo = ncmRepo;
        this.pessoaRepo = pessoaRepo;
        this.produtoRepo = produtoRepo;
        this.veiculoRepo = veiculoRepo;
        this.usuarioRepo = usuarioRepo;
        this.usuarioEmpresaRepo = usuarioEmpresaRepo;
        this.passwordEncoder = passwordEncoder;
        this.membershipService = membershipService;
    }

    public List<Cfop> listarCfop(Long empresaId) {
        return cfopRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByCfopAsc(empresaId, LIMITE_LISTAGEM);
    }

    @Transactional
    public Cfop salvarCfop(Long empresaId, Cfop body) {
        body.setEmpresaId(empresaId);
        return cfopRepo.save(body);
    }

    @Transactional
    public Cfop atualizarCfop(Long empresaId, Long id, Cfop body) {
        var atual = cfopRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("CFOP nao encontrado"));
        atual.setCfop(body.getCfop());
        atual.setAplicacao(body.getAplicacao());
        atual.setDescricao(body.getDescricao());
        return cfopRepo.save(atual);
    }

    @Transactional
    public void excluirCfop(Long empresaId, Long id) {
        var atual = cfopRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("CFOP nao encontrado"));
        cfopRepo.delete(atual);
    }

    public List<Ncm> listarNcm(String q) {
        if (q == null || q.isBlank()) {
            return ncmRepo.findAll(LIMITE_LISTAGEM).getContent();
        }
        return ncmRepo.findByCodigoContainingOrDescricaoContainingOrderByCodigoAsc(q, q, LIMITE_LISTAGEM);
    }

    @Transactional
    public Ncm salvarNcm(Ncm body) {
        return ncmRepo.save(body);
    }

    @Transactional
    public Ncm atualizarNcm(Long id, Ncm body) {
        var atual = ncmRepo.findById(id).orElseThrow(() -> new NoSuchElementException("NCM nao encontrado"));
        atual.setCodigo(body.getCodigo());
        atual.setDescricao(body.getDescricao());
        atual.setObservacao(body.getObservacao());
        return ncmRepo.save(atual);
    }

    @Transactional
    public void excluirNcm(Long id) {
        ncmRepo.deleteById(id);
    }

    public List<Pessoa> listarPessoas(Long empresaId, String q) {
        if (q != null && !q.isBlank()) {
            return pessoaRepo.findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAsc(empresaId, q, LIMITE_LISTAGEM);
        }
        return pessoaRepo.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId, LIMITE_LISTAGEM);
    }

    public Pessoa obterPessoa(Long empresaId, Long id) {
        return pessoaRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Pessoa nao encontrada"));
    }

    @Transactional
    public Pessoa salvarPessoa(Long empresaId, Pessoa body) {
        body.setEmpresaId(empresaId);
        return pessoaRepo.save(body);
    }

    @Transactional
    public Pessoa atualizarPessoa(Long empresaId, Long id, Pessoa body) {
        var atual = obterPessoa(empresaId, id);
        copiarPessoa(body, atual);
        return pessoaRepo.save(atual);
    }

    @Transactional
    public void excluirPessoa(Long empresaId, Long id) {
        var p = obterPessoa(empresaId, id);
        p.setAtivo(false);
        pessoaRepo.save(p);
    }

    public List<Produto> listarProdutos(Long empresaId) {
        return produtoRepo.findByEmpresaIdOrderByNomeAsc(empresaId, LIMITE_LISTAGEM);
    }

    public List<Produto> listarProdutosAtivos(Long empresaId) {
        return produtoRepo.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId, LIMITE_LISTAGEM);
    }

    public List<Produto> buscarProdutos(Long empresaId, String q) {
        if (q == null || q.isBlank()) {
            return listarProdutosAtivos(empresaId).stream().limit(40).toList();
        }
        return produtoRepo.buscarAtivosPorNomeOuCodigo(empresaId, q.trim(), PageRequest.of(0, 40));
    }

    public Produto obterProduto(Long empresaId, Long id) {
        return produtoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Produto nao encontrado"));
    }

    @Transactional
    public Produto salvarProduto(Long empresaId, Produto body) {
        body.setEmpresaId(empresaId);
        if (body.getTipo() == null || body.getTipo().isBlank()) {
            body.setTipo("P");
        }
        if (body.getOrigem() == null || body.getOrigem().isBlank()) {
            body.setOrigem("0");
        }
        if (body.getUnidade() == null || body.getUnidade().isBlank()) {
            body.setUnidade("UN");
        }
        body.setDescricaoPdv(blankToNull(body.getDescricaoPdv()));
        body.setGtin(blankToNull(body.getGtin()));
        body.setCodigoNcm(blankToNull(body.getCodigoNcm()));
        body.setCest(blankToNull(body.getCest()));
        body.setExTipi(blankToNull(body.getExTipi()));
        body.setObservacoes(blankToNull(body.getObservacoes()));
        return produtoRepo.save(body);
    }

    @Transactional
    public Produto atualizarProduto(Long empresaId, Long id, Produto body) {
        var atual = obterProduto(empresaId, id);
        copiarProduto(body, atual);
        return produtoRepo.save(atual);
    }

    @Transactional
    public void excluirProduto(Long empresaId, Long id) {
        var p = obterProduto(empresaId, id);
        p.setAtivo(false);
        produtoRepo.save(p);
    }

    public List<Veiculo> listarVeiculos(Long empresaId) {
        return veiculoRepo.findByEmpresaIdOrderByPlacaAsc(empresaId, LIMITE_LISTAGEM);
    }

    public List<Veiculo> listarVeiculosAtivos(Long empresaId) {
        return veiculoRepo.findByEmpresaIdAndAtivoTrueOrderByPlacaAsc(empresaId, LIMITE_LISTAGEM);
    }

    public Veiculo obterVeiculo(Long empresaId, Long id) {
        return veiculoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Veiculo nao encontrado"));
    }

    @Transactional
    public Veiculo salvarVeiculo(Long empresaId, Veiculo body) {
        body.setEmpresaId(empresaId);
        normalizarVeiculo(body);
        return veiculoRepo.save(body);
    }

    @Transactional
    public Veiculo atualizarVeiculo(Long empresaId, Long id, Veiculo body) {
        var atual = obterVeiculo(empresaId, id);
        copiarVeiculo(body, atual);
        return veiculoRepo.save(atual);
    }

    @Transactional
    public void excluirVeiculo(Long empresaId, Long id) {
        var v = obterVeiculo(empresaId, id);
        v.setAtivo(false);
        veiculoRepo.save(v);
    }

    public List<Map<String, Object>> listarUsuarios(Long empresaId) {
        return usuarioRepo.findByEmpresaId(empresaId).stream()
                .map(CadastroFiscalService::usuarioResumo)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> salvarUsuario(Long gestorId, Long empresaId, UsuarioRequest req) {
        membershipService.requireGestao(gestorId, empresaId);
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) {
            throw new IllegalStateException("Conta nao encontrada");
        }

        var email = req.email().trim().toLowerCase();
        var papel = req.perfil() != null ? req.perfil() : UsuarioEmpresa.PAPEL_OPERADOR;

        var existente = usuarioRepo.findByEmail(email);
        if (existente.isPresent()) {
            var u = existente.get();
            if (!u.isAtivo()) {
                throw new IllegalArgumentException("Usuario inativo");
            }
            var outraConta = membershipService.contaIdDoUsuario(u.getId());
            if (outraConta != null && !outraConta.equals(contaId)) {
                throw new IllegalArgumentException("E-mail ja cadastrado em outra conta");
            }
            membershipService.vincularUsuarioEmpresa(u.getId(), empresaId, contaId, papel);
            if (req.senha() != null && !req.senha().isBlank()) {
                u.setSenha(passwordEncoder.encode(req.senha()));
                usuarioRepo.save(u);
            }
            return usuarioResumo(u, papel);
        }

        if (req.senha() == null || req.senha().isBlank()) {
            throw new IllegalArgumentException("Senha obrigatoria para novo usuario");
        }

        var u = Usuario.create(empresaId, req.nome(), email, passwordEncoder.encode(req.senha()));
        if (req.cpf() != null) u.setCpf(req.cpf().replaceAll("\\D", ""));
        u.setPerfil(papel);
        u = usuarioRepo.save(u);
        membershipService.vincularUsuarioEmpresa(u.getId(), empresaId, contaId, papel);
        return usuarioResumo(u, papel);
    }

    @Transactional
    public Map<String, Object> atualizarUsuario(Long gestorId, Long empresaId, Long id, UsuarioRequest req) {
        membershipService.requireGestao(gestorId, empresaId);
        var u = usuarioRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario nao encontrado"));
        // Mesmo escopo da listagem: qualquer emitente em que o gestor é OWNER/ADMIN
        if (!gestorAdministraUsuario(gestorId, id)) {
            throw new NoSuchElementException("Usuario nao encontrado entre os emitentes que voce administra");
        }
        u.setNome(req.nome());
        if (req.cpf() != null) u.setCpf(req.cpf().replaceAll("\\D", ""));
        if (req.perfil() != null) u.setPerfil(req.perfil());
        if (req.ativo() != null) u.setAtivo(req.ativo());
        if (req.senha() != null && !req.senha().isBlank()) {
            u.setSenha(passwordEncoder.encode(req.senha()));
        }
        return usuarioResumo(usuarioRepo.save(u), req.perfil());
    }

    private boolean gestorAdministraUsuario(Long gestorId, Long usuarioAlvoId) {
        Set<Long> delegaveis = membershipService.listarEmpresasDelegaveisPorGestor(gestorId).stream()
                .map(e -> e.getId())
                .collect(Collectors.toSet());
        if (delegaveis.isEmpty()) {
            return false;
        }
        return usuarioEmpresaRepo.findByUsuarioIdAndAtivoTrueOrderByEmpresaIdAsc(usuarioAlvoId).stream()
                .anyMatch(ue -> delegaveis.contains(ue.getEmpresaId()));
    }

    private static Map<String, Object> usuarioResumo(Usuario u, String papel) {
        return Map.of(
                "id", u.getId(),
                "nome", u.getNome(),
                "email", u.getEmail(),
                "cpf", u.getCpf() != null ? u.getCpf() : "",
                "perfil", papel != null ? papel : (u.getPerfil() != null ? u.getPerfil() : "OPERADOR"),
                "ativo", u.isAtivo());
    }

    private static Map<String, Object> usuarioResumo(Usuario u) {
        return usuarioResumo(u, u.getPerfil());
    }

    private static void copiarPessoa(Pessoa src, Pessoa dst) {
        dst.setNome(src.getNome());
        dst.setNomeFantasia(src.getNomeFantasia());
        dst.setTipo(src.getTipo());
        dst.setCpfCnpj(src.getCpfCnpj());
        dst.setEmail(src.getEmail());
        dst.setFone(src.getFone());
        dst.setCelular(src.getCelular());
        dst.setInscricaoEstadual(src.getInscricaoEstadual());
        dst.setLogradouro(src.getLogradouro());
        dst.setNumero(src.getNumero());
        dst.setComplemento(src.getComplemento());
        dst.setBairro(src.getBairro());
        dst.setMunicipio(src.getMunicipio());
        dst.setUf(src.getUf());
        dst.setCep(src.getCep());
        dst.setCodigoMunicipioIbge(src.getCodigoMunicipioIbge());
        dst.setLatitude(src.getLatitude());
        dst.setLongitude(src.getLongitude());
        dst.setObservacoes(src.getObservacoes());
        dst.setAtivo(src.isAtivo());
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void copiarProduto(Produto src, Produto dst) {
        dst.setCodigo(src.getCodigo());
        dst.setNome(src.getNome());
        dst.setDescricaoPdv(blankToNull(src.getDescricaoPdv()));
        dst.setGtin(blankToNull(src.getGtin()));
        dst.setCodigoNcm(blankToNull(src.getCodigoNcm()));
        dst.setCest(blankToNull(src.getCest()));
        dst.setExTipi(blankToNull(src.getExTipi()));
        dst.setUnidade(src.getUnidade());
        dst.setOrigem(src.getOrigem() != null && !src.getOrigem().isBlank() ? src.getOrigem() : "0");
        dst.setTipo(src.getTipo() != null && !src.getTipo().isBlank() ? src.getTipo() : "P");
        dst.setValorUnitario(src.getValorUnitario());
        dst.setValorCusto(src.getValorCusto());
        dst.setMarkup(src.getMarkup());
        dst.setPeso(src.getPeso());
        dst.setEstoqueMinimo(src.getEstoqueMinimo());
        dst.setEstoqueAtual(src.getEstoqueAtual());
        dst.setObservacoes(blankToNull(src.getObservacoes()));
        dst.setGrupoTributarioId(src.getGrupoTributarioId());
        dst.setGrupoId(src.getGrupoId());
        dst.setSubgrupoId(src.getSubgrupoId());
        dst.setAtivo(src.isAtivo());
    }

    private static void copiarVeiculo(Veiculo src, Veiculo dst) {
        dst.setPlaca(src.getPlaca());
        dst.setModelo(src.getModelo());
        dst.setMarca(src.getMarca());
        dst.setRenavam(src.getRenavam());
        dst.setTipoRodado(src.getTipoRodado());
        dst.setTipoCarroceria(src.getTipoCarroceria());
        dst.setAtivo(src.isAtivo());
        normalizarVeiculo(dst);
    }

    private static void normalizarVeiculo(Veiculo v) {
        if (v.getPlaca() != null) {
            v.setPlaca(v.getPlaca().replaceAll("[^A-Za-z0-9]", "").toUpperCase());
        }
        v.setModelo(blankToNull(v.getModelo()));
        v.setMarca(blankToNull(v.getMarca()));
        v.setRenavam(blankToNull(v.getRenavam()));
        v.setTipoRodado(blankToNull(v.getTipoRodado()));
        v.setTipoCarroceria(blankToNull(v.getTipoCarroceria()));
    }

    public record UsuarioRequest(String nome, String email, String senha, String cpf, String perfil, Boolean ativo) {}
}
