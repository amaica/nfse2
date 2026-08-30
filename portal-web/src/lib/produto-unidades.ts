/** Unidades comerciais usadas no Fluxo / NF-e (SIGLA → descrição). */
export const UNIDADES_PRODUTO: Array<{ sigla: string; descricao: string }> = [
  { sigla: "UN", descricao: "Unidade" },
  { sigla: "KG", descricao: "Quilograma" },
  { sigla: "CBS", descricao: "Cabeças" },
  { sigla: "CX", descricao: "Caixa" },
  { sigla: "VD", descricao: "Vidro" },
  { sigla: "MT", descricao: "Metro" },
  { sigla: "M2", descricao: "Metro quadrado" },
  { sigla: "M3", descricao: "Metro cúbico" },
  { sigla: "LT", descricao: "Litro" },
  { sigla: "PÇ", descricao: "Peça" },
  { sigla: "JG", descricao: "Jogo" },
  { sigla: "GR", descricao: "Grama" },
  { sigla: "KT", descricao: "Kit" },
  { sigla: "RL", descricao: "Rolo" },
  { sigla: "PT", descricao: "Pote" },
  { sigla: "FL", descricao: "Folha" },
  { sigla: "FR", descricao: "Frasco" },
  { sigla: "SC", descricao: "Saco" },
  { sigla: "CM", descricao: "Centímetro" },
  { sigla: "LA", descricao: "Lata" },
  { sigla: "DB", descricao: "Balde" },
  { sigla: "PA", descricao: "Par" },
  { sigla: "TN", descricao: "Tonelada" },
];

export function labelUnidade(sigla?: string | null): string {
  if (!sigla) return "—";
  const u = UNIDADES_PRODUTO.find((x) => x.sigla === sigla);
  return u ? `${u.sigla} — ${u.descricao}` : sigla;
}
