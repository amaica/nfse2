"use client";

import { ConsultaPanel } from "@/components/nfse/ConsultaPanel";
import { EmissaoWizard } from "@/components/nfse/wizard/EmissaoWizard";
import { useEmbedToken } from "@/hooks/useEmbedToken";
import { cn } from "@/lib/utils";
import { useState } from "react";

type Aba = "emissao" | "consulta";

export default function EmbedPage() {
  const { token } = useEmbedToken();
  const [aba, setAba] = useState<Aba>("emissao");

  if (!token) return null;

  return (
    <div className="min-h-screen w-full bg-[var(--background)] px-4 py-4 sm:px-6 sm:py-6">
      <nav className="mb-4 flex w-full justify-end">
        <div className="flex gap-1 rounded-full bg-slate-100 p-1">
          {(
            [
              ["emissao", "Emitir"],
              ["consulta", "Consultar"],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              onClick={() => setAba(id)}
              className={cn(
                "rounded-full px-4 py-1.5 text-sm font-medium transition",
                aba === id ? "bg-white text-slate-900 shadow-sm" : "text-[var(--muted)] hover:text-slate-700",
              )}
            >
              {label}
            </button>
          ))}
        </div>
      </nav>
      <div className="w-full">
        {aba === "emissao" ? <EmissaoWizard token={token} /> : <ConsultaPanel token={token} />}
      </div>
    </div>
  );
}
