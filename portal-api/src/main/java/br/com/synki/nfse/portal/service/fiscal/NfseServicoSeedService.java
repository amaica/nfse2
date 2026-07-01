package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.fiscal.TributNfseServico;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributNfseServicoRepository;
import br.com.synki.nfse.portal.service.EmpresaCnaeService;
import br.com.synki.nfse.portal.service.NbsService;
import br.com.synki.nfse.portal.service.ServicosLc116Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class NfseServicoSeedService {

    private static final Logger log = LoggerFactory.getLogger(NfseServicoSeedService.class);

    private static final List<String> PADRAO_LABORATORIO = List.of(
            "04.03.02.000", "04.02.04.000", "04.03.03.000");
    private static final List<String> PADRAO_AGRO = List.of(
            "07.16.01.000", "07.01.02.000", "07.02.01.000");
    private static final List<String> PADRAO_GENERICO = List.of(
            "17.01.01.000", "01.07.01.000", "14.01.01.000");

    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoNfseRepository configuracaoNfseRepository;
    private final TributNfseServicoRepository nfseServicoRepository;
    private final EmpresaCnaeService empresaCnaeService;
    private final ServicosLc116Service servicosLc116Service;
    private final NbsService nbsService;

    public NfseServicoSeedService(
            EmpresaRepository empresaRepository,
            ConfiguracaoNfseRepository configuracaoNfseRepository,
            TributNfseServicoRepository nfseServicoRepository,
            EmpresaCnaeService empresaCnaeService,
            ServicosLc116Service servicosLc116Service,
            NbsService nbsService) {
        this.empresaRepository = empresaRepository;
        this.configuracaoNfseRepository = configuracaoNfseRepository;
        this.nfseServicoRepository = nfseServicoRepository;
        this.empresaCnaeService = empresaCnaeService;
        this.servicosLc116Service = servicosLc116Service;
        this.nbsService = nbsService;
    }

    @Transactional
    public int garantirCadastros(Long empresaId) {
        if (nfseServicoRepository.countByEmpresaId(empresaId) > 0) {
            return 0;
        }
        var empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            return 0;
        }
        var servicos = resolverServicosLc116(empresa);
        if (servicos.isEmpty()) {
            return 0;
        }
        String municipioIbge = configuracaoNfseRepository.findByEmpresaId(empresaId)
                .map(c -> c.getCodigoMunicipioIbge())
                .orElse("4310009");
        String cnaePrincipal = resolverCnaePrincipal(empresa, empresaCnaeService);
        boolean simples = empresa.isOptanteSimples();

        int count = 0;
        boolean primeiro = true;
        for (var servico : servicos) {
            if (count >= 3) {
                break;
            }
            var cad = new TributNfseServico();
            cad.setEmpresaId(empresaId);
            cad.setDescricao(truncar(servico.descricao(), 255));
            cad.setItemListaServico(servico.codigo());
            cad.setDescricaoServico(servico.descricao());
            cad.setMunicipioPrestacaoIbge(municipioIbge);
            cad.setAliquotaIss(new BigDecimal("2.0000"));
            cad.setTributacaoIssqn("1");
            cad.setIssRetido("1");
            cad.setSimplesNacional(simples ? "2" : "1");
            cad.setRegimeEspecial("0");
            cad.setCstPisCofins("08");
            cad.setHabilitarIbsCbs(true);
            cad.setPrincipal(primeiro);
            cad.setAtivo(true);
            if (cnaePrincipal != null) {
                cad.setCnae(cnaePrincipal);
            }
            var nbs = nbsService.sugerirPorLc116(servico.codigo(), 1);
            if (!nbs.isEmpty()) {
                cad.setNbs(nbs.getFirst().codigoNacional());
            }
            nfseServicoRepository.save(cad);
            count++;
            primeiro = false;
        }
        if (count > 0) {
            log.info("Cadastros NFS-e padrao criados para empresa {}: {}", empresaId, count);
        }
        return count;
    }

    @Transactional
    public int garantirCadastrosTodasEmpresas() {
        int total = 0;
        for (var empresa : empresaRepository.findAll()) {
            if (empresa.isAtivo()) {
                total += garantirCadastros(empresa.getId());
            }
        }
        return total;
    }

    private List<ServicosLc116Service.ServicoLc116> resolverServicosLc116(Empresa empresa) {
        var codigos = new LinkedHashSet<String>();
        var cnaes = new ArrayList<String>();
        if (empresa.getCnaePrincipal() != null && !empresa.getCnaePrincipal().isBlank()) {
            cnaes.add(empresa.getCnaePrincipal());
        }
        String doc = empresa.getCnpj();
        if (doc != null && doc.length() == 14) {
            try {
                for (var cnae : empresaCnaeService.obterCnaes(doc)) {
                    cnaes.add(cnae.codigo());
                }
            } catch (Exception ex) {
                log.debug("CNAEs nao obtidos para empresa {}: {}", empresa.getId(), ex.getMessage());
            }
        }
        if (!cnaes.isEmpty()) {
            for (var s : servicosLc116Service.sugerirPorCnaes(cnaes, 5)) {
                codigos.add(s.codigo());
            }
        }
        if (codigos.isEmpty()) {
            var nome = empresa.getNome() != null ? empresa.getNome().toUpperCase(Locale.ROOT) : "";
            List<String> padrao;
            if (nome.contains("LABORATOR") || nome.contains("ANALISE") || nome.contains("CLINICA")) {
                padrao = PADRAO_LABORATORIO;
            } else if (empresa.getFluxoLegacyId() != null
                    || nome.contains("CEREAL") || nome.contains("SUINO") || nome.contains("AGRO")) {
                padrao = PADRAO_AGRO;
            } else {
                padrao = PADRAO_GENERICO;
            }
            codigos.addAll(padrao);
        }
        var result = new ArrayList<ServicosLc116Service.ServicoLc116>();
        for (String codigo : codigos) {
            servicosLc116Service.buscar(codigo, 1, "todos").stream()
                    .filter(s -> s.codigo().equals(codigo))
                    .findFirst()
                    .ifPresent(result::add);
            if (result.size() >= 3) {
                break;
            }
        }
        return result;
    }

    private static String resolverCnaePrincipal(Empresa empresa, EmpresaCnaeService empresaCnaeService) {
        if (empresa.getCnaePrincipal() != null && !empresa.getCnaePrincipal().isBlank()) {
            String digits = empresa.getCnaePrincipal().replaceAll("\\D", "");
            if (digits.length() >= 4) {
                return digits.substring(0, Math.min(7, digits.length()));
            }
        }
        String doc = empresa.getCnpj();
        if (doc != null && doc.length() == 14) {
            try {
                var cnaes = empresaCnaeService.obterCnaes(doc);
                var principal = cnaes.stream()
                        .filter(EmpresaCnaeService.CnaeEmpresa::principal)
                        .findFirst()
                        .or(() -> cnaes.stream().findFirst());
                return principal
                        .map(c -> c.codigo().replaceAll("\\D", ""))
                        .filter(d -> d.length() >= 4)
                        .map(d -> d.substring(0, Math.min(7, d.length())))
                        .orElse(null);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return "";
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
