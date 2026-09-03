"use client";

import { usePathname } from "next/navigation";
import { resolveOutcome } from "@/lib/menu/tree";
import type { MenuNode } from "@/lib/menu/types";
import { useMenus } from "@/lib/menu/useMenus";

function findBreadcrumb(menu: MenuNode[], path: string): string[] {
  for (const item of menu) {
    const self = resolveOutcome(item.outcome, item.label);
    if (self?.kind === "internal" && self.href === path) return [item.label];
    for (const sub of item.children) {
      const resolved = resolveOutcome(sub.outcome, sub.label);
      if (resolved?.kind === "internal") {
        if (resolved.href === path) return [item.label, sub.label];
        if (path.startsWith(resolved.href + "/")) return [item.label, sub.label];
      }
    }
  }
  return ["Início"];
}

export function AppBreadcrumb() {
  const pathname = usePathname();
  const { menuTree } = useMenus();
  const crumbs = findBreadcrumb(menuTree, pathname);

  return (
    <nav className="text-sm text-agro-muted" aria-label="Breadcrumb">
      {crumbs.map((c, i) => (
        <span key={`${c}-${i}`}>
          {i > 0 && <span className="mx-2 text-[var(--primary-200)]">/</span>}
          <span className={i === crumbs.length - 1 ? "font-medium text-agro-body" : ""}>{c}</span>
        </span>
      ))}
    </nav>
  );
}
