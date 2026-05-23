"use client";

import { type EmissaoContexto, formatarCnpjCpf } from "@/lib/api";
import { AlertCircle, CheckCircle2 } from "lucide-react";

/** Exibe apenas dados vindos de GET /api/nfse/emissao/contexto (certificado no servidor). */
export function CertificadoAmbienteBar({ ctx }: { ctx: EmissaoContexto | null }) {
  if (!ctx) return null;

  const homolog = ctx.ambiente?.toLowerCase() === "homologacao";

  return (
    <div
      className={`mb-4 rounded-xl border px-4 py-3 text-sm ${
        ctx.certificadoCadastrado && ctx.podeEmitir
          ? "border-emerald-200 bg-emerald-50/80"
          : "border-amber-200 bg-amber-50/80"
      }`}
    >
      <div className="flex gap-2">
        {ctx.certificadoCadastrado && ctx.podeEmitir ? (
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
        ) : (
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
        )}
        <div>
          <p className="font-medium text-slate-900">
            {homolog ? "Homologação" : "Produção"} · {ctx.prefeitura} (IBGE {ctx.codigoMunicipioIbge})
          </p>
          {ctx.certificadoCadastrado ? (
            <p className="mt-0.5 text-slate-600">
              Prestador: <strong>{ctx.prestadorNome}</strong> · {formatarCnpjCpf(ctx.prestadorDocumento)}
            </p>
          ) : (
            <p className="mt-0.5 text-slate-600">
              Certificado não disponível no servidor. Configure CERTIFICADO_PATH na API.
            </p>
          )}
          {ctx.aviso && <p className="mt-1 text-amber-800">{ctx.aviso}</p>}
          {homolog && ctx.certificadoCadastrado && (
            <p className="mt-2 text-xs text-slate-500">
              Ambiente restrito SEFIN — notas sem valor fiscal.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
