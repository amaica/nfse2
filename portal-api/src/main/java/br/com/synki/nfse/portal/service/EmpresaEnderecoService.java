package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.web.dto.EnderecoEmpresaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmpresaEnderecoService {

    private final EmpresaEnderecoRepository repository;

    public EmpresaEnderecoService(EmpresaEnderecoRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listarMapa(Long empresaId) {
        return repository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream()
                .map(this::paraMapa)
                .toList();
    }

    @Transactional
    public List<EmpresaEndereco> sincronizar(Long empresaId, List<EnderecoEmpresaRequest> itensInput) {
        if (itensInput == null || itensInput.isEmpty()) {
            return repository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId);
        }
        var itens = garantirUmPrincipal(itensInput);
        var mantidos = new ArrayList<Long>();
        for (var req : itens) {
            var end = req.id() != null
                    ? repository.findById(req.id()).filter(e -> e.getEmpresaId().equals(empresaId))
                            .orElseThrow(() -> new IllegalArgumentException("Endereco nao encontrado: " + req.id()))
                    : new EmpresaEndereco();
            if (req.id() == null) {
                end.setEmpresaId(empresaId);
            }
            aplicar(end, req);
            repository.save(end);
            mantidos.add(end.getId());
        }
        var remover = repository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream()
                .map(EmpresaEndereco::getId)
                .filter(id -> !mantidos.contains(id))
                .collect(Collectors.toSet());
        if (mantidos.size() < 1 || mantidos.size() == remover.size()) {
            throw new IllegalArgumentException("Empresa deve ter ao menos um endereco");
        }
        remover.forEach(repository::deleteById);
        normalizarPrincipalUnico(empresaId);
        return repository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId);
    }

    @Transactional
    public EmpresaEndereco criarMatrizFromEmpresa(
            Long empresaId, Empresa empresa, String codigoMunicipioIbge, String serieNfe, Long ultimoNumeroNfe) {
        if (!repository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).isEmpty()) {
            return repository.findByEmpresaIdAndPrincipalTrue(empresaId).orElse(null);
        }
        var end = new EmpresaEndereco();
        end.setEmpresaId(empresaId);
        end.setApelido("Matriz");
        end.setCep(empresa.getCep());
        end.setLogradouro(empresa.getLogradouro());
        end.setNumero(empresa.getNumero());
        end.setComplemento(empresa.getComplemento());
        end.setBairro(empresa.getBairro());
        end.setMunicipio(empresa.getMunicipio());
        end.setUf(empresa.getUf());
        end.setCodigoMunicipioIbge(apenasDigitos(codigoMunicipioIbge));
        end.setInscricaoEstadual(empresa.getInscricaoEstadual());
        end.setSerieNfe(serieNfe != null && !serieNfe.isBlank()
                ? serieNfe.trim()
                : NfeSerieUtil.seriePadrao(empresa.getCnpj()));
        end.setUltimoNumeroNfe(ultimoNumeroNfe != null ? Math.max(0, ultimoNumeroNfe) : 0);
        end.setPrincipal(true);
        end.setAtivo(true);
        return repository.save(end);
    }

    public void sincronizarEmpresaFromPrincipal(Empresa empresa) {
        repository.findByEmpresaIdAndPrincipalTrue(empresa.getId()).ifPresent(p -> {
            empresa.setCep(p.getCep());
            empresa.setLogradouro(p.getLogradouro());
            empresa.setNumero(p.getNumero());
            empresa.setComplemento(p.getComplemento());
            empresa.setBairro(p.getBairro());
            empresa.setMunicipio(p.getMunicipio());
            empresa.setUf(p.getUf());
            empresa.setInscricaoEstadual(p.getInscricaoEstadual());
        });
    }

    private void aplicar(EmpresaEndereco end, EnderecoEmpresaRequest req) {
        if (req.apelido() == null || req.apelido().isBlank()) {
            throw new IllegalArgumentException("Apelido do endereco obrigatorio");
        }
        end.setApelido(req.apelido().trim());
        end.setCep(apenasDigitos(req.cep()));
        end.setLogradouro(blankToNull(req.logradouro()));
        end.setNumero(blankToNull(req.numero()));
        end.setComplemento(blankToNull(req.complemento()));
        end.setBairro(blankToNull(req.bairro()));
        end.setMunicipio(blankToNull(req.municipio()));
        end.setUf(blankToNull(req.uf()));
        end.setCodigoMunicipioIbge(apenasDigitos(req.codigoMunicipioIbge()));
        end.setInscricaoEstadual(blankToNull(req.inscricaoEstadual()));
        end.setSerieNfe(req.serieNfe() != null && !req.serieNfe().isBlank()
                ? req.serieNfe().trim()
                : NfeSerieUtil.SERIE_CPF_PADRAO);
        end.setUltimoNumeroNfe(req.ultimoNumeroNfe() != null ? Math.max(0, req.ultimoNumeroNfe()) : 0);
        end.setPrincipal(Boolean.TRUE.equals(req.principal()));
        end.setAtivo(req.ativo() == null || req.ativo());
    }

    private List<EnderecoEmpresaRequest> garantirUmPrincipal(List<EnderecoEmpresaRequest> itens) {
        if (itens.stream().anyMatch(i -> Boolean.TRUE.equals(i.principal()))) {
            return itens;
        }
        var first = itens.get(0);
        var copy = new ArrayList<>(itens);
        copy.set(0, new EnderecoEmpresaRequest(
                first.id(), first.apelido(), first.cep(), first.logradouro(), first.numero(),
                first.complemento(), first.bairro(), first.municipio(), first.uf(),
                first.codigoMunicipioIbge(), first.inscricaoEstadual(), first.serieNfe(),
                first.ultimoNumeroNfe(), true, first.ativo()));
        return copy;
    }

    private void normalizarPrincipalUnico(Long empresaId) {
        var todos = repository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId);
        var principal = todos.stream().filter(EmpresaEndereco::isPrincipal).findFirst();
        if (principal.isEmpty() && !todos.isEmpty()) {
            var p = todos.get(0);
            p.setPrincipal(true);
            repository.save(p);
            return;
        }
        principal.ifPresent(p -> todos.stream()
                .filter(e -> !e.getId().equals(p.getId()) && e.isPrincipal())
                .forEach(e -> {
                    e.setPrincipal(false);
                    repository.save(e);
                }));
    }

    private Map<String, Object> paraMapa(EmpresaEndereco e) {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", e.getId());
        map.put("apelido", e.getApelido());
        map.put("cep", nuloVazio(e.getCep()));
        map.put("logradouro", nuloVazio(e.getLogradouro()));
        map.put("numero", nuloVazio(e.getNumero()));
        map.put("complemento", nuloVazio(e.getComplemento()));
        map.put("bairro", nuloVazio(e.getBairro()));
        map.put("municipio", nuloVazio(e.getMunicipio()));
        map.put("uf", nuloVazio(e.getUf()));
        map.put("codigoMunicipioIbge", nuloVazio(e.getCodigoMunicipioIbge()));
        map.put("inscricaoEstadual", nuloVazio(e.getInscricaoEstadual()));
        map.put("serieNfe", e.getSerieNfe());
        map.put("ultimoNumeroNfe", e.getUltimoNumeroNfe());
        map.put("proximoNumeroNfe", e.getUltimoNumeroNfe() + 1);
        map.put("principal", e.isPrincipal());
        map.put("ativo", e.isAtivo());
        return map;
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
}
