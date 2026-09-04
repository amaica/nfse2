"use client";

import Image from "next/image";
import { isGestaoPapel } from "@/lib/menu-config";
import { useAppSession } from "@/hooks/useAppSession";
import { AppMenu } from "./AppMenu";

export function AppSidebar() {
  const { session, ready } = useAppSession();
  const admin = ready && isGestaoPapel(session?.papel);

  return (
    <aside className={`layout-sidebar ${admin ? "layout-sidebar--admin" : "layout-sidebar--user"}`}>
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <div className="sidebar-brand__mark">
            <Image
              src="/images/logo.png"
              alt="SyncNota"
              width={28}
              height={28}
              className="sidebar-brand__logo"
            />
          </div>
          <div className="sidebar-brand__text-wrap">
            <span className="sidebar-brand__text">SyncNota</span>
            <span className="sidebar-brand__sub">Gestão fiscal</span>
          </div>
        </div>
        <span className={`sidebar-role-badge ${admin ? "sidebar-role-badge--admin" : "sidebar-role-badge--user"}`}>
          {admin ? "Administrador" : "Usuário"}
        </span>
      </div>
      <AppMenu />
    </aside>
  );
}
