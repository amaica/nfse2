"use client";

import { useEmbedToken } from "@/hooks/useEmbedToken";
import { Suspense, type ReactNode } from "react";

function EmbedGate({ children }: { children: ReactNode }) {
  const { valid, loading, erro } = useEmbedToken();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--background)] text-[var(--muted)]">
        Carregando...
      </div>
    );
  }

  if (!valid) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-[var(--background)] p-6 text-center">
        <p className="text-lg font-medium text-slate-900">
          {erro && erro !== "missing" ? "Acesso negado" : "Acesso ao portal NFS-e"}
        </p>
        <p className="max-w-md text-sm text-[var(--muted)]">
          {erro && erro !== "missing" ? (
            erro
          ) : (
            <>
              Abra com CNPJ e senha na URL do iframe:
              <br />
              <span className="mt-2 block break-all font-mono text-xs text-slate-600">
                /embed/06866960000115?senha=***
              </span>
              <br />
              <span className="mt-2 block text-xs text-amber-700">
                Se a senha tiver <span className="font-mono">#</span>, use{" "}
                <span className="font-mono">%23</span> na URL ou a senha inteira após{" "}
                <span className="font-mono">?senha=</span> (ex.: ?senha=@lface#81).
              </span>
              <br />
              Ou <span className="font-mono">/embed?email=...</span> / <span className="font-mono">?t=TOKEN</span>.
            </>
          )}
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
