"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  ArrowRightLeft,
  BarChart3,
  BookOpen,
  Box,
  Briefcase,
  Building2,
  Calendar,
  Car,
  ChevronDown,
  ClipboardList,
  Cog,
  CreditCard,
  Database,
  Download,
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
  download: Download,
  "chart-bar": BarChart3,
  "clipboard-list": ClipboardList,
  shield: Shield,
  plug: Plug,
  mail: Mail,
  book: BookOpen,
  calendar: Calendar,
};

function nodeMatchesPath(node: MenuNode, pathname: string): boolean {
  const resolved = resolveOutcome(node.outcome, node.label);
  if (!resolved || resolved.kind !== "internal") return false;
  return pathname === resolved.href || pathname.startsWith(resolved.href + "/");
}

function groupHasActiveChild(item: MenuNode, pathname: string): boolean {
  return item.children.some((c) => nodeMatchesPath(c, pathname));
}

function MenuLeaf({ node, nested }: { node: MenuNode; nested?: boolean }) {
  const pathname = usePathname();
  const resolved = resolveOutcome(node.outcome, node.label);
  if (!resolved) return null;

  const Icon = node.icon ? ICONS[node.icon] : null;
  const active = resolved.kind === "internal" && nodeMatchesPath(node, pathname);
  const className = `layout-menuitem-link ${nested ? "layout-menuitem-link--nested" : ""} ${active ? "active-route" : ""}`;

  if (resolved.kind === "external") {
    return (
      <a href={resolved.href} target="_blank" rel="noopener noreferrer" className={className}>
        {Icon ? <Icon size={nested ? 16 : 18} className="layout-menuitem-icon" /> : null}
        <span className="layout-menuitem-text">{node.label}</span>
      </a>
    );
  }

  return (
    <Link href={resolved.href} className={className} title={node.label}>
      {Icon ? <Icon size={nested ? 16 : 18} className="layout-menuitem-icon" /> : null}
      <span className="layout-menuitem-text">{node.label}</span>
    </Link>
  );
}

function MenuGroup({ item }: { item: MenuNode }) {
  const pathname = usePathname();
  const activeChild = useMemo(() => groupHasActiveChild(item, pathname), [item, pathname]);
  const [open, setOpen] = useState(activeChild);
  const ParentIcon = item.icon ? ICONS[item.icon] : null;

  useEffect(() => {
    if (activeChild) setOpen(true);
  }, [activeChild]);

  return (
    <li className={`layout-root-menuitem ${open ? "is-open" : ""} ${activeChild ? "has-active" : ""}`}>
      <button
        type="button"
        className="layout-menuitem-root-toggle"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        title={item.label}
      >
        {ParentIcon ? (
          <ParentIcon size={18} className="layout-menuitem-icon" />
        ) : (
          <span className="layout-menuitem-icon-dot" aria-hidden />
        )}
        <span className="layout-menuitem-text layout-menuitem-root-text">{item.label}</span>
        <ChevronDown
          size={16}
          className={`layout-menuitem-chevron ${open ? "layout-menuitem-chevron--open" : ""}`}
        />
      </button>
      <ul className="submenu" hidden={!open}>
        {item.children.map((sub) => (
          <li key={sub.id}>
            <MenuLeaf node={sub} nested />
          </li>
        ))}
      </ul>
    </li>
  );
}

export function AppMenu() {
  const { session, ready } = useAppSession();
  const { menuTree, loading } = useMenus();
  const admin = ready && isGestaoPapel(session?.papel);

  return (
    <nav className={`layout-menu-container ${admin ? "layout-menu--admin" : "layout-menu--user"}`}>
      {loading && menuTree.length === 0 ? (
        <p className="layout-menu-loading">Carregando menu…</p>
      ) : (
        <ul className="layout-menu">
          {menuTree.map((item) =>
            item.children.length > 0 ? (
              <MenuGroup key={item.id} item={item} />
            ) : (
              <li key={item.id} className="layout-menuitem">
                <MenuLeaf node={item} />
              </li>
            ),
          )}
        </ul>
      )}
    </nav>
  );
}
