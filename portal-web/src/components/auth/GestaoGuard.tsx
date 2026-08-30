"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { isGestaoPapel } from "@/lib/menu-config";
import { useAppSession } from "@/hooks/useAppSession";

export function GestaoGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { session, ready } = useAppSession();
  const allowed = ready && isGestaoPapel(session?.papel);

  useEffect(() => {
    if (ready && !allowed) {
      router.replace("/painel");
    }
  }, [ready, allowed, router]);

  if (!ready || !allowed) {
    return <div className="app-loading">Carregando…</div>;
  }

  return <>{children}</>;
}
