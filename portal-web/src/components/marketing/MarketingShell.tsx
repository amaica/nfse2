"use client";

import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";

type Props = {
  children: ReactNode;
};

export function MarketingShell({ children }: Props) {
  return (
    <div className="marketing-shell min-h-screen">
      <div className="login-bg" aria-hidden>
        <div className="login-bg__base" />
        <div className="login-bg__orb login-bg__orb--1" />
        <div className="login-bg__orb login-bg__orb--2" />
        <div className="login-bg__orb login-bg__orb--3" />
      </div>

      <header className="relative z-10 mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <Link href="/" className="flex items-center gap-2">
          <Image src="/images/logo.png" alt="SyncNota" width={140} height={32} priority />
        </Link>
        <nav className="flex items-center gap-2 sm:gap-4">
          <Link href="/precos" className="btn-ghost hidden text-sm sm:inline-flex">
            Planos
          </Link>
          <Link href="/login" className="btn-ghost text-sm">
            Entrar
          </Link>
          <Link href="/registrar" className="fiscal-btn-primary text-sm">
            Começar grátis
          </Link>
        </nav>
      </header>

      <main className="relative z-10">{children}</main>

      <footer className="relative z-10 border-t border-[var(--border)] bg-white/60 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-6 py-10 sm:flex-row">
          <p className="text-sm text-agro-muted">© SyncNota · Synki — gestão fiscal para o agro</p>
          <nav className="flex flex-wrap justify-center gap-4 text-sm">
            <Link href="/precos" className="link-agro">
              Planos
            </Link>
            <Link href="/termos" className="link-agro">
              Termos
            </Link>
            <Link href="/privacidade" className="link-agro">
              Privacidade
            </Link>
            <Link href="/login" className="link-agro">
              Suporte
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}
