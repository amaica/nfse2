const STORAGE_PREFIX = "nfse-embed-token";

export function storageKey(cnpj?: string) {
  const doc = cnpj?.replace(/\D/g, "") ?? "";
  return doc ? `${STORAGE_PREFIX}-${doc}` : STORAGE_PREFIX;
}

export function salvarTokenEmbed(token: string, cnpj?: string) {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(storageKey(cnpj), token);
}

export function lerTokenEmbed(cnpj?: string): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(storageKey(cnpj));
}

export function limparTokenEmbed(cnpj?: string) {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(storageKey(cnpj));
}
