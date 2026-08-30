"use client";

import Link from "next/link";
import { type EmissaoContexto, formatarCnpjCpf } from "@/lib/api";
import { AlertCircle, CheckCircle2 } from "lucide-react";

/** Exibe apenas dados vindos de GET /api/nfse/emissao/contexto (certificado no servidor). */
export function CertificadoAmbienteBar({ ctx }: { ctx: EmissaoContexto | null }) {
  if (!ctx) return null;

  const homolog = ctx.ambiente?.toLowerCase() === "homologacao";
  const ok = ctx.certificadoCadastrado && ctx.podeEmitir;

  return (
    <div
      className={`mb-4 rounded-xl border px-4 py-3 text-sm ${
        ok ? "alert-agro-success" : "border-amber-200 bg-amber-50/80"
      }`}
    >
      <div className="flex gap-2">
        {ok ? (
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-[var(--primary-600)]" />
        ) : (
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
        )}
        <div>
          <p className="font-medium text-agro-body">
            {homolog ? "Homologação" : "Produção"} · {ctx.prefeitura} (IBGE {ctx.codigoMunicipioIbge})
          </p>
          {ctx.certificadoCadastrado ? (
            <p className="mt-0.5 text-agro-muted">
              Prestador: <strong>{ctx.prestadorNome}</strong> · {formatarCnpjCpf(ctx.prestadorDocumento)}
            </p>
          ) : (
            <p className="mt-0.5 text-agro-muted">
              {ctx.aviso ?? "Certificado digital (A1) ainda não cadastrado para este emitente."}{" "}
              <Link href="/cadastros/empresa" className="font-medium text-[var(--primary-700)] hover:underline">
                Cadastros → Emitentes
              </Link>{" "}
              para enviar o arquivo .pfx.
            </p>
          )}
          {ctx.certificadoCadastrado && ctx.aviso && (
            <p className="mt-1 text-amber-800">{ctx.aviso}</p>
          )}
          {homolog && ctx.certificadoCadastrado && (
            <p className="mt-2 text-xs text-agro-muted">
              Ambiente restrito SEFIN — notas sem valor fiscal.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
