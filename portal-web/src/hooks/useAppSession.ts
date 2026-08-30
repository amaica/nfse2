"use client";

import { useEffect, useState } from "react";
import { getAppSession, type AppSession } from "@/lib/app-session";

const SESSION_EVENT = "portal-session-change";

/** Sessão do portal — null no SSR e no 1º render do cliente (evita hydration mismatch). */
export function useAppSession(): { session: AppSession | null; ready: boolean } {
  const [session, setSession] = useState<AppSession | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const sync = () => setSession(getAppSession());
    sync();
    setReady(true);
    window.addEventListener(SESSION_EVENT, sync);
    return () => window.removeEventListener(SESSION_EVENT, sync);
  }, []);

  return { session, ready };
}
