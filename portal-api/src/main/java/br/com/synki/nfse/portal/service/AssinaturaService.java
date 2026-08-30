package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.FluxoImportProperties;
import br.com.synki.nfse.portal.config.StripeProperties;
import br.com.synki.nfse.portal.domain.Assinatura;
import br.com.synki.nfse.portal.domain.Conta;
import br.com.synki.nfse.portal.domain.UsoMensal;
import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.repository.AssinaturaRepository;
import br.com.synki.nfse.portal.repository.ContaEmpresaRepository;
import br.com.synki.nfse.portal.repository.ContaRepository;
import br.com.synki.nfse.portal.repository.UsoMensalRepository;
import br.com.synki.nfse.portal.repository.UsuarioEmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.security.EmbedSession;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaService.class);

    private static final Set<String> STATUS_EMISSAO = Set.of(
            Assinatura.STATUS_TRIAL,
            Assinatura.STATUS_ATIVA);

    private static final Set<String> STATUS_CHECKOUT = Set.of(
            Assinatura.STATUS_TRIAL,
            Assinatura.STATUS_PENDENTE,
            Assinatura.STATUS_CANCELADA);

    private static final ZoneId TZ = ZoneId.of("America/Sao_Paulo");

    private final StripeProperties stripeProperties;
    private final FluxoImportProperties fluxoImportProperties;
    private final AssinaturaRepository assinaturaRepository;
    private final UsoMensalRepository usoMensalRepository;
    private final ContaRepository contaRepository;
    private final ContaEmpresaRepository contaEmpresaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembershipService membershipService;
    private final BillingDunningService billingDunningService;
    private final PortalMailService portalMailService;

    public AssinaturaService(
            StripeProperties stripeProperties,
            FluxoImportProperties fluxoImportProperties,
            AssinaturaRepository assinaturaRepository,
            UsoMensalRepository usoMensalRepository,
            ContaRepository contaRepository,
            ContaEmpresaRepository contaEmpresaRepository,
            UsuarioEmpresaRepository usuarioEmpresaRepository,
            UsuarioRepository usuarioRepository,
            MembershipService membershipService,
            BillingDunningService billingDunningService,
            PortalMailService portalMailService) {
        this.stripeProperties = stripeProperties;
        this.fluxoImportProperties = fluxoImportProperties;
        this.assinaturaRepository = assinaturaRepository;
        this.usoMensalRepository = usoMensalRepository;
        this.contaRepository = contaRepository;
        this.contaEmpresaRepository = contaEmpresaRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.membershipService = membershipService;
        this.billingDunningService = billingDunningService;
        this.portalMailService = portalMailService;
    }

    @PostConstruct
    void initStripe() {
        if (stripeProperties.enabled() && stripeProperties.secretKey() != null && !stripeProperties.secretKey().isBlank()) {
            Stripe.apiKey = stripeProperties.secretKey();
        }
    }

    public Map<String, Object> statusConta(Long contaId) {
        return statusConta(contaId, usuarioIdAtual().orElse(null));
    }

    public Map<String, Object> statusConta(Long contaId, Long usuarioId) {
        var assinatura = obterOuCriar(contaId);
        var uso = usoAtual(contaId);
        int empresasUsadas = contaEmpresaRepository.findByContaId(contaId).size();
        int usuariosUsados = (int) usuarioEmpresaRepository.findByContaIdAndAtivoTrueOrderByUsuarioIdAsc(contaId).stream()
                .map(m -> m.getUsuarioId())
                .distinct()
                .count();
        boolean adminPlataforma = isAdminPlataforma(usuarioId);

        var body = new LinkedHashMap<String, Object>();
        body.put("stripeHabilitado", stripeProperties.enabled());
        body.put("status", assinatura.getStatus());
        body.put("pacotes", assinatura.getPacotes());
        body.put("periodoFim", assinatura.getPeriodoFim() != null ? assinatura.getPeriodoFim().toString() : null);
        body.put("empresasQuota", assinatura.getEmpresasQuota());
        body.put("empresasUsadas", empresasUsadas);
        body.put("usuariosQuota", assinatura.getUsuariosQuota());
        body.put("usuariosUsados", usuariosUsados);
        body.put("nfseMesQuota", assinatura.getNfseMesQuota());
        body.put("nfseMesUsadas", uso.getNfseCount());
        body.put("nfeMesQuota", assinatura.getNfeMesQuota());
        body.put("nfeMesUsadas", uso.getNfeCount());
        body.put("podeEmitir", adminPlataforma || calcularPodeEmitir(assinatura));
        body.put("mensagemStatus", adminPlataforma
                ? "Conta administrativa — emissão liberada sem cobrança."
                : mensagemStatusAmigavel(assinatura));
        if (Assinatura.STATUS_TRIAL.equals(assinatura.getStatus()) && assinatura.getPeriodoFim() != null) {
            var dias = ChronoUnit.DAYS.between(Instant.now(), assinatura.getPeriodoFim());
            body.put("diasTrialRestantes", Math.max(0, dias));
        }
        return body;
    }

    private boolean calcularPodeEmitir(Assinatura assinatura) {
        if (!stripeProperties.enabled()) {
            return true;
        }
        if (!STATUS_EMISSAO.contains(assinatura.getStatus())) {
            return false;
        }
        return !(Assinatura.STATUS_TRIAL.equals(assinatura.getStatus())
                && assinatura.getPeriodoFim() != null
                && Instant.now().isAfter(assinatura.getPeriodoFim()));
    }

    private boolean isAdminPlataforma(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        var emailAdmin = fluxoImportProperties.adminPlataformaEmail();
        if (emailAdmin == null || emailAdmin.isBlank()) {
            return false;
        }
        return usuarioRepository.findById(usuarioId)
                .map(u -> emailAdmin.equalsIgnoreCase(u.getEmail()))
                .orElse(false);
    }

    private boolean isAdminPlataformaAtual() {
        return usuarioIdAtual().map(this::isAdminPlataforma).orElse(false);
    }

    private java.util.Optional<Long> usuarioIdAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmbedSession session) {
            return java.util.Optional.ofNullable(session.usuarioId());
        }
        return java.util.Optional.empty();
    }

    private String mensagemStatusAmigavel(Assinatura assinatura) {
        return switch (assinatura.getStatus()) {
            case Assinatura.STATUS_TRIAL -> {
                if (assinatura.getPeriodoFim() != null && Instant.now().isAfter(assinatura.getPeriodoFim())) {
                    yield "Seu trial de 14 dias encerrou. Assine um plano para continuar emitindo.";
                }
                var dias = assinatura.getPeriodoFim() != null
                        ? ChronoUnit.DAYS.between(Instant.now(), assinatura.getPeriodoFim())
                        : 14;
                yield "Trial gratuito — " + Math.max(0, dias) + " dia(s) restantes.";
            }
            case Assinatura.STATUS_ATIVA -> "Plano ativo. Obrigado por assinar o SyncNota.";
            case Assinatura.STATUS_VENCIDA -> "Pagamento em atraso — emissão bloqueada até regularizar.";
            case Assinatura.STATUS_PENDENTE -> "Assinatura pendente. Conclua o pagamento para emitir notas.";
            case Assinatura.STATUS_CANCELADA -> "Assinatura cancelada. Reative em Conta → Assinatura.";
            default -> "Verifique sua assinatura em Conta → Assinatura.";
        };
    }

    public void requireEmissaoNfse(Long empresaId) {
        if (!stripeProperties.enabled() || isAdminPlataformaAtual()) {
            return;
        }
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) {
            return;
        }
        var assinatura = obterOuCriar(contaId);
        validarStatusEmissao(assinatura);
        var uso = usoAtual(contaId);
        if (uso.getNfseCount() >= assinatura.getNfseMesQuota()) {
            throw new AccessDeniedException("Cota mensal de NFS-e esgotada. Aumente seu plano.");
        }
    }

    public void requireEmissaoNfe(Long empresaId) {
        if (!stripeProperties.enabled() || isAdminPlataformaAtual()) {
            return;
        }
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) {
            return;
        }
        var assinatura = obterOuCriar(contaId);
        validarStatusEmissao(assinatura);
        var uso = usoAtual(contaId);
        if (uso.getNfeCount() >= assinatura.getNfeMesQuota()) {
            throw new AccessDeniedException("Cota mensal de NF-e esgotada. Aumente seu plano.");
        }
    }

    public void requireNovaEmpresa(Long contaId) {
        if (!stripeProperties.enabled() || isAdminPlataformaAtual()) {
            return;
        }
        var assinatura = obterOuCriar(contaId);
        validarStatusEmissao(assinatura);
        long usadas = contaEmpresaRepository.findByContaId(contaId).size();
        if (usadas >= assinatura.getEmpresasQuota()) {
            throw new AccessDeniedException("Cota de empresas emitentes esgotada.");
        }
    }

    @Transactional
    public void registrarNfseEmitida(Long empresaId) {
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) return;
        var uso = usoAtual(contaId);
        uso.incrementNfse();
        usoMensalRepository.save(uso);
    }

    @Transactional
    public void registrarNfeEmitida(Long empresaId) {
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        if (contaId == null) return;
        var uso = usoAtual(contaId);
        uso.incrementNfe();
        usoMensalRepository.save(uso);
    }

    @Transactional
    public String criarCheckout(Long gestorUsuarioId, Long empresaId, int pacotes) throws Exception {
        membershipService.requireGestao(gestorUsuarioId, empresaId);
        if (!stripeProperties.enabled()) {
            throw new IllegalStateException("Stripe nao configurado");
        }
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        var conta = contaRepository.findById(contaId).orElseThrow();
        var assinatura = obterOuCriar(contaId);
        if (!STATUS_CHECKOUT.contains(assinatura.getStatus())) {
            throw new IllegalArgumentException("Use o portal do cliente para gerenciar assinatura ativa");
        }
        var owner = usuarioRepository.findById(conta.getOwnerUsuarioId() != null ? conta.getOwnerUsuarioId() : gestorUsuarioId)
                .orElseThrow();
        var customerId = garantirCustomer(conta, owner);
        int qty = Math.max(1, pacotes);

        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(stripeProperties.portalReturnUrl() + "?checkout=success")
                .setCancelUrl(stripeProperties.portalReturnUrl() + "?checkout=cancel")
                .putMetadata("conta_id", contaId.toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(stripeProperties.priceId())
                        .setQuantity((long) qty)
                        .build())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("conta_id", contaId.toString())
                        .build())
                .build();

        var session = com.stripe.model.checkout.Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public String abrirPortal(Long gestorUsuarioId, Long empresaId) throws Exception {
        membershipService.requireGestao(gestorUsuarioId, empresaId);
        if (!stripeProperties.enabled()) {
            throw new IllegalStateException("Stripe nao configurado");
        }
        var contaId = membershipService.contaIdDaEmpresa(empresaId);
        var conta = contaRepository.findById(contaId).orElseThrow();
        var assinatura = obterOuCriar(contaId);
        if (!List.of(Assinatura.STATUS_ATIVA, Assinatura.STATUS_VENCIDA).contains(assinatura.getStatus())) {
            throw new IllegalArgumentException("Portal disponivel apenas para assinaturas ativas ou vencidas");
        }
        if (conta.getStripeCustomerId() == null || conta.getStripeCustomerId().isBlank()) {
            throw new IllegalArgumentException("Nenhum customer Stripe associado");
        }

        var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(conta.getStripeCustomerId())
                .setReturnUrl(stripeProperties.portalReturnUrl())
                .build();
        var session = com.stripe.model.billingportal.Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public void processarWebhook(String payload, String signatureHeader) {
        if (!stripeProperties.enabled()) {
            return;
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.webhookSecret());
        } catch (Exception e) {
            throw new IllegalArgumentException("Webhook Stripe invalido: " + e.getMessage());
        }

        switch (event.getType()) {
            case "invoice.payment_succeeded" -> onInvoicePaid(event);
            case "customer.subscription.updated" -> onSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> onSubscriptionDeleted(event);
            case "invoice.payment_failed" -> onPaymentFailed(event);
            default -> { /* ignorado */ }
        }
    }

    @Transactional
    public Assinatura provisionarParaConta(Long contaId) {
        return assinaturaRepository.findByContaId(contaId).orElseGet(() ->
                assinaturaRepository.save(Assinatura.trial(contaId)));
    }

    private Assinatura obterOuCriar(Long contaId) {
        return assinaturaRepository.findByContaId(contaId).orElseGet(() ->
                assinaturaRepository.save(Assinatura.trial(contaId)));
    }

    private UsoMensal usoAtual(Long contaId) {
        var mes = YearMonth.now(TZ).toString();
        return usoMensalRepository.findByContaIdAndAnoMes(contaId, mes)
                .orElseGet(() -> usoMensalRepository.save(UsoMensal.zerado(contaId, mes)));
    }

    @Scheduled(cron = "0 0 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void jobExpirarTrials() {
        if (!stripeProperties.enabled()) {
            return;
        }
        var agora = Instant.now();
        for (var assinatura : assinaturaRepository.findByStatus(Assinatura.STATUS_TRIAL)) {
            if (assinatura.getPeriodoFim() == null || !agora.isAfter(assinatura.getPeriodoFim())) {
                continue;
            }
            assinatura.setStatus(Assinatura.STATUS_PENDENTE);
            assinaturaRepository.save(assinatura);
            ownerDaConta(assinatura.getContaId()).ifPresent(portalMailService::enviarTrialEncerrado);
        }
    }

    private java.util.Optional<Usuario> ownerDaConta(Long contaId) {
        return contaRepository.findById(contaId)
                .map(Conta::getOwnerUsuarioId)
                .flatMap(usuarioRepository::findById);
    }

    private void validarStatusEmissao(Assinatura assinatura) {
        if (!STATUS_EMISSAO.contains(assinatura.getStatus())) {
            throw new AccessDeniedException("Assinatura " + assinatura.getStatus() + " — emissao bloqueada");
        }
        if (Assinatura.STATUS_TRIAL.equals(assinatura.getStatus())
                && assinatura.getPeriodoFim() != null
                && Instant.now().isAfter(assinatura.getPeriodoFim())) {
            assinatura.setStatus(Assinatura.STATUS_PENDENTE);
            assinaturaRepository.save(assinatura);
            throw new AccessDeniedException("Periodo de trial encerrado — assine um plano");
        }
    }

    private String garantirCustomer(Conta conta, Usuario owner) throws Exception {
        if (conta.getStripeCustomerId() != null && !conta.getStripeCustomerId().isBlank()) {
            return conta.getStripeCustomerId();
        }
        var customer = com.stripe.model.Customer.create(CustomerCreateParams.builder()
                .setEmail(owner.getEmail())
                .setName(owner.getNome())
                .putMetadata("conta_id", conta.getId().toString())
                .build());
        conta.setStripeCustomerId(customer.getId());
        contaRepository.save(conta);
        return customer.getId();
    }

    private void onInvoicePaid(Event event) {
        Invoice invoice = carregarInvoiceDoEvento(event);
        if (invoice == null) {
            log.warn("Webhook invoice.payment_succeeded: invoice nao resolvido");
            return;
        }
        var subscriptionId = subscriptionIdFromInvoice(invoice);
        int pacotes = 1;
        if (invoice.getLines() != null && invoice.getLines().getData() != null) {
            pacotes = invoice.getLines().getData().stream()
                    .findFirst()
                    .map(line -> line.getQuantity() != null ? line.getQuantity().intValue() : 1)
                    .orElse(1);
        }
        atualizarDeSubscription(subscriptionId, pacotes, periodEnd(invoice));
    }

    private void onSubscriptionUpdated(Event event) {
        Subscription sub = carregarSubscriptionDoEvento(event);
        if (sub == null) {
            log.warn("Webhook customer.subscription.updated: subscription nao resolvida");
            return;
        }
        int qty = sub.getItems().getData().stream()
                .findFirst()
                .map(item -> item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                .orElse(1);
        atualizarDeSubscription(sub.getId(), qty, Instant.ofEpochSecond(sub.getCurrentPeriodEnd()));
    }

    private void onSubscriptionDeleted(Event event) {
        Subscription sub = carregarSubscriptionDoEvento(event);
        if (sub == null) {
            log.warn("Webhook customer.subscription.deleted: subscription nao resolvida");
            return;
        }
        var contaId = parseContaId(sub.getMetadata().get("conta_id"));
        if (contaId == null) return;
        var assinatura = obterOuCriar(contaId);
        assinatura.setStatus(Assinatura.STATUS_CANCELADA);
        assinaturaRepository.save(assinatura);
    }

    private void onPaymentFailed(Event event) {
        Invoice invoice = carregarInvoiceDoEvento(event);
        if (invoice == null) {
            log.warn("Webhook invoice.payment_failed: invoice nao resolvido");
            return;
        }
        var contaId = contaIdFromSubscription(subscriptionIdFromInvoice(invoice));
        if (contaId == null) return;
        var assinatura = obterOuCriar(contaId);
        assinatura.setStatus(Assinatura.STATUS_PENDENTE);
        assinaturaRepository.save(assinatura);
        billingDunningService.avisarPagamentoFalhou(contaId, invoice.getId());
    }

    private void atualizarDeSubscription(String subscriptionId, int pacotes, Instant periodoFim) {
        if (subscriptionId == null) return;
        try {
            Subscription sub = Subscription.retrieve(subscriptionId);
            var contaId = parseContaId(sub.getMetadata().get("conta_id"));
            if (contaId == null) {
                contaId = contaIdFromCustomer(sub.getCustomer());
            }
            if (contaId == null) return;
            var assinatura = obterOuCriar(contaId);
            var eraAtiva = Assinatura.STATUS_ATIVA.equals(assinatura.getStatus());
            assinatura.setStripeSubscriptionId(sub.getId());
            assinatura.setStatus(Assinatura.STATUS_ATIVA);
            assinatura.aplicarPacotes(pacotes);
            assinatura.setPeriodoFim(periodoFim);
            assinatura.setUpdatedAt(Instant.now());
            assinaturaRepository.save(assinatura);
            if (!eraAtiva) {
                ownerDaConta(contaId).ifPresent(portalMailService::enviarAssinaturaAtiva);
            }
            log.info("Assinatura ativada conta={} pacotes={} sub={}", contaId, pacotes, sub.getId());
        } catch (Exception ex) {
            log.warn("Falha ao atualizar assinatura via webhook sub={}: {}", subscriptionId, ex.getMessage());
        }
    }

    private Invoice carregarInvoiceDoEvento(Event event) {
        var invoice = eventObject(event, Invoice.class);
        if (invoice != null && invoice.getId() != null) {
            try {
                return Invoice.retrieve(invoice.getId());
            } catch (Exception ex) {
                log.warn("Falha ao recarregar invoice {}: {}", invoice.getId(), ex.getMessage());
                return invoice;
            }
        }
        var invoiceId = extrairIdDoEvento(event, "in_");
        if (invoiceId == null) {
            return null;
        }
        try {
            return Invoice.retrieve(invoiceId);
        } catch (Exception ex) {
            log.warn("Falha ao buscar invoice {}: {}", invoiceId, ex.getMessage());
            return null;
        }
    }

    private Subscription carregarSubscriptionDoEvento(Event event) {
        var sub = eventObject(event, Subscription.class);
        if (sub != null && sub.getId() != null) {
            try {
                return Subscription.retrieve(sub.getId());
            } catch (Exception ex) {
                log.warn("Falha ao recarregar subscription {}: {}", sub.getId(), ex.getMessage());
                return sub;
            }
        }
        var subId = extrairIdDoEvento(event, "sub_");
        if (subId == null) {
            return null;
        }
        try {
            return Subscription.retrieve(subId);
        } catch (Exception ex) {
            log.warn("Falha ao buscar subscription {}: {}", subId, ex.getMessage());
            return null;
        }
    }

    private static String extrairIdDoEvento(Event event, String prefix) {
        var raw = event.getDataObjectDeserializer().getRawJson();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        var matcher = Pattern.compile("\"" + Pattern.quote(prefix) + "[A-Za-z0-9]+\"").matcher(raw);
        if (matcher.find()) {
            return matcher.group().replace("\"", "");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends StripeObject> T eventObject(Event event, Class<T> type) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return type.cast(deserializer.getObject().get());
        }
        var raw = event.getData().getObject();
        if (raw != null && type.isInstance(raw)) {
            return type.cast(raw);
        }
        return null;
    }

    private static String subscriptionIdFromInvoice(Invoice invoice) {
        if (invoice.getSubscription() != null && !invoice.getSubscription().isBlank()) {
            return invoice.getSubscription();
        }
        if (invoice.getLines() != null && invoice.getLines().getData() != null) {
            for (var line : invoice.getLines().getData()) {
                if (line.getSubscription() != null && !line.getSubscription().isBlank()) {
                    return line.getSubscription();
                }
            }
        }
        return null;
    }

    private Long contaIdFromSubscription(String subscriptionId) {
        if (subscriptionId == null) return null;
        try {
            Subscription sub = Subscription.retrieve(subscriptionId);
            return parseContaId(sub.getMetadata().get("conta_id"));
        } catch (Exception e) {
            return null;
        }
    }

    private Long contaIdFromCustomer(String customerId) {
        if (customerId == null) return null;
        return contaRepository.findByStripeCustomerId(customerId)
                .map(Conta::getId)
                .orElse(null);
    }

    private static Long parseContaId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant periodEnd(Invoice invoice) {
        if (invoice.getLines() != null && !invoice.getLines().getData().isEmpty()) {
            var period = invoice.getLines().getData().getFirst().getPeriod();
            if (period != null && period.getEnd() != null) {
                return Instant.ofEpochSecond(period.getEnd());
            }
        }
        return Instant.now().plusSeconds(30L * 86400L);
    }
}
