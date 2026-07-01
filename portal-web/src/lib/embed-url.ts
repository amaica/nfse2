/** Lê senha da query sem perder caracteres após # (fragmento do navegador). */
export function senhaDaUrl(href?: string): string {
  if (typeof window === "undefined" && !href) return "";
  const url = href ?? window.location.href;

  const match = url.match(/[?&](?:senha|password)=([^&]*)/);
  if (!match) return "";

  const paramStart = url.indexOf(match[0]);
  const valueStart = url.indexOf("=", paramStart) + 1;
  const nextAmp = url.indexOf("&", valueStart);
  const queryEnd = nextAmp >= 0 ? nextAmp : url.length;

  let raw = url.slice(valueStart, queryEnd);
  if (nextAmp < 0) {
    const hashIdx = url.indexOf("#", valueStart);
    if (hashIdx >= 0) {
      raw += url.slice(hashIdx);
    }
  }

  try {
    return decodeURIComponent(raw.replace(/\+/g, " "));
  } catch {
    return raw.replace(/\+/g, " ");
  }
}
