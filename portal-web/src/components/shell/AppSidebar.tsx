"use client";

import Image from "next/image";
import { AppMenu } from "./AppMenu";

export function AppSidebar() {
  return (
    <aside className="layout-sidebar">
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <Image src="/images/logo.png" alt="SyncNota" width={120} height={28} className="sidebar-brand__logo" />
          <span className="sidebar-brand__text">SyncNota</span>
        </div>
      </div>
      <AppMenu />
    </aside>
  );
}
