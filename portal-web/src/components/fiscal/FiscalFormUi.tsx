"use client";

import type { ReactNode } from "react";

export function FiscalSection({ title, children, className = "" }: { title: string; children: ReactNode; className?: string }) {
  return (
    <div className={`fiscal-section-card ${className}`}>
      <div className="fiscal-section-title">{title}</div>
      {children}
    </div>
  );
}

export function FiscalField({
  label,
  children,
  className = "",
}: {
  label: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <label className={`fiscal-field ${className}`}>
      <span className="fiscal-field-label">{label}</span>
      {children}
    </label>
  );
}

export function FiscalRow({ children }: { children: ReactNode }) {
  return <div className="fiscal-field-row">{children}</div>;
}
