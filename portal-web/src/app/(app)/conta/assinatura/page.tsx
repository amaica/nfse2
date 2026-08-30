"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CreditCard, Loader2, Sparkles, AlertTriangle, CheckCircle2 } from "lucide-react";
import { fiscalApi } from "@/lib/fiscal-api";
import type { AssinaturaStatus } from "@/lib/assinatura";
import { PageCard, PageHeader, StatusBadge } from "@/components/ui/PageCard";

const STATUS_LABEL: Record<string, string> = {
  trial: "Trial gratuito",
  pendente: "Pagamento pendente",
  ativa: "Plano ativo",
  vencida: "Pagamento em atraso",
  cancelada: "Cancelada",
};

const STATUS_TONE: Record<string, "success" | "warn" | "danger" | "neutral"> = {
  trial: "neutral",
  pendente: "warn",
  ativa: "success",
  vencida: "danger",
  cancelada: "neutral",
};

function pct(usado: number, total: number) {
  if (!total) return 0;
  return Math.min(100, Math.round((usado / total) * 100));
}

function barClass(p: number) {
  if (p >= 100) return "quota-bar__fill--danger";
  if (p >= 80) return "quota-bar__fill--warn";
  return "quota-bar__fill--ok";
}

function QuotaBar({ label, usado, total }: { label: string; usado: number; total: number }) {
  const p = pct(usado, total);
  return (
    <div>
      <div className="mb-1.5 flex justify-between text-sm">
        <span className="text-gray-700">{label}</span>
        <span className="font-medium text-gray-500">
          {usado} / {total}
        </span>
      </div>
      <div className="quota-bar">
        <div className={`quota-bar__fill ${barClass(p)}`} style={{ width: `${p}%` }} />
      </div>
    </div>
  );
}

function StatusHero({ data }: { data: AssinaturaStatus }) {
  if (data.podeEmitir === false) {
    return (
      <div className="mb-6 flex gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900">
        <AlertTriangle className="h-5 w-5 shrink-0" />
        <div>
          <p className="font-semibold">Emissão bloqueada</p>
          <p className="mt-1">{data.mensagemStatus}</p>
        </div>
      </div>
    );
  }
  if (data.status === "trial") {
    return (
      <div className="mb-6 flex gap-3 rounded-xl border border-sky-200 bg-sky-50 p-4 text-sm text-sky-900">
        <Sparkles className="h-5 w-5 shrink-0" />
        <div>
          <p className="font-semibold">Período de teste</p>
          <p className="mt-1">{data.mensagemStatus}</p>
          {data.diasTrialRestantes != null && data.diasTrialRestantes <= 3 && (
            <p className="mt-2 font-medium">Assine antes do fim do trial para não interromper emissões.</p>
          )}
        </div>
      </div>
    );
  }
  if (data.status === "ativa") {
    return (
      <div className="mb-6 flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
        <CheckCircle2 className="h-5 w-5 shrink-0" />
        <p>{data.mensagemStatus}</p>
      </div>
    );
  }
  if (data.status === "vencida") {
    return (
      <div className="mb-6 flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        <AlertTriangle className="h-5 w-5 shrink-0" />
        <p>{data.mensagemStatus}</p>
      </div>
    );
  }
  return null;
}

export default function AssinaturaPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center gap-2 py-20 text-gray-500">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando assinatura…
      </div>
    }>
      <AssinaturaPageContent />
    </Suspense>
  );
}

