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
import { getMenuForPapel, isGestaoPapel, type MenuItem } from "@/lib/menu-config";
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
};

function MenuLink({ item, nested }: { item: MenuItem; nested?: boolean }) {
  const pathname = usePathname();
  const Icon = item.icon ? ICONS[item.icon] : null;
  const active = item.href && (pathname === item.href || pathname.startsWith(item.href + "/"));

  if (!item.href) return null;

  const className = `layout-menuitem-link ${active ? "active-route" : ""}`;

  if (item.external) {
    return (
      <a href={item.href} target="_blank" rel="noopener noreferrer" className={className}>
        {Icon && <Icon size={18} />}
        <span className="layout-menuitem-text">{item.label}</span>
      </a>
    );
  }

  return (
    <Link href={item.href} className={className}>
      {Icon && <Icon size={nested ? 16 : 18} />}
      <span className="layout-menuitem-text">{item.label}</span>
    </Link>
  );
}

export function AppMenu() {
  const { session, ready } = useAppSession();
  const menu = ready ? getMenuForPapel(session?.papel) : getMenuForPapel(undefined);
  const admin = ready && isGestaoPapel(session?.papel);

  return (
    <nav className={`layout-menu-container ${admin ? "layout-menu--admin" : "layout-menu--user"}`}>
      <ul className="layout-menu">
        {menu.map((item) =>
          item.items ? (
            <li key={item.label} className="layout-root-menuitem">
              <div className="layout-menuitem-root-text">{item.label}</div>
              <ul className="submenu">
                {item.items.map((sub) => (
                  <li key={sub.label}>
                    <MenuLink item={sub} nested />
                  </li>
                ))}
              </ul>
            </li>
          ) : (
            <li key={item.label}>
              <MenuLink item={item} />
            </li>
          ),
        )}
      </ul>
    </nav>
  );
}
