"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError, empresaPortalApi, type EmpresaDetalhe } from "@/lib/api";
import { getAppToken } from "@/lib/app-session";
import { useAppSession } from "@/hooks/useAppSession";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { IntegracaoErpPanel } from "@/components/conta/IntegracaoErpPanel";
import { PORTAL_EMPRESA_EVENT } from "@/lib/portal-empresa";

export default function IntegracaoErpPage() {
  const [token, setToken] = useState<string | null>(null);
  const { session, ready } = useAppSession();
  const { empresaId } = useEmpresaScope();
  const [det, setDet] = useState<EmpresaDetalhe | null>(null);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setToken(getAppToken());
  }, []);

  const carregar = useCallback(async () => {
    if (!token || !empresaId) {
      setDet(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setErro("");
    try {
      const d = await empresaPortalApi.obterEmpresa(token, empresaId);
      setDet(d);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar emitente");
      setDet(null);
    } finally {
      setLoading(false);
    }
  }, [token, empresaId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  useEffect(() => {
    const onTroca = () => void carregar();
    window.addEventListener(PORTAL_EMPRESA_EVENT, onTroca);
    return () => window.removeEventListener(PORTAL_EMPRESA_EVENT, onTroca);
  }, [carregar]);

  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-2xl font-semibold text-slate-800">Integração ERP</h1>
        <p className="mt-1 text-sm text-slate-500">
          Links de iframe por emitente — troque o emitente abaixo para ver as URLs de cada um.
        </p>
      </header>

      <EmitenteEmissaoBar dica="Cada emitente tem URL, certificado e cadastros isolados." />

      {erro && <p className="text-sm text-red-600">{erro}</p>}
      {loading && <p className="text-sm text-slate-500">Carregando…</p>}

      {!loading && det && (
        <IntegracaoErpPanel
          cnpj={det.cnpj}
          emailIntegracao={det.emailIntegracao}
          embedUrlCnpj={det.embedUrlCnpj}
          embedUrlCnpjComSenha={det.embedUrlCnpjComSenha}
          embedUrlEmail={det.embedUrlEmail}
        />
      )}

      {!loading && !det && empresaId && (
        <p className="text-sm text-slate-500">Não foi possível carregar dados do emitente {empresaId}.</p>
      )}

      {!empresaId && ready && session && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          Selecione um emitente na barra superior.
        </p>
      )}
    </div>
  );
}
