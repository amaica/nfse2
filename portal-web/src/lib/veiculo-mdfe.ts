/** Tipos oficiais MDFe / NF-e (modal rodoviário). */
export const TIPOS_RODADO: Array<{ codigo: string; descricao: string }> = [
  { codigo: "01", descricao: "Truck" },
  { codigo: "02", descricao: "Toco" },
  { codigo: "03", descricao: "Cavalo mecânico" },
  { codigo: "04", descricao: "VAN" },
  { codigo: "05", descricao: "Utilitário" },
  { codigo: "06", descricao: "Outros" },
];

export const TIPOS_CARROCERIA: Array<{ codigo: string; descricao: string }> = [
  { codigo: "00", descricao: "Não aplicável" },
  { codigo: "01", descricao: "Aberta" },
  { codigo: "02", descricao: "Fechada / baú" },
  { codigo: "03", descricao: "Granelera" },
  { codigo: "04", descricao: "Porta-container" },
  { codigo: "05", descricao: "Sider" },
];

export function normalizarPlaca(raw?: string | null): string {
  return (raw ?? "").replace(/[^A-Za-z0-9]/g, "").toUpperCase().slice(0, 7);
}

export function normalizarCodigoMdfe(raw?: string | null): string {
  const t = (raw ?? "").trim();
  if (!t) return "";
  const d = t.replace(/\D/g, "");
  if (d) return d.padStart(2, "0").slice(-2);
  return t.slice(0, 2).toUpperCase();
}

function labelDe(lista: Array<{ codigo: string; descricao: string }>, codigo?: string | null): string {
  const c = normalizarCodigoMdfe(codigo);
  if (!c) return "—";
  const hit = lista.find((x) => x.codigo === c);
  return hit ? `${hit.codigo} — ${hit.descricao}` : c;
}

export function labelRodado(codigo?: string | null): string {
  return labelDe(TIPOS_RODADO, codigo);
}

export function labelCarroceria(codigo?: string | null): string {
  return labelDe(TIPOS_CARROCERIA, codigo);
}

export function opcoesComValorAtual(
  lista: Array<{ codigo: string; descricao: string }>,
  atual?: string | null,
): Array<{ codigo: string; descricao: string }> {
  const c = normalizarCodigoMdfe(atual);
  if (!c || lista.some((x) => x.codigo === c)) return lista;
  return [{ codigo: c, descricao: "Valor do cadastro" }, ...lista];
}
