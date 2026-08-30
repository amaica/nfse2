import type { LoginResponse } from "./api";

export type { LoginResponse };

export const PORTAL_EMPRESA_EVENT = "portal-empresa-alterada";

export function dispatchEmpresaAlterada(res: LoginResponse) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent<LoginResponse>(PORTAL_EMPRESA_EVENT, { detail: res }));
}
