"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { hasPortalAccess } from "@/lib/app-session";
import { AppSidebar } from "./AppSidebar";
import { AppTopbar } from "./AppTopbar";

export function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (!hasPortalAccess()) {
      router.replace("/login");
      return;
    }
    setReady(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- checagem única na montagem
  }, []);

  if (!ready) {
    return <div className="flex min-h-screen items-center justify-center bg-slate-100">Carregando…</div>;
  }

  const containerClass = [
    "layout-container",
    "layout-static",
    collapsed ? "layout-static-inactive" : "",
    mobileOpen ? "layout-mobile-active" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={containerClass}>
      <AppSidebar />
      <div className="layout-content-wrapper">
        <AppTopbar
          onToggleMenu={() => setMobileOpen((v) => !v)}
          onToggleSidebar={() => setCollapsed((v) => !v)}
          sidebarCollapsed={collapsed}
        />
        <div className="content-breadcrumb sm:hidden">
          {/* breadcrumb mobile opcional */}
        </div>
        <main className="layout-content">{children}</main>
      </div>
    </div>
  );
}
