"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  ArrowRightLeft,
  Box,
  Briefcase,
  Building2,
  Car,
  Database,
  ExternalLink,
  File,
  FileEdit,
  Frame,
  Home,
  List,
  Pencil,
  Percent,
  Receipt,
  Settings,
  Network,
  Tag,
  UserPlus,
  Users,
  type LucideIcon,
} from "lucide-react";
import { APP_MENU, type MenuItem } from "@/lib/menu-config";

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
  percent: Percent,
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
  return (
    <nav className="layout-menu-container">
      <ul className="layout-menu">
        {APP_MENU.map((item) =>
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
