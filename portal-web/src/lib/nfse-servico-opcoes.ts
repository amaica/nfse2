export type OpcaoCodigo = { value: string; label: string };

export const TRIBUTACAO_ISSQN: OpcaoCodigo[] = [
  { value: "1", label: "Operação tributável" },
  { value: "2", label: "Imunidade" },
  { value: "3", label: "Exportação" },
  { value: "4", label: "Não incidência" },
  { value: "5", label: "Isento" },
];

export const ISS_RETIDO: OpcaoCodigo[] = [
  { value: "1", label: "Não retido" },
  { value: "2", label: "Retido pelo tomador" },
];

export const SIMPLES_NACIONAL: OpcaoCodigo[] = [
  { value: "1", label: "Não optante" },
  { value: "2", label: "ME / EPP" },
  { value: "3", label: "MEI" },
];

export const REGIME_ESPECIAL: OpcaoCodigo[] = [
  { value: "0", label: "Nenhum" },
  { value: "1", label: "Microempresa municipal" },
  { value: "2", label: "Estimativa" },
  { value: "3", label: "Sociedade de profissionais" },
  { value: "4", label: "Cooperativa" },
  { value: "5", label: "MEI" },
  { value: "6", label: "ME / EPP" },
];

export const CST_PIS_COFINS: OpcaoCodigo[] = [
  { value: "01", label: "Tributável — alíquota básica" },
  { value: "06", label: "Sem incidência" },
  { value: "07", label: "Isento" },
  { value: "08", label: "Sem retenção" },
];

export function labelOpcao(opcoes: OpcaoCodigo[], valor?: string | null, vazio = "—"): string {
  const v = (valor ?? "").trim();
  if (!v) return vazio;
  return opcoes.find((o) => o.value === v)?.label ?? v;
}

export function fmtAliquota(n?: number | null): string {
  if (n == null || Number.isNaN(Number(n))) return "—";
  return `${Number(n).toLocaleString("pt-BR", { maximumFractionDigits: 4 })}%`;
}
