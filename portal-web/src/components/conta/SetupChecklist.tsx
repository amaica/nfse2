"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { CheckCircle2, Circle, Loader2 } from "lucide-react";
import { fiscalApi } from "@/lib/fiscal-api";
import { useAppSession } from "@/hooks/useAppSession";
import { menuAllowsHref } from "@/lib/menu/tree";
import { useMenus } from "@/lib/menu/useMenus";

type Passo = {
  id: string;
  titulo: string;
  concluido: boolean;
  href: string;
};

type SetupData = {
  passos: Passo[];
  concluidos: number;
  total: number;
  completo: boolean;
  percentual: number;
};

export function SetupChecklist() {
  const { session, ready } = useAppSession();
  const { menuTree } = useMenus();
  const [data, setData] = useState<SetupData | null>(null);
  const [loading, setLoading] = useState(true);

  const carregar = useCallback(async () => {
    if (!session?.empresaId) {
      setData(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const res = await fiscalApi.request<SetupData>("/api/conta/setup");
      setData(res);
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [session?.empresaId]);

  useEffect(() => {
    if (!ready) return;
    void carregar();
  }, [ready, carregar]);

  const passosVisiveis = useMemo(() => {
    if (!data) return [];
    return data.passos.filter((p) => menuAllowsHref(menuTree, p.href));
  }, [data, menuTree]);

  if (!ready || !session?.empresaId) {
    return null;
  }

  if (loading) {
    return (
      <div className="flex items-center gap-2 py-4 text-sm text-agro-muted">
        <Loader2 className="h-4 w-4 animate-spin" />
        Carregando checklist…
      </div>
    );
  }

  if (!data || data.completo || passosVisiveis.length === 0) {
    return null;
  }

  const concluidosVisiveis = passosVisiveis.filter((p) => p.concluido).length;
  const percentualVisivel = Math.round((concluidosVisiveis / passosVisiveis.length) * 100);

  return (
    <section className="mb-10">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-agro-muted">
            Configure em minutos
          </h2>
          <p className="mt-1 text-sm text-agro-muted">
            {concluidosVisiveis} de {passosVisiveis.length} concluídos
          </p>
        </div>
        <div className="h-2 w-32 overflow-hidden rounded-full bg-gray-200">
          <div
            className="h-full rounded-full bg-[var(--primary-600)] transition-all"
            style={{ width: `${percentualVisivel}%` }}
          />
        </div>
      </div>
      <div className="grid gap-2">
        {passosVisiveis.map((passo) => (
          <Link
            key={passo.id}
            href={passo.href}
            className={`saas-dashboard-card flex items-center gap-3 !py-3 transition ${
              passo.concluido ? "opacity-70" : "hover:border-[var(--primary-400)]"
            }`}
          >
            {passo.concluido ? (
              <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
            ) : (
              <Circle className="h-5 w-5 shrink-0 text-[var(--primary-500)]" />
            )}
            <span
              className={`text-sm font-medium ${
                passo.concluido ? "text-agro-muted line-through" : "text-agro-body"
              }`}
            >
              {passo.titulo}
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
