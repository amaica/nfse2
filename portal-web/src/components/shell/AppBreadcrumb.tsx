"use client";

import { usePathname } from "next/navigation";
import { getMenuForPapel, type MenuItem } from "@/lib/menu-config";
import { useAppSession } from "@/hooks/useAppSession";

function findBreadcrumb(menu: MenuItem[], path: string): string[] {
  for (const item of menu) {
    if (item.href === path) return [item.label];
    if (item.items) {
      for (const sub of item.items) {
        if (sub.href === path) return [item.label, sub.label];
        if (sub.href && path.startsWith(sub.href + "/")) return [item.label, sub.label];
      }
    }
  }
  return ["Início"];
}

export function AppBreadcrumb() {
  const pathname = usePathname();
  const { session, ready } = useAppSession();
  const menu = ready ? getMenuForPapel(session?.papel) : getMenuForPapel(undefined);

  const crumbs = findBreadcrumb(menu, pathname);

  return (
    <nav className="text-sm text-agro-muted" aria-label="Breadcrumb">
      {crumbs.map((c, i) => (
        <span key={c}>
          {i > 0 && <span className="mx-2 text-[var(--primary-200)]">/</span>}
          <span className={i === crumbs.length - 1 ? "font-medium text-agro-body" : ""}>{c}</span>
        </span>
      ))}
    </nav>
  );
}
