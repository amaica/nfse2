"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import {
  clearAppSession,
  getAppSession,
  getAppToken,
  isOnboardingSession,
} from "@/lib/app-session";
import { OnboardingWizard } from "./OnboardingWizard";

export default function OnboardingPage() {
  const router = useRouter();
  const [booting, setBooting] = useState(true);
  const session = getAppSession();
  const token = getAppToken();

  useEffect(() => {
    if (!token || !session) {
      router.replace("/login");
      return;
    }
    if (!isOnboardingSession()) {
      router.replace("/painel");
      return;
    }
    setBooting(false);
  }, [router, token, session]);

  if (booting || !token || !session) {
    return <div className="app-loading">Carregando…</div>;
  }

  return (
    <div className="min-h-screen bg-[var(--surface-ground)]">
      <div className="flex min-h-screen flex-col items-center justify-center px-4 py-10">
        <div className="mb-6 flex items-center gap-2">
          <Image src="/images/logo.png" alt="SyncNota" width={140} height={32} />
        </div>
        <OnboardingWizard token={token} contaNome={session.contaNome} />
        <button
          type="button"
          className="mt-6 text-sm text-agro-muted hover:text-[var(--primary-600)]"
          onClick={() => {
            clearAppSession();
            router.push("/login");
          }}
        >
          Sair e usar outra conta
        </button>
      </div>
    </div>
  );
}
