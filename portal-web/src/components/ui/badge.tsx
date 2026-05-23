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
        variant === "success" && "bg-emerald-500/15 text-emerald-400",
        variant === "warn" && "bg-amber-500/15 text-amber-400",
        variant === "default" && "bg-zinc-500/15 text-zinc-300",
        className,
      )}
    >
      {children}
    </span>
  );
}
