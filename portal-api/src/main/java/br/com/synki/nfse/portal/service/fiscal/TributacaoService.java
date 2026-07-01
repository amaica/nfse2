package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.*;
import br.com.synki.nfse.portal.repository.fiscal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TributacaoService {

    private final TributGrupoTributarioRepository grupoRepo;
    private final TributOperacaoFiscalRepository operacaoRepo;
    private final TributConfiguraOfGtRepository configRepo;
    private final TributNfseServicoRepository nfseServicoRepo;

    public TributacaoService(
            TributGrupoTributarioRepository grupoRepo,
            TributOperacaoFiscalRepository operacaoRepo,
            TributConfiguraOfGtRepository configRepo,
            TributNfseServicoRepository nfseServicoRepo) {
        this.grupoRepo = grupoRepo;
        this.operacaoRepo = operacaoRepo;
        this.configRepo = configRepo;
        this.nfseServicoRepo = nfseServicoRepo;
    }

    public List<TributGrupoTributario> listarGrupos(Long empresaId) {
        return grupoRepo.findByEmpresaIdOrderByDescricaoAsc(empresaId);
    }

    public TributGrupoTributario obterGrupo(Long empresaId, Long id) {
        return grupoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Grupo tributario nao encontrado"));
    }

    @Transactional
    public TributGrupoTributario salvarGrupo(Long empresaId, TributGrupoTributario body) {
        body.setEmpresaId(empresaId);
        return grupoRepo.save(body);
    }

    @Transactional
    public TributGrupoTributario atualizarGrupo(Long empresaId, Long id, TributGrupoTributario body) {
        var atual = obterGrupo(empresaId, id);
        atual.setDescricao(body.getDescricao());
        atual.setOrigemMercadoria(body.getOrigemMercadoria());
        atual.setObservacao(body.getObservacao());
        return grupoRepo.save(atual);
    }

    @Transactional
    public void excluirGrupo(Long empresaId, Long id) {
        grupoRepo.delete(obterGrupo(empresaId, id));
    }

    public List<TributOperacaoFiscal> listarOperacoes(Long empresaId) {
        return operacaoRepo.findByEmpresaIdOrderByDescricaoAsc(empresaId);
    }

    public TributOperacaoFiscal obterOperacao(Long empresaId, Long id) {
        return operacaoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Operacao fiscal nao encontrada"));
    }

    @Transactional
    public TributOperacaoFiscal salvarOperacao(Long empresaId, TributOperacaoFiscal body) {
        body.setEmpresaId(empresaId);
        return operacaoRepo.save(body);
    }

    @Transactional
    public TributOperacaoFiscal atualizarOperacao(Long empresaId, Long id, TributOperacaoFiscal body) {
        var atual = obterOperacao(empresaId, id);
        copiarOperacao(body, atual);
        return operacaoRepo.save(atual);
    }

    @Transactional
    public void excluirOperacao(Long empresaId, Long id) {
        operacaoRepo.delete(obterOperacao(empresaId, id));
    }

    public List<TributConfiguraOfGt> listarConfiguracoes(Long empresaId) {
        return configRepo.findByEmpresaIdOrderByIdAsc(empresaId);
    }

    @Transactional(readOnly = true)
    public TributConfiguraOfGt obterConfiguracao(Long empresaId, Long id) {
        return configRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Configuracao OF/GT nao encontrada"));
    }

    @Transactional
    public TributConfiguraOfGt salvarConfiguracao(Long empresaId, TributConfiguraOfGt body) {
        body.setEmpresaId(empresaId);
        if (body.getListaIcmsUf() != null) {
            body.getListaIcmsUf().forEach(icms -> icms.setConfiguraOfGt(body));
        }
        Long id = configRepo.save(body).getId();
        return obterConfiguracao(empresaId, id);
    }

    @Transactional
    public TributConfiguraOfGt atualizarConfiguracao(Long empresaId, Long id, TributConfiguraOfGt body) {
        var atual = obterConfiguracao(empresaId, id);
        atual.setTributOperacaoFiscalId(body.getTributOperacaoFiscalId());
        atual.setTributGrupoTributarioId(body.getTributGrupoTributarioId());
        atual.getListaIcmsUf().clear();
        if (body.getListaIcmsUf() != null) {
            for (var icms : body.getListaIcmsUf()) {
                icms.setConfiguraOfGt(atual);
                atual.getListaIcmsUf().add(icms);
            }
        }
        configRepo.save(atual);
        return obterConfiguracao(empresaId, id);
    }

    @Transactional
    public void excluirConfiguracao(Long empresaId, Long id) {
        configRepo.delete(obterConfiguracao(empresaId, id));
    }

    public List<TributNfseServico> listarNfseServicos(Long empresaId, boolean apenasAtivos) {
        return listarNfseServicos(empresaId, apenasAtivos, null);
    }

    public List<TributNfseServico> listarNfseServicos(Long empresaId, boolean apenasAtivos, String q) {
        var lista = apenasAtivos
                ? nfseServicoRepo.findByEmpresaIdAndAtivoTrueOrderByPrincipalDescDescricaoAsc(empresaId)
                : nfseServicoRepo.findByEmpresaIdOrderByPrincipalDescDescricaoAsc(empresaId);
        if (q == null || q.isBlank()) {
            return lista;
        }
        var termo = q.trim().toLowerCase();
        return lista.stream()
                .filter(s -> s.getDescricao().toLowerCase().contains(termo)
                        || s.getItemListaServico().toLowerCase().contains(termo)
                        || (s.getDescricaoServico() != null && s.getDescricaoServico().toLowerCase().contains(termo)))
                .toList();
    }

    public TributNfseServico obterNfseServico(Long empresaId, Long id) {
        return nfseServicoRepo.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new NoSuchElementException("Tributacao NFS-e nao encontrada"));
    }

    @Transactional
    public TributNfseServico salvarNfseServico(Long empresaId, TributNfseServico body) {
        body.setEmpresaId(empresaId);
        if (body.isPrincipal()) {
            desmarcarPrincipalNfse(empresaId, null);
        }
        return nfseServicoRepo.save(body);
    }

    @Transactional
    public TributNfseServico atualizarNfseServico(Long empresaId, Long id, TributNfseServico body) {
        var atual = obterNfseServico(empresaId, id);
        copiarNfseServico(body, atual);
        if (atual.isPrincipal()) {
            desmarcarPrincipalNfse(empresaId, atual.getId());
        }
        return nfseServicoRepo.save(atual);
    }

    @Transactional
    public void excluirNfseServico(Long empresaId, Long id) {
        nfseServicoRepo.delete(obterNfseServico(empresaId, id));
    }

    private void desmarcarPrincipalNfse(Long empresaId, Long excetoId) {
        for (var item : nfseServicoRepo.findByEmpresaIdOrderByPrincipalDescDescricaoAsc(empresaId)) {
            if (excetoId == null || !item.getId().equals(excetoId)) {
                if (item.isPrincipal()) {
                    item.setPrincipal(false);
                    nfseServicoRepo.save(item);
                }
            }
        }
    }

    private static void copiarNfseServico(TributNfseServico src, TributNfseServico dst) {
        dst.setDescricao(src.getDescricao());
        dst.setItemListaServico(src.getItemListaServico());
        dst.setCodigoTributacaoMunicipio(src.getCodigoTributacaoMunicipio());
        dst.setNbs(src.getNbs());
        dst.setCnae(src.getCnae());
        dst.setDescricaoServico(src.getDescricaoServico());
        dst.setMunicipioPrestacaoIbge(src.getMunicipioPrestacaoIbge());
        dst.setAliquotaIss(src.getAliquotaIss());
        dst.setTributacaoIssqn(src.getTributacaoIssqn());
        dst.setIssRetido(src.getIssRetido());
        dst.setSimplesNacional(src.getSimplesNacional());
        dst.setRegimeEspecial(src.getRegimeEspecial());
        dst.setCstPisCofins(src.getCstPisCofins());
        dst.setAliquotaPis(src.getAliquotaPis());
        dst.setAliquotaCofins(src.getAliquotaCofins());
        dst.setTipoRetencaoPisCofins(src.getTipoRetencaoPisCofins());
        dst.setHabilitarRetencoes(src.isHabilitarRetencoes());
        dst.setRetencaoInss(src.getRetencaoInss());
        dst.setRetencaoIrrf(src.getRetencaoIrrf());
        dst.setRetencaoCsll(src.getRetencaoCsll());
        dst.setIbsCbsCst(src.getIbsCbsCst());
        dst.setIbsCbsClassTrib(src.getIbsCbsClassTrib());
        dst.setAliquotaIbs(src.getAliquotaIbs());
        dst.setAliquotaCbs(src.getAliquotaCbs());
        dst.setHabilitarIbsCbs(src.isHabilitarIbsCbs());
        dst.setPrincipal(src.isPrincipal());
        dst.setAtivo(src.isAtivo());
    }

    private static void copiarOperacao(TributOperacaoFiscal src, TributOperacaoFiscal dst) {
        dst.setDescricao(src.getDescricao());
        dst.setTipoOperacao(src.getTipoOperacao());
        dst.setGeraFinanceiro(src.getGeraFinanceiro());
        dst.setMovimentaEstoque(src.getMovimentaEstoque());
        dst.setDescricaoNaNf(src.getDescricaoNaNf());
        dst.setCfop(src.getCfop());
        dst.setObservacao(src.getObservacao());
        dst.setPrincipal(src.getPrincipal());
        dst.setFinalidade(src.getFinalidade());
        dst.setFinalidadeOperacao(src.getFinalidadeOperacao());
        dst.setCMunFGIBS(src.getCMunFGIBS());
        dst.setTpNFDebito(src.getTpNFDebito());
        dst.setTpNFCredito(src.getTpNFCredito());
        dst.setTpEnteGov(src.getTpEnteGov());
        dst.setPRedutor(src.getPRedutor());
        dst.setTpOperGov(src.getTpOperGov());
        dst.setIndIntermed(src.getIndIntermed());
        dst.setIbsCbsCst(src.getIbsCbsCst());
        dst.setIbsCbsClassTrib(src.getIbsCbsClassTrib());
        dst.setAliquotaIbsUf(src.getAliquotaIbsUf());
        dst.setAliquotaIbsMun(src.getAliquotaIbsMun());
        dst.setAliquotaCbs(src.getAliquotaCbs());
        dst.setHabilitarIbsCbs(src.isHabilitarIbsCbs());
    }
}
