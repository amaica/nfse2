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
          <Image src="/images/logo.png" alt="SyncNota" width={120} height={28} className="sidebar-brand__logo" />
          <span className="sidebar-brand__text">SyncNota</span>
        </div>
        <span className={`sidebar-role-badge ${admin ? "sidebar-role-badge--admin" : "sidebar-role-badge--user"}`}>
          {admin ? "Administrador" : "Usuário"}
        </span>
      </div>
      <AppMenu />
    </aside>
  );
}
