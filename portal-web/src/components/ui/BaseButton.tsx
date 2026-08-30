"use client";

import type { ButtonHTMLAttributes, ReactNode } from "react";

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost";
  loading?: boolean;
  children: ReactNode;
};

export function BaseButton({
  variant = "primary",
  loading,
  className = "",
  children,
  disabled,
  type = "button",
  ...props
}: Props) {
  if (variant === "primary") {
    return (
      <button
        type={type}
        className={`btn-primary-gradient ${className}`}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? "Aguarde…" : children}
      </button>
    );
  }

  const secondary =
    "inline-flex w-full items-center justify-center rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50";
  const ghost =
    "inline-flex items-center justify-center rounded-xl px-3 py-2 text-sm text-slate-600 hover:bg-slate-100";

  return (
    <button
      type={type}
      className={`${variant === "secondary" ? secondary : ghost} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? "Aguarde…" : children}
    </button>
  );
}
