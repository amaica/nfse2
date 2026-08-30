const TOKEN_KEY = "portal-app-token";
const REFRESH_KEY = "portal-refresh-token";
const SESSION_KEY = "portal-app-session";
const ADMIN_KEY = "nfse-admin-key";

export type AppSession = {
  token: string;
  refreshToken?: string;
  empresaId: number;
  empresaNome?: string;
  empresaCnpj?: string;
  nome: string;
  email: string;
  papel?: string;
  contaId?: number;
  contaNome?: string;
  onboardingRequired?: boolean;
};

export function saveAppSession(session: AppSession) {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(TOKEN_KEY, session.token);
  if (session.refreshToken) {
    sessionStorage.setItem(REFRESH_KEY, session.refreshToken);
  }
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  window.dispatchEvent(new CustomEvent("portal-session-change"));
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

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(REFRESH_KEY);
}

export function clearAppSession() {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
  sessionStorage.removeItem(SESSION_KEY);
}

/** Guarda o token de sessao admin de curta duracao (nunca o NFSE_ADMIN_SECRET em si). */
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

export function isOnboardingSession(): boolean {
  const s = getAppSession();
  return !!s?.onboardingRequired || s?.empresaId === 0;
}

export function saveLoginResponse(res: {
  token: string;
  refreshToken?: string;
  empresaId: number;
  empresaNome?: string;
  empresaCnpj?: string;
  nome: string;
  email: string;
  papel?: string;
  contaId?: number;
  contaNome?: string;
  onboardingRequired?: boolean;
}) {
  saveAppSession({
    token: res.token,
    refreshToken: res.refreshToken,
    empresaId: res.empresaId,
    empresaNome: res.empresaNome,
    empresaCnpj: res.empresaCnpj,
    nome: res.nome,
    email: res.email,
    papel: res.papel,
    contaId: res.contaId,
    contaNome: res.contaNome,
    onboardingRequired: res.onboardingRequired,
  });
}

/** Sessão app (JWT) ou chave admin para o shell fiscal */
export function hasPortalAccess(): boolean {
  return isAppAuthenticated() || !!getAdminKey();
}

/** Owner ou admin da conta — menu e rotas de gestão. */
export function isGestaoSession(): boolean {
  const papel = getAppSession()?.papel;
  return papel === "OWNER" || papel === "ADMIN";
}
