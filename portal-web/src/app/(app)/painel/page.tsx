"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  ArrowRight,
  BookOpen,
  CreditCard,
  FileText,
  Receipt,
  Settings,
  Users,
} from "lucide-react";
import { formatarCnpjCpf } from "@/lib/api";
import { isGestaoPapel } from "@/lib/menu-config";
import { useAppSession } from "@/hooks/useAppSession";
import { AssinaturaBanner } from "@/components/conta/AssinaturaBanner";
import { SetupChecklist } from "@/components/conta/SetupChecklist";
import { fiscalApi } from "@/lib/fiscal-api";
import type { AssinaturaStatus } from "@/lib/assinatura";

const acoesPrincipais = [
  {
    href: "/nfe/emissao",
    label: "Emitir NF-e",
    desc: "Nota de produto — venda, remessa, devolução",
    icon: FileText,
    cor: "from-emerald-600 to-emerald-700",
  },
  {
    href: "/nfse/emissao",
    label: "Emitir NFS-e",
    desc: "Nota de serviço — prefeitura / SEFIN",
    icon: Receipt,
    cor: "from-sky-600 to-sky-700",
  },
];

const atalhosAdmin = [
  { href: "/conta/contabilidade", label: "Livro Caixa + LCDPR", icon: BookOpen },
  { href: "/cadastros/usuarios", label: "Usuários", icon: Users },
  { href: "/cadastros/empresa", label: "Emitentes", icon: Settings },
  { href: "/conta/assinatura", label: "Assinatura", icon: CreditCard },
];

export default function PainelPage() {
  const { session, ready } = useAppSession();
  const admin = ready && isGestaoPapel(session?.papel);
  const [assinatura, setAssinatura] = useState<AssinaturaStatus | null>(null);

  useEffect(() => {
    fiscalApi
      .request<AssinaturaStatus>("/api/conta/assinatura")
      .then(setAssinatura)
      .catch(() => setAssinatura(null));
  }, []);

  return (
    <div className="animate-in">
      <header className="mb-6">
        <p className="page-header__eyebrow">Início</p>
        <h1 className="page-header__title">
          {ready && session ? `Olá, ${session.nome.split(" ")[0]}` : "SyncNota"}
        </h1>
        <p className="page-header__subtitle">
          {session?.empresaNome ? (
            <>
              Emitente: <strong>{session.empresaNome}</strong>
              {session.empresaCnpj ? ` · ${formatarCnpjCpf(session.empresaCnpj)}` : ""}
            </>
          ) : (
            "Selecione um emitente na barra superior para começar."
          )}
        </p>
      </header>

      <AssinaturaBanner />

      {/* Ações principais */}
      <section className="mb-10">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-agro-muted">
          O que você quer fazer?
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {acoesPrincipais.map((a) => (
            <Link
              key={a.href}
              href={a.href}
              className={`group relative overflow-hidden rounded-2xl bg-gradient-to-br ${a.cor} p-6 text-white shadow-lg transition hover:scale-[1.01] hover:shadow-xl`}
            >
              <a.icon className="mb-3 h-8 w-8 opacity-90" />
              <h3 className="text-xl font-semibold">{a.label}</h3>
              <p className="mt-1 text-sm text-white/85">{a.desc}</p>
              <ArrowRight className="absolute bottom-5 right-5 h-5 w-5 opacity-70 transition group-hover:translate-x-0.5" />
            </Link>
          ))}
        </div>
      </section>

      <SetupChecklist />

      {/* Resumo assinatura */}
      {assinatura && (
        <section className="mb-10">
          <div className="saas-dashboard-card flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-agro-muted">Seu plano</p>
              <p className="mt-1 font-medium text-agro-body">
                {assinatura.mensagemStatus ?? `Status: ${assinatura.status}`}
              </p>
              <p className="mt-1 text-sm text-agro-muted">
                NFS-e: {assinatura.nfseMesUsadas}/{assinatura.nfseMesQuota} · NF-e:{" "}
                {assinatura.nfeMesUsadas}/{assinatura.nfeMesQuota}
              </p>
            </div>
            <Link href="/conta/assinatura" className="fiscal-btn-primary text-sm">
              Gerenciar assinatura
            </Link>
          </div>
        </section>
      )}

      {/* Admin */}
      {admin && (
        <section>
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-agro-muted">
            Administração
          </h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {atalhosAdmin.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="saas-dashboard-card flex items-center gap-3 !py-3"
              >
                <div className="agro-icon-box shrink-0">
                  <item.icon className="h-4 w-4" />
                </div>
                <span className="font-medium text-agro-body">{item.label}</span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
