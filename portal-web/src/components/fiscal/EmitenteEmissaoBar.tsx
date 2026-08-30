"use client";

import { Building2, RefreshCw } from "lucide-react";
import { formatarCnpjCpf } from "@/lib/api";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { EmpresaSwitcher } from "@/components/shell/EmpresaSwitcher";

type Props = {
  /** Texto curto abaixo da barra (opcional). */
  dica?: string;
};

/** Barra de emitente nas telas de emissão — troca na hora sem sair do fluxo. */
export function EmitenteEmissaoBar({ dica }: Props) {
  const { empresaNome, empresaCnpj } = useEmpresaScope();

  return (
    <div className="rounded-xl border border-[var(--primary-200)] bg-gradient-to-r from-[var(--primary-50)] to-white px-4 py-3 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white shadow-sm">
            <Building2 className="h-5 w-5 text-[var(--primary-600)]" />
          </div>
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-wide text-[var(--primary-700)]">
              Emitente ativo
            </p>
            <p className="truncate text-base font-semibold text-agro-body">
              {empresaNome ?? "Nenhum emitente selecionado"}
            </p>
            {empresaCnpj && (
              <p className="text-sm text-agro-muted">{formatarCnpjCpf(empresaCnpj)}</p>
            )}
          </div>
        </div>
        <div className="flex shrink-0 flex-col items-stretch gap-1 sm:items-end">
          <span className="hidden text-xs text-agro-muted sm:block">Trocar emitente</span>
          <EmpresaSwitcher labelTrocar />
        </div>
      </div>
      <p className="mt-2 flex items-center gap-1.5 text-xs text-agro-muted">
        <RefreshCw className="h-3 w-3 shrink-0" />
        {dica ??
          "Ao trocar o emitente, a numeração, cadastros e certificado são recarregados automaticamente."}
      </p>
    </div>
  );
}
