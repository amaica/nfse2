"use client";

import { useCallback, useEffect, useState } from "react";
import { BarChart3, Loader2 } from "lucide-react";
import { fiscalApi } from "@/lib/fiscal-api";
import { PageCard, PageHeader } from "@/components/ui/PageCard";

type HistoricoMes = { anoMes: string; nfse: number; nfe: number };

type MetricasPainel = {
  status: string;
  nfseMesUsadas: number;
  nfseMesQuota: number;
  nfeMesUsadas: number;
  nfeMesQuota: number;
  eventosAudit30Dias: number;
  historicoMensal: HistoricoMes[];
};

function maxValor(historico: HistoricoMes[]) {
  return Math.max(1, ...historico.flatMap((h) => [h.nfse, h.nfe]));
}

export default function MetricasPage() {
  const [data, setData] = useState<MetricasPainel | null>(null);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fiscalApi.request<MetricasPainel>("/api/conta/metricas");
      setData(res);
      setErro("");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar métricas");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  if (loading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-gray-500">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando métricas…
      </div>
    );
  }

  if (!data) {
    return <p className="py-10 text-center text-red-600">{erro || "Sem dados"}</p>;
  }

  const max = maxValor(data.historicoMensal ?? []);

  return (
    <div className="animate-in mx-auto max-w-3xl">
      <PageHeader
        eyebrow="Conta"
        title="Métricas de uso"
        subtitle="Histórico mensal e atividade da conta"
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        {[
          { v: data.nfseMesUsadas, l: "NFS-e este mês" },
          { v: data.nfeMesUsadas, l: "NF-e este mês" },
          { v: data.eventosAudit30Dias, l: "Eventos (30 dias)" },
        ].map((s) => (
          <div key={s.l} className="page-card p-4 text-center">
            <p className="text-2xl font-bold text-[var(--primary-600)]">{s.v}</p>
            <p className="text-xs text-gray-500">{s.l}</p>
          </div>
        ))}
      </div>

      <PageCard
        title="Emissões por mês"
        subtitle={`Plano: ${data.status}`}
        icon={<BarChart3 className="h-4 w-4" />}
      >
        {(data.historicoMensal ?? []).length === 0 ? (
          <p className="text-sm text-gray-500">Nenhuma emissão registrada ainda.</p>
        ) : (
          <ul className="space-y-4">
            {[...(data.historicoMensal ?? [])].reverse().map((h) => (
              <li key={h.anoMes}>
                <div className="mb-1 flex justify-between text-xs text-gray-500">
                  <span>{h.anoMes}</span>
                  <span>
                    NFS-e {h.nfse} · NF-e {h.nfe}
                  </span>
                </div>
                <div className="flex h-2 gap-1 overflow-hidden rounded-full bg-gray-100">
                  <div
                    className="rounded-full bg-[var(--primary-500)]"
                    style={{ width: `${(h.nfse / max) * 100}%` }}
                  />
                  <div
                    className="rounded-full bg-[var(--accent-gold)]"
                    style={{ width: `${(h.nfe / max) * 100}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
        <p className="mt-4 text-xs text-gray-400">Verde = NFS-e · Dourado = NF-e</p>
      </PageCard>

      {erro && <p className="mt-4 text-sm text-red-600">{erro}</p>}
    </div>
  );
}
