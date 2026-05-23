export type NbsSalvo = { codigo: string; codigoNacional: string; label: string };

const KEY = "nfse_nbs_recentes";
const MAX = 8;

export function listarNbsRecentes(): NbsSalvo[] {
  if (typeof window === "undefined") return [];
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? "[]") as NbsSalvo[];
  } catch {
    return [];
  }
}

export function salvarNbsRecente(item: NbsSalvo): void {
  const lista = listarNbsRecentes().filter((x) => x.codigoNacional !== item.codigoNacional);
  lista.unshift(item);
  localStorage.setItem(KEY, JSON.stringify(lista.slice(0, MAX)));
}
