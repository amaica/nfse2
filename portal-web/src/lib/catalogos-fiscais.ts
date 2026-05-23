export const CNAE_SUGESTOES = [
  { codigo: "6201500", label: "6201-5/00 — Desenvolvimento de programas sob encomenda" },
  { codigo: "6202300", label: "6202-3/00 — Desenvolvimento de software sob encomenda" },
  { codigo: "6203100", label: "6203-1/00 — Desenvolvimento de software customizável" },
  { codigo: "6204000", label: "6204-0/00 — Consultoria em TI" },
  { codigo: "6311900", label: "6311-9/00 — Tratamento de dados" },
];

export const NBS_SUGESTOES = [
  { codigo: "114061100", label: "114061100 — Serviços de TI" },
  { codigo: "114061200", label: "114061200 — Licenciamento de software" },
  { codigo: "114051000", label: "114051000 — Consultoria" },
];

export const CST_PIS_COFINS = [
  { value: "00", label: "00 — Nenhum" },
  { value: "01", label: "01 — Operação tributável (alíquota básica)" },
  { value: "06", label: "06 — Operação sem incidência" },
  { value: "07", label: "07 — Operação isenta" },
  { value: "08", label: "08 — Sem retenção" },
];

export const RESPONSAVEL_RETENCAO_ISS = [
  { value: "1", label: "Prestador" },
  { value: "2", label: "Cliente" },
  { value: "3", label: "Intermediário" },
];

export function filtrarCatalogo<T extends { codigo: string; label: string }>(
  lista: T[],
  termo: string,
): T[] {
  const t = termo.trim().toLowerCase();
  if (!t) return lista.slice(0, 6);
  return lista.filter((i) => i.codigo.includes(t) || i.label.toLowerCase().includes(t)).slice(0, 8);
}
