"use client";

import { usePathname } from "next/navigation";
import { APP_MENU } from "@/lib/menu-config";

function findBreadcrumb(path: string): string[] {
  for (const item of APP_MENU) {
    if (item.href === path) return [item.label];
    if (item.items) {
      for (const sub of item.items) {
        if (sub.href === path) return [item.label, sub.label];
      }
    }
  }
  return ["Início"];
}

export function AppBreadcrumb() {
  const pathname = usePathname();
  const crumbs = findBreadcrumb(pathname);

  return (
    <nav className="text-sm text-slate-500" aria-label="Breadcrumb">
      {crumbs.map((c, i) => (
        <span key={c}>
          {i > 0 && <span className="mx-2 text-slate-300">/</span>}
          <span className={i === crumbs.length - 1 ? "font-medium text-slate-800" : ""}>{c}</span>
        </span>
      ))}
    </nav>
  );
}
