"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
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
import { menuAllowsHref } from "@/lib/menu/tree";
import { useMenus } from "@/lib/menu/useMenus";

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
  const { menuTree, loading: menuLoading } = useMenus();
  const admin = ready && isGestaoPapel(session?.papel);
  const [assinatura, setAssinatura] = useState<AssinaturaStatus | null>(null);

  useEffect(() => {
    fiscalApi
      .request<AssinaturaStatus>("/api/conta/assinatura")
      .then(setAssinatura)
      .catch(() => setAssinatura(null));
  }, []);

  const acoesVisiveis = useMemo(() => {
    // Enquanto o menu carrega, não mostra atalhos que possam violar permissão
    if (menuLoading && menuTree.length === 0) return [];
    return acoesPrincipais.filter((a) => menuAllowsHref(menuTree, a.href));
  }, [menuLoading, menuTree]);

  const atalhosAdminVisiveis = useMemo(
    () => atalhosAdmin.filter((a) => menuAllowsHref(menuTree, a.href)),
    [menuTree],
  );

  const podeAssinatura = menuAllowsHref(menuTree, "/conta/assinatura");
  const podeNfse = menuAllowsHref(menuTree, "/nfse/emissao");
  const podeNfe = menuAllowsHref(menuTree, "/nfe/emissao");

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

      {acoesVisiveis.length > 0 ? (
        <section className="mb-10">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-agro-muted">
            O que você quer fazer?
          </h2>
          <div className={`grid gap-4 ${acoesVisiveis.length > 1 ? "sm:grid-cols-2" : "sm:grid-cols-1 max-w-xl"}`}>
            {acoesVisiveis.map((a) => (
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
      ) : null}

      <SetupChecklist />

      {assinatura && (podeAssinatura || podeNfe || podeNfse) ? (
        <section className="mb-10">
          <div className="saas-dashboard-card flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-agro-muted">Seu plano</p>
              <p className="mt-1 font-medium text-agro-body">
                {assinatura.mensagemStatus ?? `Status: ${assinatura.status}`}
              </p>
              <p className="mt-1 text-sm text-agro-muted">
                {[
                  podeNfse
                    ? `NFS-e: ${assinatura.nfseMesUsadas}/${assinatura.nfseMesQuota}`
                    : null,
                  podeNfe ? `NF-e: ${assinatura.nfeMesUsadas}/${assinatura.nfeMesQuota}` : null,
                ]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
            </div>
            {podeAssinatura ? (
              <Link href="/conta/assinatura" className="fiscal-btn-primary text-sm">
                Gerenciar assinatura
              </Link>
            ) : null}
          </div>
        </section>
      ) : null}

      {admin && atalhosAdminVisiveis.length > 0 ? (
        <section>
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-agro-muted">
            Administração
          </h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {atalhosAdminVisiveis.map((item) => (
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
      ) : null}
    </div>
  );
}
