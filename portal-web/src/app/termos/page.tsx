import Link from "next/link";
import { MarketingShell } from "@/components/marketing/MarketingShell";

export default function TermosPage() {
  return (
    <MarketingShell>
      <article className="mx-auto max-w-3xl px-6 py-16 prose prose-slate">
        <h1 className="text-3xl font-semibold text-agro-body">Termos de uso</h1>
        <p className="text-sm text-agro-muted">Última atualização: julho de 2026</p>

        <section className="mt-8 space-y-4 text-sm leading-relaxed text-agro-body">
          <p>
            Ao utilizar o SyncNota, você concorda com estes termos. O serviço é oferecido pela Synki como
            plataforma SaaS de gestão fiscal (NFS-e, NF-e e cadastros).
          </p>
          <h2 className="text-lg font-semibold">Conta e responsabilidade</h2>
          <p>
            Você é responsável pelos dados fiscais, certificados digitais e notas emitidas na sua conta.
            Mantenha credenciais seguras e informe usuários autorizados apenas.
          </p>
          <h2 className="text-lg font-semibold">Assinatura e trial</h2>
          <p>
            O período de trial é gratuito conforme o plano escolhido. Após o trial, a cobrança recorrente
            ocorre via Stripe até cancelamento na área Conta → Assinatura.
          </p>
          <h2 className="text-lg font-semibold">Disponibilidade</h2>
          <p>
            Buscamos alta disponibilidade, mas emissões dependem também de prefeituras, SEFAZ e Receita
            Federal. Não nos responsabilizamos por indisponibilidade de terceiros.
          </p>
        </section>

        <p className="mt-10">
          <Link href="/" className="link-agro">
            ← Voltar ao início
          </Link>
        </p>
      </article>
    </MarketingShell>
  );
}
