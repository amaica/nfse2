"use client";

import { useEffect, useState } from "react";
import { getAppSession } from "@/lib/app-session";
import { PORTAL_EMPRESA_EVENT } from "@/lib/portal-empresa";

const SESSION_EVENT = "portal-session-change";

export type EmpresaScope = {
  empresaId: number | null;
  empresaNome: string | null;
  empresaCnpj: string | null;
};

const EMPTY_SCOPE: EmpresaScope = {
  empresaId: null,
  empresaNome: null,
  empresaCnpj: null,
};

function lerSessao(): EmpresaScope {
  const s = getAppSession();
  return {
    empresaId: s?.empresaId ?? null,
    empresaNome: s?.empresaNome ?? null,
    empresaCnpj: s?.empresaCnpj ?? null,
  };
}

/** Sessão do emitente ativo — atualiza ao trocar empresa no portal. */
export function useEmpresaScope(): EmpresaScope {
  const [scope, setScope] = useState<EmpresaScope>(EMPTY_SCOPE);

  useEffect(() => {
    const sync = () => setScope(lerSessao());
    sync();
    window.addEventListener(PORTAL_EMPRESA_EVENT, sync);
    window.addEventListener(SESSION_EVENT, sync);
    return () => {
      window.removeEventListener(PORTAL_EMPRESA_EVENT, sync);
      window.removeEventListener(SESSION_EVENT, sync);
    };
  }, []);

  return scope;
}
