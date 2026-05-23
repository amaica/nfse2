"use client";

import { cn } from "@/lib/utils";
import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from "react";

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn("rounded-2xl border border-[var(--border)] bg-[var(--card)] p-6 shadow-sm shadow-slate-200/50", className)}>
      {children}
    </div>
  );
}

export function Label({ children, hint }: { children: ReactNode; hint?: string }) {
  return (
    <div className="mb-2">
      <span className="text-sm font-medium text-slate-800">{children}</span>
      {hint && <p className="mt-0.5 text-xs text-[var(--muted)]">{hint}</p>}
    </div>
  );
}

export const fieldClass =
  "w-full rounded-xl border border-[var(--border)] bg-white px-4 py-3 text-[15px] text-slate-900 shadow-sm transition placeholder:text-slate-400 focus:border-[var(--brand)] focus:outline-none focus:ring-2 focus:ring-[var(--brand)]/20";

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn(fieldClass, props.className)} />;
}

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn(fieldClass, "min-h-[96px] resize-y", props.className)} />;
}

export function PrimaryButton({
  children,
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      type="button"
      {...props}
      className={cn(
        "inline-flex items-center justify-center rounded-xl bg-[var(--brand)] px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-[var(--brand-hover)] disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
    >
      {children}
    </button>
  );
}

export function GhostButton({
  children,
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      type="button"
      {...props}
      className={cn(
        "inline-flex items-center justify-center rounded-xl px-4 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900",
        className,
      )}
    >
      {children}
    </button>
  );
}

export function Toggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
  label: string;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="flex w-full items-center justify-between rounded-xl border border-[var(--border)] bg-white px-4 py-3 text-left transition hover:border-slate-300"
    >
      <span className="text-sm font-medium text-slate-800">{label}</span>
      <span
        className={cn(
          "relative h-6 w-11 rounded-full transition",
          checked ? "bg-[var(--brand)]" : "bg-slate-200",
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition",
            checked ? "left-[22px]" : "left-0.5",
          )}
        />
      </span>
    </button>
  );
}
