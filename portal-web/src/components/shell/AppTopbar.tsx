"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import {
  Building2,
  ChevronDown,
  CreditCard,
  FileText,
  LogOut,
  Menu,
  User,
  Users,
} from "lucide-react";
import { clearAppSession, clearAdminKey } from "@/lib/app-session";
import { useAppSession } from "@/hooks/useAppSession";
import { AppBreadcrumb } from "./AppBreadcrumb";
import { EmpresaSwitcher } from "./EmpresaSwitcher";

type Props = {
  onToggleMenu: () => void;
  onToggleSidebar: () => void;
  sidebarCollapsed: boolean;
};

export function AppTopbar({ onToggleMenu, onToggleSidebar }: Props) {
  const router = useRouter();
  const [perfilAberto, setPerfilAberto] = useState(false);
  const perfilRef = useRef<HTMLDivElement>(null);
  const { session } = useAppSession();

  useEffect(() => {
    if (!perfilAberto) return;
    function fora(e: MouseEvent) {
      if (!perfilRef.current?.contains(e.target as Node)) setPerfilAberto(false);
    }
    document.addEventListener("mousedown", fora);
    return () => document.removeEventListener("mousedown", fora);
  }, [perfilAberto]);

  function logout() {
    clearAppSession();
    clearAdminKey();
    router.push("/login");
  }

  return (
    <header className="layout-topbar">
      <div className="topbar-start flex items-center gap-2">
        <button type="button" className="topbar-menubutton" onClick={onToggleMenu} aria-label="Menu">
          <Menu size={18} />
        </button>
        <button
          type="button"
          className="topbar-menubutton hidden lg:inline-flex"
          onClick={onToggleSidebar}
          aria-label="Recolher menu"
        >
          <Menu size={18} />
        </button>
        <Link href="/painel" className="saas-brand hidden sm:flex">
          <span className="saas-brand__icon">
            <FileText className="h-4 w-4" />
          </span>
          <span className="saas-brand__title">SyncNota</span>
        </Link>
        <div className="topbar-breadcrumb hidden md:block">
          <AppBreadcrumb />
        </div>
      </div>
      <div className="topbar-end flex shrink-0 items-center gap-2">
        <EmpresaSwitcher compact />
        <button
          type="button"
          onClick={logout}
          className="topbar-logout-btn"
          title="Sair"
          aria-label="Sair da conta"
        >
          <LogOut className="h-4 w-4" />
          <span className="hidden md:inline">Sair</span>
        </button>
        <div className="relative" ref={perfilRef}>
          <button
            type="button"
            onClick={() => setPerfilAberto((v) => !v)}
            className="empresa-switcher-btn max-w-[180px]"
            aria-expanded={perfilAberto}
          >
            <User className="h-4 w-4 shrink-0 text-[var(--brand)]" />
            <span className="min-w-0 truncate">{session?.nome ?? "Conta"}</span>
            <ChevronDown className="h-4 w-4 shrink-0 text-agro-muted" />
          </button>
          {perfilAberto && (
            <div className="absolute right-0 top-full z-[100001] mt-2 w-56 rounded-xl border border-[var(--border)] bg-white py-1 shadow-xl">
              <div className="border-b border-[var(--border)] px-3 py-2">
                <p className="truncate text-sm font-medium text-agro-body">{session?.nome ?? "Conta"}</p>
                <p className="truncate text-xs text-agro-muted">{session?.email ?? ""}</p>
              </div>
              <Link
                href="/conta/metricas"
                className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-[var(--primary-50)]"
                onClick={() => setPerfilAberto(false)}
              >
                <CreditCard className="h-4 w-4" /> Métricas
              </Link>
              <Link
                href="/conta/assinatura"
                className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-[var(--primary-50)]"
                onClick={() => setPerfilAberto(false)}
              >
                <CreditCard className="h-4 w-4" /> Assinatura
              </Link>
              <Link
                href="/cadastros/usuarios"
                className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-[var(--primary-50)]"
                onClick={() => setPerfilAberto(false)}
              >
                <Users className="h-4 w-4" /> Usuários
              </Link>
              <Link
                href="/cadastros/empresa"
                className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-[var(--primary-50)]"
                onClick={() => setPerfilAberto(false)}
              >
                <Building2 className="h-4 w-4" /> Emitentes
              </Link>
              <button
                type="button"
                onClick={logout}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-rose-700 hover:bg-rose-50"
              >
                <LogOut className="h-4 w-4" /> Sair
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
