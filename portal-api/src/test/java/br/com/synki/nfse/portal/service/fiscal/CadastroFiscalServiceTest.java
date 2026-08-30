package br.com.synki.nfse.portal.service.fiscal;

import br.com.synki.nfse.portal.domain.fiscal.Cfop;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.repository.fiscal.CfopRepository;
import br.com.synki.nfse.portal.repository.fiscal.NcmRepository;
import br.com.synki.nfse.portal.repository.fiscal.PessoaRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import br.com.synki.nfse.portal.repository.fiscal.VeiculoRepository;
import br.com.synki.nfse.portal.service.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre a regressao do bug de isolamento de tenant no CFOP: antes desta correcao,
 * atualizarCfop/excluirCfop buscavam por id sem checar empresaId, permitindo que
 * um usuario de uma empresa alterasse/apagasse CFOP de outra.
 */
@ExtendWith(MockitoExtension.class)
class CadastroFiscalServiceTest {

    private static final Long EMPRESA_A = 1L;
    private static final Long EMPRESA_B = 2L;
    private static final Long CFOP_ID = 99L;

    @Mock private CfopRepository cfopRepo;
    @Mock private NcmRepository ncmRepo;
    @Mock private PessoaRepository pessoaRepo;
    @Mock private ProdutoRepository produtoRepo;
    @Mock private VeiculoRepository veiculoRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MembershipService membershipService;

    private CadastroFiscalService service;

    @BeforeEach
    void setUp() {
        service = new CadastroFiscalService(
                cfopRepo, ncmRepo, pessoaRepo, produtoRepo, veiculoRepo,
                usuarioRepo, passwordEncoder, membershipService);
    }

    @Test
    void atualizarCfopDeOutraEmpresaLancaNaoEncontrado() {
        when(cfopRepo.findByIdAndEmpresaId(CFOP_ID, EMPRESA_B)).thenReturn(Optional.empty());

        var body = new Cfop();
        body.setCfop("5102");
        body.setDescricao("Venda");

        assertThatThrownBy(() -> service.atualizarCfop(EMPRESA_B, CFOP_ID, body))
                .isInstanceOf(NoSuchElementException.class);

        verify(cfopRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void excluirCfopDeOutraEmpresaLancaNaoEncontrado() {
        when(cfopRepo.findByIdAndEmpresaId(CFOP_ID, EMPRESA_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluirCfop(EMPRESA_B, CFOP_ID))
                .isInstanceOf(NoSuchElementException.class);

        verify(cfopRepo, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void atualizarCfopDaPropriaEmpresaFunciona() {
        var existente = new Cfop();
        existente.setEmpresaId(EMPRESA_A);
        existente.setCfop("5101");
        existente.setDescricao("Antiga descricao");
        when(cfopRepo.findByIdAndEmpresaId(CFOP_ID, EMPRESA_A)).thenReturn(Optional.of(existente));
        when(cfopRepo.save(existente)).thenReturn(existente);

        var body = new Cfop();
        body.setCfop("5102");
        body.setDescricao("Nova descricao");

        var atualizado = service.atualizarCfop(EMPRESA_A, CFOP_ID, body);

        assertThat(atualizado.getCfop()).isEqualTo("5102");
        assertThat(atualizado.getDescricao()).isEqualTo("Nova descricao");
    }

    @Test
    void salvarCfopAtribuiEmpresaDoTenantAtual() {
        var body = new Cfop();
        body.setCfop("5405");
        when(cfopRepo.save(body)).thenReturn(body);

        service.salvarCfop(EMPRESA_A, body);

        assertThat(body.getEmpresaId()).isEqualTo(EMPRESA_A);
    }
}
