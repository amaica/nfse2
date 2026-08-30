"use client";

import { useEffect, useState } from "react";
import { getAppToken } from "@/lib/app-session";
import { PORTAL_EMPRESA_EVENT, type LoginResponse } from "@/lib/portal-empresa";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { AssinaturaBanner } from "@/components/conta/AssinaturaBanner";
import { EmissaoWizard } from "@/components/nfse/wizard/EmissaoWizard";

/** Emissão NFS-e nativa no portal — iframe/embed fica em Conta → Integração ERP. */
export default function NfseEmissaoPage() {
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    setToken(getAppToken());
  }, []);

  useEffect(() => {
    const onTroca = (ev: Event) => {
      const detail = (ev as CustomEvent<LoginResponse>).detail;
      if (detail?.token) setToken(detail.token);
    };
    window.addEventListener(PORTAL_EMPRESA_EVENT, onTroca);
    return () => window.removeEventListener(PORTAL_EMPRESA_EVENT, onTroca);
  }, []);

  if (!token) {
    return (
      <div className="fiscal-card text-sm text-slate-500">
        Faça login para emitir NFS-e.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <EmitenteEmissaoBar dica="Cadastros, serviços e numeração NFS-e são do emitente selecionado." />
      <AssinaturaBanner compact />
      <EmissaoWizard token={token} />
    </div>
  );
}
