"use client";

import { useState } from "react";
import { Download, Shield } from "lucide-react";
import { getAppToken } from "@/lib/app-session";
import { apiBaseUrl } from "@/lib/api-base";

const API_URL = apiBaseUrl();

export default function LgpdPage() {
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState(false);

  async function exportar() {
    const token = getAppToken();
    if (!token) return;
    setLoading(true);
    setErro("");
    setOk(false);
    try {
      const res = await fetch(`${API_URL}/api/conta/lgpd/export`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error((body as { erro?: string }).erro ?? res.statusText);
      }
      const blob = await res.blob();
      const disp = res.headers.get("Content-Disposition") ?? "";
      const match = disp.match(/filename="([^"]+)"/);
      const filename = match?.[1] ?? "syncnota-lgpd-export.json";
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
      setOk(true);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha na exportação");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="animate-in mx-auto max-w-2xl space-y-6">
      <header>
        <p className="page-header__eyebrow">Conta</p>
        <h1 className="page-header__title">Dados pessoais (LGPD)</h1>
        <p className="page-header__subtitle">Exportação dos dados da sua conta</p>
      </header>

      <section className="fiscal-card p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="agro-icon-box">
            <Shield className="h-4 w-4" />
          </div>
          <div>
            <h2 className="font-semibold text-agro-body">Exportar meus dados</h2>
            <p className="text-sm text-agro-muted">
              Arquivo JSON com usuários, empresas, assinatura, uso mensal e auditoria (12 meses).
            </p>
          </div>
        </div>

        <ul className="mb-6 list-inside list-disc space-y-1 text-sm text-agro-muted">
          <li>Não inclui senhas, certificados A1 ou XMLs de notas</li>
          <li>Apenas administradores da conta podem exportar</li>
          <li>A exportação é registrada na auditoria</li>
        </ul>

        <button
          type="button"
          className="fiscal-btn-primary inline-flex items-center gap-2"
          onClick={() => void exportar()}
          disabled={loading}
        >
          <Download className="h-4 w-4" />
          {loading ? "Gerando arquivo…" : "Baixar exportação LGPD"}
        </button>

        {ok && (
          <p className="mt-4 text-sm text-[var(--primary-700)]">
            Download iniciado. Guarde o arquivo em local seguro.
          </p>
        )}
        {erro && <p className="mt-4 text-sm text-rose-600">{erro}</p>}
      </section>
    </div>
  );
}
