"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  ArrowRightLeft,
  BarChart3,
  BookOpen,
  Box,
  Briefcase,
  Building2,
  Calendar,
  Car,
  ClipboardList,
  Cog,
  CreditCard,
  Database,
  ExternalLink,
  File,
  FileEdit,
  Frame,
  Home,
  List,
  Mail,
  Network,
  Pencil,
  Percent,
  Plug,
  Receipt,
  Scale,
  Settings,
  Shield,
  Tag,
  UserPlus,
  Users,
  type LucideIcon,
} from "lucide-react";
import { isGestaoPapel } from "@/lib/menu-config";
import { resolveOutcome } from "@/lib/menu/tree";
import type { MenuNode } from "@/lib/menu/types";
import { useMenus } from "@/lib/menu/useMenus";
import { useAppSession } from "@/hooks/useAppSession";

const ICONS: Record<string, LucideIcon> = {
  home: Home,
  database: Database,
  building: Building2,
  "user-plus": UserPlus,
  users: Users,
  box: Box,
  "arrow-right-left": ArrowRightLeft,
  tag: Tag,
  car: Car,
  cog: Cog,
  percent: Percent,
  scale: Scale,
  sitemap: Network,
  briefcase: Briefcase,
  settings: Settings,
  file: File,
  pencil: Pencil,
  list: List,
  "file-edit": FileEdit,
  receipt: Receipt,
  "external-link": ExternalLink,
  frame: Frame,
  "credit-card": CreditCard,
  "chart-bar": BarChart3,
  "clipboard-list": ClipboardList,
  shield: Shield,
  plug: Plug,
  mail: Mail,
  book: BookOpen,
  calendar: Calendar,
};

function MenuLeaf({ node, nested }: { node: MenuNode; nested?: boolean }) {
  const pathname = usePathname();
  const resolved = resolveOutcome(node.outcome, node.label);
  if (!resolved) return null;

  const Icon = node.icon ? ICONS[node.icon] : null;
  const active =
    resolved.kind === "internal" &&
    (pathname === resolved.href || pathname.startsWith(resolved.href + "/"));
  const className = `layout-menuitem-link ${active ? "active-route" : ""}`;

  if (resolved.kind === "external") {
    return (
      <a href={resolved.href} target="_blank" rel="noopener noreferrer" className={className}>
        {Icon && <Icon size={nested ? 16 : 18} />}
        <span className="layout-menuitem-text">{node.label}</span>
      </a>
    );
  }

  return (
    <Link href={resolved.href} className={className}>
      {Icon && <Icon size={nested ? 16 : 18} />}
      <span className="layout-menuitem-text">{node.label}</span>
    </Link>
  );
}

export function AppMenu() {
  const { session, ready } = useAppSession();
  const { menuTree, loading } = useMenus();
  const admin = ready && isGestaoPapel(session?.papel);

  return (
    <nav className={`layout-menu-container ${admin ? "layout-menu--admin" : "layout-menu--user"}`}>
      {loading && menuTree.length === 0 ? (
        <p className="layout-menuitem-root-text" style={{ padding: "0.75rem 1rem" }}>
          Carregando menu…
        </p>
      ) : (
        <ul className="layout-menu">
          {menuTree.map((item) =>
            item.children.length > 0 ? (
              <li key={item.id} className="layout-root-menuitem">
                <div className="layout-menuitem-root-text">{item.label}</div>
                <ul className="submenu">
                  {item.children.map((sub) => (
                    <li key={sub.id}>
                      <MenuLeaf node={sub} nested />
                    </li>
                  ))}
                </ul>
              </li>
            ) : (
              <li key={item.id}>
                <MenuLeaf node={item} />
              </li>
            ),
          )}
        </ul>
      )}
    </nav>
  );
}
