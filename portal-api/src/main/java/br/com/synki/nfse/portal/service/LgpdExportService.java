package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.repository.UsoMensalRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LgpdExportService {

    private final ContaRepository contaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembershipService membershipService;
    private final UsoMensalRepository usoMensalRepository;
    private final AuditLogService auditLogService;

    public LgpdExportService(
            ContaRepository contaRepository,
            AssinaturaRepository assinaturaRepository,
            UsuarioEmpresaRepository usuarioEmpresaRepository,
            UsuarioRepository usuarioRepository,
            MembershipService membershipService,
            UsoMensalRepository usoMensalRepository,
            AuditLogService auditLogService) {
        this.contaRepository = contaRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.membershipService = membershipService;
        this.usoMensalRepository = usoMensalRepository;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> exportarConta(Long contaId) {
        var conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
        var desde = Instant.now().minusSeconds(365L * 86400);

        var body = new LinkedHashMap<String, Object>();
        body.put("tipo", "lgpd_export_conta");
        body.put("versao", "1.0");
        body.put("geradoEm", Instant.now().toString());
        body.put("conta", Map.of(
                "id", conta.getId(),
                "nome", conta.getNome(),
                "status", conta.getStatus(),
                "createdAt", conta.getCreatedAt() != null ? conta.getCreatedAt().toString() : null));

        assinaturaRepository.findByContaId(contaId).ifPresent(a -> body.put("assinatura", Map.of(
                "status", a.getStatus(),
                "pacotes", a.getPacotes(),
                "periodoFim", a.getPeriodoFim() != null ? a.getPeriodoFim().toString() : null)));

        var usuarioIds = usuarioEmpresaRepository.findByContaIdAndAtivoTrueOrderByUsuarioIdAsc(contaId).stream()
                .map(m -> m.getUsuarioId())
                .distinct()
                .toList();
        body.put("usuarios", usuarioRepository.findAllById(usuarioIds).stream()
                .map(this::usuarioSemSenha)
                .toList());

        body.put("empresas", membershipService.listarTodasEmpresasPermitidasPorConta(contaId).stream()
                .map(this::empresaResumo)
                .toList());

        body.put("usoMensal12Meses", usoMensalRepository
                .findByContaIdOrderByAnoMesDesc(contaId, PageRequest.of(0, 12))
                .stream()
                .map(u -> Map.of(
                        "anoMes", u.getAnoMes(),
                        "nfse", u.getNfseCount(),
                        "nfe", u.getNfeCount()))
                .toList());

        body.put("eventosAuditoria12Meses", auditLogService.eventosRecentesExport(contaId, desde));
        body.put("observacao", "Exportacao LGPD — dados pessoais sem senhas ou certificados.");
        return body;
    }

    private Map<String, Object> usuarioSemSenha(Usuario u) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", u.getId());
        m.put("nome", u.getNome());
        m.put("email", u.getEmail());
        m.put("cpf", u.getCpf());
        m.put("ativo", u.isAtivo());
        m.put("perfil", u.getPerfil());
        return m;
    }

    private Map<String, Object> empresaResumo(Empresa e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("nome", e.getNome());
        m.put("cnpj", e.getCnpj());
        m.put("email", e.getEmail());
        m.put("municipio", e.getMunicipio());
        m.put("uf", e.getUf());
        m.put("ativo", e.isAtivo());
        return m;
    }
}