function AssinaturaPageContent() {
  const searchParams = useSearchParams();
  const [checkoutOk, setCheckoutOk] = useState(false);
  const [data, setData] = useState<AssinaturaStatus | null>(null);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);
  const [acaoLoading, setAcaoLoading] = useState(false);
  const [pacotes, setPacotes] = useState(1);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fiscalApi.request<AssinaturaStatus>("/api/conta/assinatura");
      setData(res);
      setPacotes(Math.max(1, res.pacotes || 1));
      setErro("");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar assinatura");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  useEffect(() => {
    if (searchParams.get("checkout") === "success") {
      setCheckoutOk(true);
      void carregar();
    }
  }, [searchParams, carregar]);

  async function acaoPrincipal() {
    if (!data) return;
    setAcaoLoading(true);
    setErro("");
    try {
      const gestionar = ["ativa", "vencida"].includes(data.status);
      const path = gestionar ? "/api/conta/billing/portal" : "/api/conta/billing/checkout";
      const body = gestionar ? undefined : JSON.stringify({ pacotes });
      const res = await fiscalApi.request<{ url: string }>(path, { method: "POST", body });
      window.open(res.url, "_blank");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao abrir pagamento");
    } finally {
      setAcaoLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-gray-500">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando assinatura…
      </div>
    );
  }

  if (!data) {
    return <p className="py-10 text-center text-red-600">{erro || "Sem dados"}</p>;
  }

  const labelBotao =
    data.status === "ativa" || data.status === "vencida"
      ? "Gerenciar pagamento"
      : data.status === "trial"
        ? "Assinar e continuar depois do trial"
        : "Assinar plano";

  return (
    <div className="animate-in mx-auto max-w-2xl">
      <PageHeader
        eyebrow="Conta"
        title="Assinatura"
        subtitle="Trial, planos e limites de emissão do SyncNota"
      />

      <StatusHero data={data} />

      {checkoutOk && (
        <div className="mb-6 flex gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
          <CheckCircle2 className="h-5 w-5 shrink-0" />
          <p>
            Pagamento recebido! Seu plano será ativado em instantes. Atualize esta página se o status
            ainda não mudou.
          </p>
        </div>
      )}

      <PageCard
        title="Plano atual"
        subtitle={`${data.pacotes} pacote(s) · Starter`}
        icon={<CreditCard className="h-4 w-4" />}
        badge={
          <StatusBadge
            label={STATUS_LABEL[data.status] ?? data.status}
            tone={STATUS_TONE[data.status] ?? "neutral"}
          />
        }
        footer={
          data.stripeHabilitado ? (
            <button
              type="button"
              className="fiscal-btn-primary w-full sm:w-auto"
              onClick={() => void acaoPrincipal()}
              disabled={acaoLoading}
            >
              {acaoLoading ? "Abrindo…" : labelBotao}
            </button>
          ) : (
            <p className="text-sm text-gray-600">
              Modo demonstração — cobrança Stripe desativada neste ambiente.
            </p>
          )
        }
      >
        {data.periodoFim && (
          <p className="mb-6 text-sm text-gray-600">
            {["ativa", "trial"].includes(data.status) ? "Válido até" : "Referência"}:{" "}
            <strong className="text-gray-900">
              {new Date(data.periodoFim).toLocaleDateString("pt-BR")}
            </strong>
          </p>
        )}

        <div className="space-y-5">
          <QuotaBar label="Emitentes" usado={data.empresasUsadas} total={data.empresasQuota} />
          <QuotaBar label="Usuários" usado={data.usuariosUsados} total={data.usuariosQuota} />
          <QuotaBar label="NFS-e este mês" usado={data.nfseMesUsadas} total={data.nfseMesQuota} />
          <QuotaBar label="NF-e este mês" usado={data.nfeMesUsadas} total={data.nfeMesQuota} />
        </div>

        {!["ativa", "vencida"].includes(data.status) && data.stripeHabilitado && (
          <div className="mt-6 rounded-lg border border-[var(--border)] bg-[var(--primary-50)] p-4">
            <label className="mb-1 block text-xs font-semibold uppercase text-gray-500">
              Quantidade de pacotes
            </label>
            <p className="mb-2 text-xs text-gray-600">
              Cada pacote: +1 emitente, +100 NFS-e/mês, +50 NF-e/mês, +5 usuários
            </p>
            <input
              type="number"
              min={1}
              max={50}
              value={pacotes}
              onChange={(e) => setPacotes(Math.max(1, Number(e.target.value) || 1))}
              className="fiscal-input max-w-[8rem]"
            />
          </div>
        )}

        {erro && <p className="mt-4 text-sm text-red-600">{erro}</p>}
      </PageCard>

      <p className="mt-6 text-center text-sm text-agro-muted">
        Dúvidas?{" "}
        <Link href="/precos" className="link-agro font-medium">
          Comparar planos
        </Link>
      </p>
    </div>
  );
}
