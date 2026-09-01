/** Máscara numérica padrão BR (milhar=. decimal=,). */

export function parseBrNumber(text: string): number | null {
  if (text == null) return null;
  let s = String(text).trim();
  if (!s) return null;
  s = s.replace(/[^\d.,-]/g, "");
  if (!s || s === "-" || s === "," || s === ".") return null;

  const hasComma = s.includes(",");
  const hasDot = s.includes(".");
  if (hasComma && hasDot) {
    s = s.replace(/\./g, "").replace(",", ".");
  } else if (hasComma) {
    s = s.replace(",", ".");
  }

  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

export function formatBrNumber(value: number | null | undefined, decimalPlaces = 2): string {
  if (value == null || !Number.isFinite(value)) return "";
  return value.toLocaleString("pt-BR", {
    minimumFractionDigits: decimalPlaces,
    maximumFractionDigits: decimalPlaces,
  });
}
