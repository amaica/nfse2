package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.fiscal.*;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.repository.fiscal.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CadastroFiscalService {

    private final CfopRepository cfopRepo;
    private final NcmRepository ncmRepo;
    private final PessoaRepository pessoaRepo;
    private final ProdutoRepository produtoRepo;
    private final VeiculoRepository veiculoRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public CadastroFiscalService(
            CfopRepository cfopRepo,
            NcmRepository ncmRepo,
            PessoaRepository pessoaRepo,
            ProdutoRepository produtoRepo,
            VeiculoRepository veiculoRepo,
            UsuarioRepository usuarioRepo,
            PasswordEncoder passwordEncoder) {
        this.cfopRepo = cfopRepo;
        this.ncmRepo = ncmRepo;
        this.pessoaRepo = pessoaRepo;
        this.produtoRepo = produtoRepo;
        this.veiculoRepo = veiculoRepo;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Cfop> listarCfop(Long empresaId) {
        return cfopRepo.findByEmpresaIdIsNullOrEmpresaIdOrderByCfopAsc(empresaId);
    }

    @Transactional
    public Cfop salvarCfop(Long empresaId, Cfop body) {
        body.setEmpresaId(empresaId);
        return cfopRepo.save(body);
    }

    @Transactional
    public Cfop atualizarCfop(Long id, Cfop body) {
        var atual = cfopRepo.findById(id).orElseThrow(() -> new NoSuchElementException("CFOP nao encontrado"));
        atual.setCfop(body.getCfop());
        atual.setAplicacao(body.getAplicacao());
        atual.setDescricao(body.getDescricao());
        return cfopRepo.save(atual);
    }

    @Transactional
    public void excluirCfop(Long id) {
        cfopRepo.deleteById(id);
    }

    public List<Ncm> listarNcm(String q) {
        if (q == null || q.isBlank()) {
            return ncmRepo.findAll();
        }
        return ncmRepo.findByCodigoContainingOrDescricaoContainingOrderByCodigoAsc(q, q);
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
            return pessoaRepo.findByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAsc(empresaId, q);
        }
        return pessoaRepo.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId);
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
        return produtoRepo.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId);
    }

    public Produto obterProduto(Long empresaId, Long id) {
        return produtoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Produto nao encontrado"));
    }

    @Transactional
    public Produto salvarProduto(Long empresaId, Produto body) {
        body.setEmpresaId(empresaId);
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
        return veiculoRepo.findByEmpresaIdAndAtivoTrueOrderByPlacaAsc(empresaId);
    }

    public Veiculo obterVeiculo(Long empresaId, Long id) {
        return veiculoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Veiculo nao encontrado"));
    }

    @Transactional
    public Veiculo salvarVeiculo(Long empresaId, Veiculo body) {
        body.setEmpresaId(empresaId);
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
    public Map<String, Object> salvarUsuario(Long empresaId, UsuarioRequest req) {
        var u = Usuario.create(empresaId, req.nome(), req.email(), passwordEncoder.encode(req.senha()));
        if (req.cpf() != null) u.setCpf(req.cpf().replaceAll("\\D", ""));
        if (req.perfil() != null) u.setPerfil(req.perfil());
        return usuarioResumo(usuarioRepo.save(u));
    }

    @Transactional
    public Map<String, Object> atualizarUsuario(Long empresaId, Long id, UsuarioRequest req) {
        var u = usuarioRepo.findById(id).filter(x -> x.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new NoSuchElementException("Usuario nao encontrado"));
        u.setNome(req.nome());
        u.setEmail(req.email());
        if (req.cpf() != null) u.setCpf(req.cpf().replaceAll("\\D", ""));
        if (req.perfil() != null) u.setPerfil(req.perfil());
        if (req.ativo() != null) u.setAtivo(req.ativo());
        if (req.senha() != null && !req.senha().isBlank()) {
            u.setSenha(passwordEncoder.encode(req.senha()));
        }
        return usuarioResumo(usuarioRepo.save(u));
    }

    private static Map<String, Object> usuarioResumo(Usuario u) {
        return Map.of(
                "id", u.getId(),
                "nome", u.getNome(),
                "email", u.getEmail(),
                "cpf", u.getCpf() != null ? u.getCpf() : "",
                "perfil", u.getPerfil() != null ? u.getPerfil() : "OPERADOR",
                "ativo", u.isAtivo());
    }

    private static void copiarPessoa(Pessoa src, Pessoa dst) {
        dst.setNome(src.getNome());
        dst.setTipo(src.getTipo());
        dst.setCpfCnpj(src.getCpfCnpj());
        dst.setEmail(src.getEmail());
        dst.setInscricaoEstadual(src.getInscricaoEstadual());
        dst.setLogradouro(src.getLogradouro());
        dst.setNumero(src.getNumero());
        dst.setBairro(src.getBairro());
        dst.setMunicipio(src.getMunicipio());
        dst.setUf(src.getUf());
        dst.setCep(src.getCep());
        dst.setCodigoMunicipioIbge(src.getCodigoMunicipioIbge());
        dst.setAtivo(src.isAtivo());
    }

    private static void copiarProduto(Produto src, Produto dst) {
        dst.setCodigo(src.getCodigo());
        dst.setNome(src.getNome());
        dst.setGtin(src.getGtin());
        dst.setCodigoNcm(src.getCodigoNcm());
        dst.setUnidade(src.getUnidade());
        dst.setValorUnitario(src.getValorUnitario());
        dst.setGrupoTributarioId(src.getGrupoTributarioId());
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
    }

    public record UsuarioRequest(String nome, String email, String senha, String cpf, String perfil, Boolean ativo) {}
}
