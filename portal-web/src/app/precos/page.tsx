"use client";

import Link from "next/link";
import { MarketingShell } from "@/components/marketing/MarketingShell";
import { PlanoCard } from "@/components/marketing/PlanoCard";
import { PLANOS } from "@/components/marketing/planos";

export default function PrecosPage() {
  return (
    <MarketingShell>
      <section className="mx-auto max-w-6xl px-6 py-16">
        <div className="mb-12 text-center">
          <h1 className="login-headline mb-4 text-4xl font-semibold text-agro-body">Planos SyncNota</h1>
          <p className="mx-auto max-w-xl text-lg text-agro-muted">
            Preços transparentes para produtor, grupo ou escritório contábil. Todos incluem download de XMLs
            em ZIP; envio automático ao contador a partir do plano Pro.{" "}
            <strong>14 dias grátis</strong> para testar.
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {PLANOS.map((p) => (
            <PlanoCard key={p.id} plano={p} />
          ))}
        </div>

        <div className="mt-16 saas-dashboard-card mx-auto max-w-2xl text-center">
          <h2 className="text-lg font-semibold text-agro-body">Dúvidas sobre qual plano escolher?</h2>
          <p className="mt-2 text-sm text-agro-muted">
            Comece no Starter — você pode mudar de plano a qualquer momento na área Conta → Assinatura.
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Link href="/registrar" className="fiscal-btn-primary">
              Criar conta grátis
            </Link>
            <a href="mailto:contato@synki.com.br" className="btn-ghost">
              Falar com vendas
            </a>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}
