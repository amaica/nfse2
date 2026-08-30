import Link from "next/link";
import { Check } from "lucide-react";
import type { PlanoMarketing } from "./planos";

export function PlanoCard({ plano }: { plano: PlanoMarketing }) {
  const externo = plano.href.startsWith("mailto:");

  return (
    <div
      className={`saas-dashboard-card flex flex-col ${
        plano.destaque ? "ring-2 ring-[var(--primary-500)] shadow-lg" : ""
      }`}
    >
      {plano.destaque && (
        <span className="mb-3 inline-block w-fit rounded-full bg-[var(--primary-100)] px-3 py-0.5 text-xs font-semibold uppercase tracking-wide text-[var(--primary-700)]">
          Mais popular
        </span>
      )}
      <h3 className="text-xl font-semibold text-agro-body">{plano.nome}</h3>
      <p className="mt-1 text-sm text-agro-muted">{plano.descricao}</p>
      <p className="mt-4">
        <span className="text-3xl font-bold text-[var(--primary-700)]">{plano.preco}</span>
        {plano.periodo && <span className="text-agro-muted">{plano.periodo}</span>}
      </p>
      <ul className="my-6 flex-1 space-y-2.5">
        {plano.recursos.map((r) => (
          <li key={r} className="flex gap-2 text-sm text-agro-body">
            <Check className="mt-0.5 h-4 w-4 shrink-0 text-[var(--primary-500)]" />
            {r}
          </li>
        ))}
      </ul>
      {externo ? (
        <a href={plano.href} className={plano.destaque ? "fiscal-btn-primary text-center" : "btn-ghost text-center"}>
          {plano.cta}
        </a>
      ) : (
        <Link
          href={plano.href}
          className={plano.destaque ? "fiscal-btn-primary text-center" : "btn-ghost text-center"}
        >
          {plano.cta}
        </Link>
      )}
    </div>
  );
}
