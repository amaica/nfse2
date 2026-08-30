package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.repository.CertificadoRepository;
import br.com.synki.nfse.portal.repository.UsoMensalRepository;
import br.com.synki.nfse.portal.repository.fiscal.PessoaRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SetupProgressService {

    private static final ZoneId TZ = ZoneId.of("America/Sao_Paulo");

    private final MembershipService membershipService;
    private final CertificadoRepository certificadoRepository;
    private final PessoaRepository pessoaRepository;
    private final ProdutoRepository produtoRepository;
    private final UsoMensalRepository usoMensalRepository;

    public SetupProgressService(
            MembershipService membershipService,
            CertificadoRepository certificadoRepository,
            PessoaRepository pessoaRepository,
            ProdutoRepository produtoRepository,
            UsoMensalRepository usoMensalRepository) {
        this.membershipService = membershipService;
        this.certificadoRepository = certificadoRepository;
        this.pessoaRepository = pessoaRepository;
        this.produtoRepository = produtoRepository;
        this.usoMensalRepository = usoMensalRepository;
    }

    public Map<String, Object> progresso(Long usuarioId, Long empresaId) {
        membershipService.requireAccess(usuarioId, empresaId);

        var certificado = certificadoRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).isPresent();
        var clientes = pessoaRepository.countByEmpresaId(empresaId) > 0;
        var produtos = produtoRepository.countByEmpresaId(empresaId) > 0;
        var emissao = temEmissao(empresaId);

        var passos = new ArrayList<Map<String, Object>>();
        passos.add(passo("certificado", "Certificado A1", certificado, "/cadastros/empresa"));
        passos.add(passo("clientes", "Clientes cadastrados", clientes, "/cadastros/pessoas"));
        passos.add(passo("produtos", "Produtos cadastrados", produtos, "/cadastros/produtos"));
        passos.add(passo("emissao", "Primeira nota emitida", emissao, "/nfe/emissao"));

        long concluidos = passos.stream().filter(p -> Boolean.TRUE.equals(p.get("concluido"))).count();

        var body = new LinkedHashMap<String, Object>();
        body.put("passos", passos);
        body.put("concluidos", concluidos);
        body.put("total", passos.size());
        body.put("completo", concluidos == passos.size());
        body.put("percentual", passos.isEmpty() ? 0 : Math.round(concluidos * 100.0 / passos.size()));
        return body;
    }

    private boolean temEmissao(Long empresaId) {
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) {
            return false;
        }
        var mes = YearMonth.now(TZ).toString();
        return usoMensalRepository.findByContaIdAndAnoMes(contaId, mes)
                .map(u -> u.getNfseCount() + u.getNfeCount() > 0)
                .orElse(false);
    }

    private static Map<String, Object> passo(String id, String titulo, boolean concluido, String href) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("titulo", titulo);
        m.put("concluido", concluido);
        m.put("href", href);
        return m;
    }
}
