import { cn } from "@/lib/utils";
import { ButtonHTMLAttributes, forwardRef } from "react";

export const Button = forwardRef<HTMLButtonElement, ButtonHTMLAttributes<HTMLButtonElement>>(
  ({ className, ...props }, ref) => (
    <button
      ref={ref}
      className={cn(
        "inline-flex items-center justify-center rounded-lg bg-[var(--brand)] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-[var(--brand-hover)] disabled:opacity-50",
        className,
      )}
      {...props}
    />
  ),
);
Button.displayName = "Button";
