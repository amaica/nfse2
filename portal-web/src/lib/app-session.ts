const TOKEN_KEY = "portal-app-token";
const SESSION_KEY = "portal-app-session";
const ADMIN_KEY = "nfse-admin-key";

export type AppSession = {
  token: string;
  empresaId: number;
  empresaNome: string;
  empresaCnpj: string;
  nome: string;
  email: string;
};

export function saveAppSession(session: AppSession) {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(TOKEN_KEY, session.token);
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function getAppSession(): AppSession | null {
  if (typeof window === "undefined") return null;
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AppSession;
  } catch {
    return null;
  }
}

export function getAppToken(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(TOKEN_KEY);
}

export function clearAppSession() {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(SESSION_KEY);
}

export function saveAdminKey(key: string) {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(ADMIN_KEY, key);
}

export function getAdminKey(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(ADMIN_KEY);
}

export function clearAdminKey() {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(ADMIN_KEY);
}

export function isAppAuthenticated(): boolean {
  return !!getAppToken() && !!getAppSession();
}

/** Sessão app (JWT) ou chave admin para o shell fiscal */
export function hasPortalAccess(): boolean {
  return isAppAuthenticated() || !!getAdminKey();
}
