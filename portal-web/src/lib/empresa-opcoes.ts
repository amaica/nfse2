export const CRT_OPCOES = [
  { value: "1", label: "Simples Nacional" },
  { value: "2", label: "Simples — excesso de sublimite" },
  { value: "3", label: "Regime normal" },
];

export const AMBIENTE_OPCOES = [
  { value: "homologacao", label: "Homologação" },
  { value: "producao", label: "Produção" },
];

export const MODELO_DFE_OPCOES = [
  { value: "NFE", label: "NF-e — nota fiscal eletrônica" },
  { value: "NFCE", label: "NFC-e — consumidor" },
];

export function labelCrt(valor?: string | null, optanteSimples?: boolean | null): string {
  const v = (valor ?? "").trim();
  if (v) return CRT_OPCOES.find((o) => o.value === v)?.label ?? v;
  if (optanteSimples === true) return "Simples Nacional";
  if (optanteSimples === false) return "Regime normal";
  return "—";
}

export function labelAmbiente(valor?: string | null): string {
  const v = (valor ?? "").trim().toLowerCase();
  if (!v) return "—";
  return AMBIENTE_OPCOES.find((o) => o.value === v)?.label ?? valor ?? "—";
}
