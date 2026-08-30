"use client";

import Link from "next/link";
import { ArrowRight, CheckCircle2, Sparkles, Sprout } from "lucide-react";
import { MarketingShell } from "./MarketingShell";
import { PlanoCard } from "./PlanoCard";
import { FeatureShowcase } from "./FeatureShowcase";
import { COMO_FUNCIONA, FAQ, PLANOS } from "./planos";

export function LandingContent() {
  return (
    <MarketingShell>
      <section className="mx-auto max-w-6xl px-6 pb-12 pt-4 lg:pb-20 lg:pt-6">
        <div className="mb-8 flex flex-wrap gap-2 landing-stat-strip">
          {["XML automático p/ contador", "Livro Caixa dos XMLs", "LCDPR leiaute 1.3", "NFS-e + NF-e"].map((text) => (
            <span key={text} className="landing-stat-pill">
              <Sparkles className="h-4 w-4" />
              {text}
            </span>
          ))}
        </div>

        <div className="grid items-center gap-12 lg:grid-cols-2">
          <div>
            <p className="login-eyebrow mb-4 inline-flex items-center gap-2">
              <Sprout className="h-4 w-4" />
              Contabilidade rural sem burocracia
            </p>
            <h1 className="login-headline mb-6 text-4xl font-semibold leading-[1.1] tracking-tight sm:text-5xl lg:text-[2.85rem]">
              <span className="login-headline__line">Mais que um emissor de notas:</span>
              <br />
              <span className="login-headline__accent">agilize a contabilidade rural com dados reais.</span>
            </h1>
            <p className="mb-5 max-w-lg text-lg leading-relaxed text-[#4a5c44]">
              Garanta tranquilidade nos períodos de prestar contas com a Receita Federal. Ajudamos você e
              seu contador a manter a contabilidade organizada e sem erros.
            </p>
            <p className="mb-8 max-w-lg text-base leading-relaxed text-agro-muted">
              Emita NFS-e e NF-e, envie XML ao escritório no automático, baixe o pacote do período e gere{" "}
              <strong className="text-agro-body">Livro Caixa + LCDPR</strong> a partir dos XMLs das notas.{" "}
              <strong className="text-agro-body">14 dias grátis</strong>, sem cartão.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link href="/registrar" className="fiscal-btn-primary inline-flex items-center gap-2 px-6 py-3 text-base shadow-lg shadow-emerald-900/10">
                Criar conta grátis
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link href="/login" className="btn-ghost px-6 py-3 text-base">
                Já tenho conta
              </Link>
            </div>
            <ul className="mt-8 grid gap-2 sm:grid-cols-2">
              {[
                "Livro Caixa a partir dos XMLs",
                "LCDPR leiaute 1.3 (RFB)",
                "Envio automático de XML",
                "Download ZIP por período",
              ].map((t) => (
                <li key={t} className="flex items-center gap-2 text-sm font-medium text-agro-body">
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
                  {t}
                </li>
              ))}
            </ul>
          </div>

          <div className="landing-hero-panel">
            <div className="border-b border-[var(--border)] bg-gradient-to-r from-[var(--primary-600)] to-[var(--primary-500)] px-5 py-4 text-white">
              <p className="text-xs font-bold uppercase tracking-widest opacity-90">Fluxo SyncNota</p>
              <p className="mt-1 text-sm font-medium">Nota → XML → Livro Caixa → LCDPR</p>
            </div>
            {[
              { label: "1. Emitir NF-e / NFS-e", cls: "landing-hero-panel__item--emerald" },
              { label: "2. XML automático p/ contabilidade", cls: "landing-hero-panel__item--sky" },
              { label: "3. Livro Caixa (CSV dos XMLs)", cls: "landing-hero-panel__item--amber" },
              { label: "4. Arquivo LCDPR para o PVA", cls: "landing-hero-panel__item--rose" },
            ].map((item) => (
              <div key={item.label} className={`landing-hero-panel__item ${item.cls}`}>
                {item.label}
                <ArrowRight className="h-4 w-4 opacity-50" />
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="landing-section--vivid border-y border-[var(--border)] py-16 lg:py-20">
        <div className="mx-auto max-w-6xl px-6">
          <div className="mb-10 text-center">
            <span className="landing-section__label">
              <Sparkles className="h-3.5 w-3.5" />
              Tudo em um lugar
            </span>
            <h2 className="text-3xl font-bold tracking-tight text-agro-body sm:text-4xl">
              Funcionalidades que trabalham por você
            </h2>
            <p className="mx-auto mt-3 max-w-2xl text-base text-agro-muted">
              Emissão fiscal, entrega ao contador e escrituração rural — com dados extraídos dos XMLs reais.
            </p>
          </div>
          <FeatureShowcase />
        </div>
      </section>

      <section className="bg-gradient-to-r from-[var(--primary-700)] to-[var(--primary-500)] py-12 text-white">
        <div className="mx-auto grid max-w-6xl gap-8 px-6 sm:grid-cols-4">
          {[
            { n: "LCDPR", u: "", d: "dos XMLs das notas" },
            { n: "Auto", u: "", d: "envio XML contador" },
            { n: "ZIP", u: "", d: "download por período" },
            { n: "14", u: "dias", d: "trial gratuito" },
          ].map((s) => (
            <div key={s.d} className="text-center">
              <p className="text-3xl font-bold">
                {s.n}
                {s.u && <span className="text-lg font-semibold"> {s.u}</span>}
              </p>
              <p className="mt-1 text-sm text-white/80">{s.d}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 py-20">
        <h2 className="mb-3 text-center text-3xl font-bold text-agro-body">Como funciona</h2>
        <p className="mx-auto mb-12 max-w-xl text-center text-agro-muted">
          Do cadastro à entrega do LCDPR — sem sair da fazenda.
        </p>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {COMO_FUNCIONA.map((c) => (
            <div key={c.passo} className="saas-dashboard-card border-2 border-transparent text-center hover:border-[var(--primary-200)]">
              <span className="mb-4 inline-flex h-11 w-11 items-center justify-center rounded-full bg-gradient-to-br from-[var(--primary-500)] to-[var(--primary-400)] text-lg font-bold text-white shadow-md">
                {c.passo}
              </span>
              <h3 className="mb-2 text-lg font-bold text-agro-body">{c.titulo}</h3>
              <p className="text-sm text-agro-muted">{c.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-[var(--primary-50)]/60 py-20" id="planos">
        <div className="mx-auto max-w-6xl px-6">
          <h2 className="mb-3 text-center text-3xl font-bold text-agro-body">Planos simples</h2>
          <p className="mx-auto mb-12 max-w-lg text-center text-agro-muted">
            Livro Caixa, LCDPR, XML automático e download em ZIP — do produtor ao escritório contábil.
          </p>
          <div className="grid gap-6 lg:grid-cols-3">
            {PLANOS.map((p) => (
              <PlanoCard key={p.id} plano={p} />
            ))}
          </div>
          <p className="mt-8 text-center text-sm text-agro-muted">
            <Link href="/precos" className="link-agro font-semibold">
              Comparar planos em detalhe →
            </Link>
          </p>
        </div>
      </section>

      <section className="border-t border-[var(--border)] py-20">
        <div className="mx-auto max-w-3xl px-6">
          <h2 className="mb-10 text-center text-3xl font-bold text-agro-body">Perguntas frequentes</h2>
          <div className="space-y-4">
            {FAQ.map((f) => (
              <div key={f.q} className="saas-dashboard-card border-l-4 border-l-[var(--primary-400)]">
                <h3 className="font-bold text-agro-body">{f.q}</h3>
                <p className="mt-2 text-sm leading-relaxed text-agro-muted">{f.a}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-[var(--primary-700)] via-[var(--primary-600)] to-[#4a7c59] px-8 py-14 text-center text-white shadow-2xl">
          <h2 className="relative mb-4 text-3xl font-bold">
            Emita notas. Gere Livro Caixa e LCDPR. Entregue ao contador.
          </h2>
          <p className="relative mx-auto mb-8 max-w-lg text-white/90">
            Tudo a partir dos XMLs das suas NFS-e e NF-e — direto da propriedade.
          </p>
          <Link href="/registrar" className="relative inline-flex items-center gap-2 rounded-xl bg-white px-8 py-3 text-base font-bold text-[var(--primary-700)] shadow-lg hover:bg-[var(--primary-50)]">
            Começar grátis — 14 dias
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </section>
    </MarketingShell>
  );
}
