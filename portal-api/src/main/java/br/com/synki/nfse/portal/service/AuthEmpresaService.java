package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.repository.ConfiguracaoNfseRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedTokenService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoNfseRepository configuracaoNfseRepository;
    private final EmbedTokenService tokenService;
    private final NfseLibService nfseLibService;
    private final MembershipService membershipService;
    private final RefreshTokenService refreshTokenService;

    public AuthEmpresaService(
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracaoNfseRepository configuracaoNfseRepository,
            EmbedTokenService tokenService,
            NfseLibService nfseLibService,
            MembershipService membershipService,
            RefreshTokenService refreshTokenService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracaoNfseRepository = configuracaoNfseRepository;
        this.tokenService = tokenService;
        this.nfseLibService = nfseLibService;
        this.membershipService = membershipService;
        this.refreshTokenService = refreshTokenService;
    }

    public List<Map<String, Object>> listarEmpresas(Long usuarioId, Long empresaAtualId, String termo, int limite) {
        membershipService.requireAccess(usuarioId, empresaAtualId);
        return membershipService.listarEmpresasPermitidas(usuarioId, termo, limite).stream()
                .map(e -> resumoEmpresa(e, e.getId().equals(empresaAtualId)))
                .toList();
    }

    public Map<String, Object> trocarEmpresa(Long usuarioId, Long empresaId) {
        membershipService.requireAccess(usuarioId, empresaId);
        var empresa = empresaRepository.findById(empresaId)
                .filter(Empresa::isAtivo)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada ou inativa"));
        var user = usuarioRepository.findById(usuarioId)
                .filter(u -> u.isAtivo())
                .orElseThrow(() -> new IllegalArgumentException("Usuario inativo ou nao encontrado"));
        if (!empresaId.equals(user.getEmpresaId())) {
            user.setEmpresaId(empresaId);
            usuarioRepository.save(user);
        }
        return AuthSessionBuilder.of(user, empresa.getId(), tokenService, refreshTokenService, empresaRepository)
                .papel(membershipService.papelAtivo(usuarioId, empresaId))
                .contaId(membershipService.contaIdDaEmpresa(empresaId))
                .build();
    }

    private Map<String, Object> resumoEmpresa(Empresa empresa, boolean atual) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", empresa.getId());
        item.put("nome", empresa.getNome());
        item.put("cnpj", empresa.getCnpj());
        item.put("atual", atual);
        configuracaoNfseRepository.findByEmpresaId(empresa.getId()).ifPresent(cfg -> {
            item.put("ambiente", cfg.getAmbiente());
            item.put("prefeitura", cfg.getPrefeitura());
        });
        item.put("certificadoCadastrado", nfseLibService.temCertificado(empresa.getId()));
        return item;
    }
}
