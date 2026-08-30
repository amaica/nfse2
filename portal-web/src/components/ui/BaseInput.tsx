"use client";

import type { InputHTMLAttributes, ReactNode } from "react";

type Props = InputHTMLAttributes<HTMLInputElement> & {
  icon?: ReactNode;
};

export function BaseInput({ icon, className = "", ...props }: Props) {
  return (
    <div className="relative">
      {icon && (
        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4 text-slate-400">
          {icon}
        </div>
      )}
      <input className={`input-saas ${icon ? "" : "!pl-4"} ${className}`} {...props} />
    </div>
  );
}
