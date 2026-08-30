"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { hasPortalAccess, isGestaoSession, isOnboardingSession } from "@/lib/app-session";
import { isGestaoRoute } from "@/lib/menu-config";
import { AppSidebar } from "./AppSidebar";
import { AppTopbar } from "./AppTopbar";

export function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (!hasPortalAccess()) {
      router.replace("/login");
      return;
    }
    if (isOnboardingSession()) {
      router.replace("/onboarding");
      return;
    }
    setReady(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- checagem única na montagem
  }, []);

  useEffect(() => {
    if (!ready) return;
    if (isGestaoRoute(pathname) && !isGestaoSession()) {
      router.replace("/painel");
    }
  }, [ready, pathname, router]);

  if (!ready) {
    return <div className="app-loading">Carregando…</div>;
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
        <main className="layout-content">
          <div className="app-page-container">{children}</div>
        </main>
      </div>
    </div>
  );
}
