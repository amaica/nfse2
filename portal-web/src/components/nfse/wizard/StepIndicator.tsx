"use client";

import { cn } from "@/lib/utils";
import { Check } from "lucide-react";

const STEPS = ["Cliente", "Serviço", "Valor", "Revisão"];

export function StepIndicator({ current }: { current: number }) {
  return (
    <ol className="mb-8 flex items-center justify-between gap-2">
      {STEPS.map((label, i) => {
        const n = i + 1;
        const done = n < current;
        const active = n === current;
        return (
          <li key={label} className="flex flex-1 flex-col items-center gap-2">
            <div className="flex w-full items-center">
              {i > 0 && (
                <div className={cn("h-0.5 flex-1", done || active ? "bg-[var(--brand)]" : "bg-slate-200")} />
              )}
              <div
                className={cn(
                  "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition",
                  done && "bg-[var(--brand)] text-white",
                  active && "bg-[var(--brand-soft)] text-[var(--brand)] ring-2 ring-[var(--brand)]",
                  !done && !active && "bg-slate-100 text-slate-400",
                )}
              >
                {done ? <Check className="h-4 w-4" /> : n}
              </div>
              {i < STEPS.length - 1 && (
                <div className={cn("h-0.5 flex-1", done ? "bg-[var(--brand)]" : "bg-slate-200")} />
              )}
            </div>
            <span
              className={cn(
                "text-[11px] font-medium sm:text-xs",
                active ? "text-slate-900" : "text-[var(--muted)]",
              )}
            >
              {label}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
