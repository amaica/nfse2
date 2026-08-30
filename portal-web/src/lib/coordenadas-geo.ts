/** Normaliza valor (troca vírgula por ponto, trim). */
export function normalizarCoord(v: string): string {
  return (v ?? "").trim().replace(",", ".");
}

export function isValidLat(v: string): boolean {
  const n = normalizarCoord(v);
  if (!n) return false;
  const d = Number(n);
  return !Number.isNaN(d) && d >= -90 && d <= 90;
}

export function isValidLng(v: string): boolean {
  const n = normalizarCoord(v);
  if (!n) return false;
  const d = Number(n);
  return !Number.isNaN(d) && d >= -180 && d <= 180;
}
