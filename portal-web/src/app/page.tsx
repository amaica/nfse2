"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { LandingContent } from "@/components/marketing/LandingContent";
import { hasPortalAccess, isOnboardingSession } from "@/lib/app-session";

export default function LandingPage() {
  const router = useRouter();
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (hasPortalAccess()) {
      router.replace(isOnboardingSession() ? "/onboarding" : "/painel");
      return;
    }
    setShow(true);
  }, [router]);

  if (!show) {
    return <div className="app-loading min-h-screen">Carregando…</div>;
  }

  return <LandingContent />;
}
