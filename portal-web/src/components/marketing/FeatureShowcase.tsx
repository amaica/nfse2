"use client";

import { RECURSOS_ICONES, RECURSOS_PLATAFORMA } from "@/lib/recursos-plataforma";
import type { LucideIcon } from "lucide-react";
import { Receipt } from "lucide-react";

const ICONS: Record<string, LucideIcon> = {
  ...RECURSOS_ICONES,
  receipt: Receipt,
};

const COR_CLASSES: Record<string, string> = {
  emerald: "landing-feat--emerald",
  sky: "landing-feat--sky",
  amber: "landing-feat--amber",
  violet: "landing-feat--violet",
  rose: "landing-feat--rose",
  lime: "landing-feat--lime",
  orange: "landing-feat--orange",
  teal: "landing-feat--teal",
};

export function FeatureShowcase() {
  return (
    <div className="landing-feat-grid">
      {RECURSOS_PLATAFORMA.map((r, i) => {
        const Icon = ICONS[r.icon] ?? Receipt;
        const cor = COR_CLASSES[r.cor] ?? "landing-feat--emerald";
        const disponivel = r.status === "disponivel";
        return (
          <article
            key={r.id}
            className={`landing-feat ${cor}`}
            style={{ animationDelay: `${i * 60}ms` }}
          >
            <div className="landing-feat__top">
              <div className="landing-feat__icon">
                <Icon className="h-5 w-5" />
              </div>
              <span className={`landing-feat__badge ${disponivel ? "landing-feat__badge--live" : "landing-feat__badge--soon"}`}>
                {disponivel ? "Disponível" : "Em breve"}
              </span>
            </div>
            <h3 className="landing-feat__title">{r.titulo}</h3>
            <p className="landing-feat__desc">{r.desc}</p>
          </article>
        );
      })}
    </div>
  );
}
