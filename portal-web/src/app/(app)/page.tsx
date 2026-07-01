"use client";

import Link from "next/link";
import { getAppSession } from "@/lib/app-session";

export default function HomePage() {
  const session = getAppSession();

  return (
    <div className="fiscal-card animate-in">
      <h1 className="text-2xl font-semibold text-slate-800">Início</h1>
      <p className="mt-2 text-slate-600">
        {session
          ? `Bem-vindo, ${session.nome}. Empresa: ${session.empresaNome}.`
          : "Painel fiscal SyncNota."}
      </p>
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link href="/cadastros/empresa" className="rounded-lg border border-slate-200 p-4 hover:bg-slate-50">
          <h2 className="font-medium text-slate-800">Cadastros</h2>
          <p className="text-sm text-slate-500">Empresa, produtos, CFOP, NCM…</p>
        </Link>
        <Link href="/tributacao/grupo-tributario" className="rounded-lg border border-slate-200 p-4 hover:bg-slate-50">
          <h2 className="font-medium text-slate-800">Tributação</h2>
          <p className="text-sm text-slate-500">Grupo tributário, operação fiscal, OF/GT</p>
        </Link>
        <Link href="/nfse/emissao" className="rounded-lg border border-slate-200 p-4 hover:bg-slate-50">
          <h2 className="font-medium text-slate-800">NFS-e</h2>
          <p className="text-sm text-slate-500">Emissão e consulta (portal integrado)</p>
        </Link>
        <Link href="/nfe/emissao" className="rounded-lg border border-slate-200 p-4 hover:bg-slate-50">
          <h2 className="font-medium text-slate-800">NF-e</h2>
          <p className="text-sm text-slate-500">Emissão e notas autorizadas</p>
        </Link>
        <a
          href="/embed"
          target="_blank"
          rel="noopener noreferrer"
          className="rounded-lg border border-dashed border-slate-300 p-4 hover:bg-slate-50"
        >
          <h2 className="font-medium text-slate-800">Embed ERP</h2>
          <p className="text-sm text-slate-500">Rota iframe para integração externa</p>
        </a>
      </div>
    </div>
  );
}
