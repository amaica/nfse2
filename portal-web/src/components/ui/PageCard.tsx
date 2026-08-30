"use client";

import type { ReactNode } from "react";

type Props = {
  title: string;
  subtitle?: string;
  badge?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
  icon?: ReactNode;
};

/** Card de página no estilo multiatendimento (AssinaturaCard). */
export function PageCard({ title, subtitle, badge, footer, children, icon }: Props) {
  return (
    <div className="page-card overflow-hidden">
      <div className="page-card__header">
        <div className="flex items-center gap-3">
          {icon && <div className="page-card__icon">{icon}</div>}
          <div>
            <h2 className="page-card__title">{title}</h2>
            {subtitle && <p className="page-card__subtitle">{subtitle}</p>}
          </div>
        </div>
        {badge}
      </div>
      <div className="page-card__body">{children}</div>
      {footer && <div className="page-card__footer">{footer}</div>}
    </div>
  );
}

export function StatusBadge({ label, tone = "success" }: { label: string; tone?: "success" | "warn" | "danger" | "neutral" }) {
  return <span className={`status-badge status-badge--${tone}`}>{label}</span>;
}

export function PageHeader({
  eyebrow,
  title,
  subtitle,
}: {
  eyebrow?: string;
  title: string;
  subtitle?: string;
}) {
  return (
    <header className="page-header mb-8">
      {eyebrow && <p className="page-header__eyebrow">{eyebrow}</p>}
      <h1 className="page-header__title">{title}</h1>
      {subtitle && <p className="page-header__subtitle">{subtitle}</p>}
    </header>
  );
}
