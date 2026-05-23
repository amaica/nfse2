"use client";

import { useEmbedToken } from "@/hooks/useEmbedToken";
import { Suspense, type ReactNode } from "react";

function EmbedGate({ children }: { children: ReactNode }) {
  const { valid, loading } = useEmbedToken();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--background)] text-[var(--muted)]">
        Carregando...
      </div>
    );
  }

  if (!valid) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-2 bg-[var(--background)] p-6 text-center">
        <p className="text-lg font-medium text-slate-900">Token inválido</p>
        <p className="max-w-sm text-sm text-[var(--muted)]">
          Verifique o parâmetro <span className="font-mono">t</span> na URL do iframe ou gere um novo token no seu sistema.
        </p>
      </div>
    );
  }

  return <>{children}</>;
}

export default function EmbedLayout({ children }: { children: ReactNode }) {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-[var(--background)]">...</div>
      }
    >
      <EmbedGate>{children}</EmbedGate>
    </Suspense>
  );
}
