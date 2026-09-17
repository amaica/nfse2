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
      <div className="home-card home-card__pad home-setup flex items-center gap-2 text-sm text-agro-muted">
        <Loader2 className="h-4 w-4 animate-spin" />
        Verificando cadastros e passos pendentes…
      </div>
    );
  }

  if (!data || data.completo || passosVisiveis.length === 0) {
    return null;
  }

  const concluidosVisiveis = passosVisiveis.filter((p) => p.concluido).length;
  const percentualVisivel = Math.round((concluidosVisiveis / passosVisiveis.length) * 100);
  const pendentes = passosVisiveis.filter((p) => !p.concluido);

  return (
    <section className="home-card home-card__pad home-setup" aria-label="Passos pendentes">
      <div className="home-card__head !mb-0">
        <div>
          <h2 className="home-card__title">Configure em minutos</h2>
          <p className="home-card__meta">
            {concluidosVisiveis} de {passosVisiveis.length} concluídos
            {pendentes.length > 0 ? ` · ${pendentes.length} pendente${pendentes.length > 1 ? "s" : ""}` : ""}
          </p>
        </div>
        <div className="home-setup__bar" title={`${percentualVisivel}%`}>
          <span style={{ width: `${percentualVisivel}%` }} />
        </div>
      </div>
      <div className="home-setup__list">
        {passosVisiveis.map((passo) => (
          <Link
            key={passo.id}
            href={passo.href}
            className={`home-setup__item ${passo.concluido ? "home-setup__item--done" : ""}`}
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
