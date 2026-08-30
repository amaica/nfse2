"use client";

import { useState } from "react";
import { Copy, Check, ExternalLink, Plug } from "lucide-react";

type Props = {
  cnpj?: string;
  emailIntegracao?: string;
  embedUrlCnpj?: string;
  embedUrlCnpjComSenha?: string;
  embedUrlEmail?: string;
  /** Exibir aviso de que NF-e é no portal nativo */
  compact?: boolean;
};

function CopiarCampo({ label, valor, sensivel }: { label: string; valor: string; sensivel?: boolean }) {
  const [ok, setOk] = useState(false);
  if (!valor) return null;
  return (
    <div className="space-y-1">
      <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
      <div className="flex gap-2">
        <input
          className="fiscal-input flex-1 font-mono text-xs"
          readOnly
          value={sensivel ? valor.replace(/senha=[^&]+/, "senha=••••••") : valor}
          onFocus={(e) => e.target.select()}
        />
        <button
          type="button"
          className="inline-flex shrink-0 items-center gap-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm hover:bg-slate-50"
          onClick={() => {
            void navigator.clipboard.writeText(valor).then(() => {
              setOk(true);
              window.setTimeout(() => setOk(false), 2000);
            });
          }}
        >
          {ok ? <Check className="h-4 w-4 text-green-600" /> : <Copy className="h-4 w-4" />}
          Copiar
        </button>
      </div>
    </div>
  );
}

export function IntegracaoErpPanel({
  cnpj,
  emailIntegracao,
  embedUrlCnpj,
  embedUrlCnpjComSenha,
  embedUrlEmail,
  compact,
}: Props) {
  const previewUrl = embedUrlCnpjComSenha || embedUrlCnpj;

  return (
    <div className={`rounded-xl border border-slate-200 bg-slate-50/80 ${compact ? "p-4" : "p-5"}`}>
      <div className="mb-3 flex items-center gap-2">
        <Plug className="h-5 w-5 text-[var(--primary-600)]" />
        <h3 className="text-base font-semibold text-slate-800">Integração ERP (iframe NFS-e)</h3>
      </div>
      <p className="mb-4 text-sm text-slate-600">
        Use estas URLs para embutir a <strong>emissão de NFS-e</strong> no seu ERP ou site. Cada emitente
        tem login e cadastros próprios. A <strong>NF-e</strong> é emitida diretamente no portal (
        <code className="rounded bg-white px-1 text-xs">/nfe/emissao</code>).
      </p>

      <div className="space-y-3">
        <CopiarCampo label="URL iframe (CNPJ + senha)" valor={embedUrlCnpjComSenha ?? ""} sensivel />
        <CopiarCampo label="URL iframe (CNPJ — informe senha no ERP)" valor={embedUrlCnpj ?? ""} />
        <CopiarCampo label="URL modelo (e-mail integração)" valor={embedUrlEmail ?? ""} />
        {emailIntegracao && (
          <p className="text-xs text-slate-500">
            Usuário integração: <strong>{emailIntegracao}</strong>
            {cnpj ? ` · CNPJ ${cnpj}` : ""}
          </p>
        )}
      </div>

      {previewUrl && (
        <a
          href={previewUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-[var(--primary-700)] hover:underline"
        >
          <ExternalLink className="h-4 w-4" /> Abrir preview da integração
        </a>
      )}

      {!previewUrl && (
        <p className="mt-3 text-sm text-amber-800">
          Salve o emitente para gerar as URLs de integração (requer CNPJ e senha de integração).
        </p>
      )}
    </div>
  );
}
