"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { Menu, LogOut } from "lucide-react";
import { clearAppSession, clearAdminKey, getAppSession } from "@/lib/app-session";
import { AppBreadcrumb } from "./AppBreadcrumb";

type Props = {
  onToggleMenu: () => void;
  onToggleSidebar: () => void;
  sidebarCollapsed: boolean;
};

export function AppTopbar({ onToggleMenu, onToggleSidebar }: Props) {
  const router = useRouter();
  const session = getAppSession();

  function logout() {
    clearAppSession();
    clearAdminKey();
    router.push("/login");
  }

  return (
    <header className="layout-topbar">
      <div className="topbar-start">
        <button type="button" className="topbar-menubutton" onClick={onToggleMenu} aria-label="Menu">
          <Menu size={18} />
        </button>
        <button
          type="button"
          className="hidden lg:inline-flex topbar-menubutton"
          onClick={onToggleSidebar}
          aria-label="Recolher menu"
        >
          <Menu size={18} />
        </button>
        <div className="topbar-breadcrumb hidden sm:block">
          <AppBreadcrumb />
        </div>
      </div>
      <div className="topbar-end">
        <ul className="topbar-menu">
          {session && (
            <li className="hidden text-sm text-slate-600 md:block">
              {session.empresaNome}
            </li>
          )}
          <li>
            <button
              type="button"
              onClick={logout}
              className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-100"
            >
              <LogOut size={16} />
              Sair
            </button>
          </li>
          <li>
            <Image src="/images/avatar-m-1.jpg" alt="" width={36} height={36} className="rounded-full" />
          </li>
        </ul>
      </div>
    </header>
  );
}
