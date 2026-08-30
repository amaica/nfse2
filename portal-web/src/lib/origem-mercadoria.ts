/** Origem da mercadoria (ICMS / NF-e, tag orig 0–8). */
export const ORIGENS_MERCADORIA: Array<{ value: string; label: string }> = [
  { value: "0", label: "0 — Nacional" },
  { value: "1", label: "1 — Estrangeira, importação direta" },
  { value: "2", label: "2 — Estrangeira, adquirida no mercado interno" },
  { value: "3", label: "3 — Nacional, conteúdo de importação > 40%" },
  { value: "4", label: "4 — Nacional, produção conforme PPB" },
  { value: "5", label: "5 — Nacional, conteúdo de importação ≤ 40%" },
  { value: "6", label: "6 — Estrangeira, importação direta, sem similar nacional" },
  { value: "7", label: "7 — Estrangeira, mercado interno, sem similar nacional" },
  { value: "8", label: "8 — Nacional, conteúdo de importação > 70%" },
];

export function labelOrigemMercadoria(codigo?: string | null): string {
  const c = (codigo ?? "").trim();
  if (!c) return "—";
  const hit = ORIGENS_MERCADORIA.find((o) => o.value === c);
  return hit ? hit.label : c;
}

export function opcoesOrigemComValorAtual(atual?: string | null): Array<{ value: string; label: string }> {
  const c = (atual ?? "").trim();
  if (!c || ORIGENS_MERCADORIA.some((o) => o.value === c)) return ORIGENS_MERCADORIA;
  return [{ value: c, label: `${c} — valor do cadastro` }, ...ORIGENS_MERCADORIA];
}
