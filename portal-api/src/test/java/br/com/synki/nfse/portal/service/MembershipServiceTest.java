package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaEmpresaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long EMPRESA_ID = 10L;
    private static final Long CONTA_ID = 100L;

    @Mock private UsuarioEmpresaRepository usuarioEmpresaRepository;
    @Mock private ContaRepository contaRepository;
    @Mock private ContaEmpresaRepository contaEmpresaRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private AssinaturaRepository assinaturaRepository;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(
                usuarioEmpresaRepository, contaRepository, contaEmpresaRepository,
                empresaRepository, assinaturaRepository);
    }

    private void membershipCom(String papel) {
        var membership = UsuarioEmpresa.vincular(USUARIO_ID, EMPRESA_ID, CONTA_ID, papel);
        when(usuarioEmpresaRepository.findByUsuarioIdAndEmpresaIdAndAtivoTrue(USUARIO_ID, EMPRESA_ID))
                .thenReturn(Optional.of(membership));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN"})
    void requireGestaoPermiteOwnerEAdmin(String papel) {
        membershipCom(papel);
        service.requireGestao(USUARIO_ID, EMPRESA_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERADOR", "VISUALIZADOR"})
    void requireGestaoBloqueiaPapeisSemGestao(String papel) {
        membershipCom(papel);
        assertThatThrownBy(() -> service.requireGestao(USUARIO_ID, EMPRESA_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN", "OPERADOR"})
    void requireOperadorPermiteQuemPodeEscrever(String papel) {
        membershipCom(papel);
        service.requireOperador(USUARIO_ID, EMPRESA_ID);
    }

    @Test
    void requireOperadorBloqueiaVisualizador() {
        membershipCom("VISUALIZADOR");
        assertThatThrownBy(() -> service.requireOperador(USUARIO_ID, EMPRESA_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void requireOperadorBloqueiaUsuarioSemVinculoAtivo() {
        when(usuarioEmpresaRepository.findByUsuarioIdAndEmpresaIdAndAtivoTrue(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireOperador(USUARIO_ID, EMPRESA_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void hasAccessRetornaFalseSemIdentificadores() {
        assertThat(service.hasAccess(null, EMPRESA_ID)).isFalse();
        assertThat(service.hasAccess(USUARIO_ID, null)).isFalse();
    }
}
