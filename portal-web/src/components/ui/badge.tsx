import { cn } from "@/lib/utils";

export function Badge({
  children,
  variant = "default",
  className,
}: {
  children: React.ReactNode;
  variant?: "default" | "success" | "warn";
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium",
        variant === "success" && "badge-agro-success",
        variant === "warn" && "badge-agro-warn",
        variant === "default" && "bg-[var(--primary-50)] text-[var(--primary-700)]",
        className,
      )}
    >
      {children}
    </span>
  );
}
