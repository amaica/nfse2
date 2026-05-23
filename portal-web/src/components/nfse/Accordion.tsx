"use client";

import { ChevronDown } from "lucide-react";
import { useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Accordion({
  title,
  defaultOpen = false,
  children,
}: {
  title: string;
  subtitle?: string;
  defaultOpen?: boolean;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <section className="overflow-hidden rounded-xl border border-[var(--border)] bg-slate-50/80">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-4 py-3 text-left text-sm font-medium text-slate-700"
      >
        {title}
        <ChevronDown className={cn("h-4 w-4 text-slate-400 transition", open && "rotate-180")} />
      </button>
      {open && <div className="border-t border-[var(--border)] bg-white px-4 py-4">{children}</div>}
    </section>
  );
}

export function FormGrid({ children, cols = 2 }: { children: ReactNode; cols?: 1 | 2 | 3 | 4 }) {
  const cls =
    cols === 1
      ? "grid gap-3"
      : cols === 4
        ? "grid gap-3 sm:grid-cols-2 lg:grid-cols-4"
        : cols === 3
          ? "grid gap-3 sm:grid-cols-2 lg:grid-cols-3"
          : "grid gap-3 sm:grid-cols-2";
  return <div className={cls}>{children}</div>;
}

export function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  required?: boolean;
  children: ReactNode;
}) {
  return (
    <label className="block space-y-1">
      <span className="text-xs font-medium text-slate-600">{label}</span>
      {hint && <span className="block text-[11px] text-slate-400">{hint}</span>}
      {children}
    </label>
  );
}

export const inputCls =
  "w-full rounded-lg border border-[var(--border)] bg-white px-3 py-2 text-sm text-slate-900 focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)]/30";

export const selectCls = inputCls;
