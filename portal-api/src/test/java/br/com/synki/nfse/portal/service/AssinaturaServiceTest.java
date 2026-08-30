package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.FluxoImportProperties;
import br.com.synki.nfse.portal.config.StripeProperties;
import br.com.synki.nfse.portal.domain.Assinatura;
import br.com.synki.nfse.portal.domain.UsoMensal;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaEmpresaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.UsoMensalRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Cobre as regras de bloqueio de emissao por cota mensal e status de assinatura
 * (nucleo do modelo de cobranca do SaaS) que nao tinham nenhum teste antes.
 */
@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    private static final Long EMPRESA_ID = 10L;
    private static final Long CONTA_ID = 100L;
    private static final String MES_ATUAL = YearMonth.now(ZoneId.of("America/Sao_Paulo")).toString();

    @Mock private AssinaturaRepository assinaturaRepository;
    @Mock private UsoMensalRepository usoMensalRepository;
    @Mock private ContaRepository contaRepository;
    @Mock private ContaEmpresaRepository contaEmpresaRepository;
    @Mock private UsuarioEmpresaRepository usuarioEmpresaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MembershipService membershipService;
    @Mock private BillingDunningService billingDunningService;
    @Mock private PortalMailService portalMailService;

    private AssinaturaService service;

    private StripeProperties stripeHabilitado() {
        return new StripeProperties(true, "test", "sk_test", "price_test", "", "", "", "http://localhost");
    }

    private FluxoImportProperties fluxoProps() {
        return new FluxoImportProperties(
                false, false, "", "", "", 4L, 1L, "x", "admin@synki.demo",
                "", "", "", "");
    }

    private void mockar(StripeProperties props) {
        service = new AssinaturaService(
                props, fluxoProps(), assinaturaRepository, usoMensalRepository, contaRepository,
                contaEmpresaRepository, usuarioEmpresaRepository, usuarioRepository,
                membershipService, billingDunningService, portalMailService);
    }

    @BeforeEach
    void setUp() {
        mockar(stripeHabilitado());
        lenient().when(membershipService.contaIdDaEmpresa(EMPRESA_ID)).thenReturn(CONTA_ID);
    }

    private Assinatura assinaturaAtiva(int nfeQuota) {
        var a = Assinatura.trial(CONTA_ID);
        a.setStatus(Assinatura.STATUS_ATIVA);
        a.setNfeMesQuota(nfeQuota);
        return a;
    }

    private UsoMensal usoComContagem(int nfeCount) {
        var uso = UsoMensal.zerado(CONTA_ID, MES_ATUAL);
        for (int i = 0; i < nfeCount; i++) {
            uso.incrementNfe();
        }
        return uso;
    }

    @Test
    void requireEmissaoNfeNaoBloqueiaComStripeDesabilitado() {
        mockar(new StripeProperties(false, "test", "", "", "", "", "", ""));
        service.requireEmissaoNfe(EMPRESA_ID);
    }

    @Test
    void requireEmissaoNfeNaoBloqueiaSemContaAssociada() {
        when(membershipService.contaIdDaEmpresa(EMPRESA_ID)).thenReturn(null);
        service.requireEmissaoNfe(EMPRESA_ID);
    }

    @Test
    void requireEmissaoNfePermiteQuandoDentroDaCota() {
        when(assinaturaRepository.findByContaId(CONTA_ID)).thenReturn(Optional.of(assinaturaAtiva(10)));
        when(usoMensalRepository.findByContaIdAndAnoMes(CONTA_ID, MES_ATUAL))
                .thenReturn(Optional.of(usoComContagem(5)));

        service.requireEmissaoNfe(EMPRESA_ID);
    }

    @Test
    void requireEmissaoNfeBloqueiaQuandoCotaEsgotada() {
        when(assinaturaRepository.findByContaId(CONTA_ID)).thenReturn(Optional.of(assinaturaAtiva(10)));
        when(usoMensalRepository.findByContaIdAndAnoMes(CONTA_ID, MES_ATUAL))
                .thenReturn(Optional.of(usoComContagem(10)));

        assertThatThrownBy(() -> service.requireEmissaoNfe(EMPRESA_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cota mensal de NF-e esgotada");
    }

    @Test
    void requireEmissaoNfeBloqueiaAssinaturaVencida() {
        var vencida = assinaturaAtiva(10);
        vencida.setStatus(Assinatura.STATUS_VENCIDA);
        when(assinaturaRepository.findByContaId(CONTA_ID)).thenReturn(Optional.of(vencida));

        assertThatThrownBy(() -> service.requireEmissaoNfe(EMPRESA_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireEmissaoNfeBloqueiaAssinaturaCancelada() {
        var cancelada = assinaturaAtiva(10);
        cancelada.setStatus(Assinatura.STATUS_CANCELADA);
        when(assinaturaRepository.findByContaId(CONTA_ID)).thenReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> service.requireEmissaoNfe(EMPRESA_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireEmissaoNfeBloqueiaTrialExpiradoEDegradaStatusParaPendente() {
        var trialExpirado = Assinatura.trial(CONTA_ID);
        trialExpirado.setPeriodoFim(Instant.now().minusSeconds(3600));
        when(assinaturaRepository.findByContaId(CONTA_ID)).thenReturn(Optional.of(trialExpirado));
        when(assinaturaRepository.save(any())).thenReturn(trialExpirado);

        assertThatThrownBy(() -> service.requireEmissaoNfe(EMPRESA_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("trial encerrado");

        assertThat(trialExpirado.getStatus()).isEqualTo(Assinatura.STATUS_PENDENTE);
    }

    @Test
    void requireEmissaoNfsePermiteQuandoDentroDaCota() {
        var assinatura = Assinatura.trial(CONTA_ID);
        assinatura.setStatus(Assinatura.STATUS_ATIVA);
        assinatura.setNfseMesQuota(10);
        when(assinaturaRepository.findByContaId(CONTA_ID)).thenReturn(Optional.of(assinatura));
        when(usoMensalRepository.findByContaIdAndAnoMes(CONTA_ID, MES_ATUAL))
                .thenReturn(Optional.of(UsoMensal.zerado(CONTA_ID, MES_ATUAL)));

        service.requireEmissaoNfse(EMPRESA_ID);
    }

    @Test
    void registrarNfeEmitidaIncrementaContadorDoMes() {
        var uso = usoComContagem(3);
        when(usoMensalRepository.findByContaIdAndAnoMes(CONTA_ID, MES_ATUAL)).thenReturn(Optional.of(uso));
        when(usoMensalRepository.save(uso)).thenReturn(uso);

        service.registrarNfeEmitida(EMPRESA_ID);

        assertThat(uso.getNfeCount()).isEqualTo(4);
    }

    @Test
    void registrarNfeEmitidaNaoFalhaSemContaAssociada() {
        when(membershipService.contaIdDaEmpresa(EMPRESA_ID)).thenReturn(null);
        service.registrarNfeEmitida(EMPRESA_ID);
    }
}
