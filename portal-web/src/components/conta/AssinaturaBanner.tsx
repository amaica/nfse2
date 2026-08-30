"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { AlertTriangle, CreditCard, Sparkles } from "lucide-react";
import { fiscalApi } from "@/lib/fiscal-api";
import type { AssinaturaStatus } from "@/lib/assinatura";

type Props = {
  compact?: boolean;
};

export function AssinaturaBanner({ compact }: Props) {
  const [data, setData] = useState<AssinaturaStatus | null>(null);

  const carregar = useCallback(async () => {
    try {
      const res = await fiscalApi.request<AssinaturaStatus>("/api/conta/assinatura");
      setData(res);
    } catch {
      setData(null);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  if (!data || data.podeEmitir !== false) {
    if (!compact && data?.status === "trial" && data.mensagemStatus) {
      return (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-900">
          <span className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 shrink-0" />
            {data.mensagemStatus}
          </span>
          <Link href="/conta/assinatura" className="font-medium text-sky-800 hover:underline">
            Ver planos
          </Link>
        </div>
      );
    }
    return null;
  }

  return (
    <div className="mb-6 rounded-xl border border-rose-200 bg-rose-50 px-4 py-4 text-sm text-rose-900">
      <div className="flex flex-wrap items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
        <div className="min-w-0 flex-1">
          <p className="font-semibold">Emissão bloqueada</p>
          <p className="mt-1">{data.mensagemStatus ?? "Regularize sua assinatura para emitir notas."}</p>
        </div>
        <Link
          href="/conta/assinatura"
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-700 px-3 py-2 text-sm font-medium text-white hover:bg-rose-800"
        >
          <CreditCard className="h-4 w-4" />
          Assinar agora
        </Link>
      </div>
    </div>
  );
}
