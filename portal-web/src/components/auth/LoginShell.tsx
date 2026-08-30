"use client";

import Image from "next/image";
import type { ReactNode } from "react";
import { LoginLeftSection } from "./LoginLeftSection";

type Props = {
  children: ReactNode;
  title?: string;
  subtitle?: string;
};

export function LoginShell({ children, title, subtitle }: Props) {
  return (
    <div className="login-shell relative min-h-screen overflow-hidden">
      <div className="login-bg" aria-hidden>
        <div className="login-bg__base" />
        <div className="login-bg__orb login-bg__orb--1" />
        <div className="login-bg__orb login-bg__orb--2" />
        <div className="login-bg__orb login-bg__orb--3" />
        <div className="login-bg__noise" />
      </div>

      <div className="relative z-10 flex min-h-screen flex-col lg:flex-row">
        <LoginLeftSection />

        <div className="flex flex-1 items-center justify-center px-4 py-10 sm:px-8 lg:px-12">
          <div className="w-full max-w-[420px]">
            <div className="mb-8 text-center lg:hidden">
              <div className="mb-5 flex justify-center">
                <Image src="/images/logo.png" alt="SyncNota" width={160} height={40} className="h-10 w-auto" />
              </div>
              <h1 className="login-headline mb-2 text-3xl font-semibold leading-tight tracking-tight">
                <span className="login-headline__line">Gestão fiscal</span>
                <span className="login-headline__accent text-3xl">para o agro</span>
              </h1>
              {subtitle && (
                <p className="text-sm font-light text-agro-muted">{subtitle}</p>
              )}
            </div>

            <div className="login-glass-card">
              <div className="w-full p-6 sm:p-8">
                {title && (
                  <h2 className="mb-1 text-center text-lg font-semibold text-agro-body">{title}</h2>
                )}
                {subtitle && (
                  <p className="mb-6 hidden text-center text-sm text-agro-muted lg:block">{subtitle}</p>
                )}
                {children}
              </div>
            </div>

            <p className="mt-8 text-center text-xs tracking-wide text-agro-muted/80">
              &copy; {new Date().getFullYear()} SyncNota · Synki
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
