/** CFOP da NF-e tem 4 dígitos (1xxx–7xxx). Zero/nulo não é código fiscal. */
export function cfopValido(cfop?: number | string | null): boolean {
  const n = Number(String(cfop ?? "").replace(/\D/g, ""));
  return Number.isInteger(n) && n >= 1000 && n <= 7999;
}

export function fmtCfop(cfop?: number | string | null): string {
  if (!cfopValido(cfop)) return "";
  return String(Number(String(cfop).replace(/\D/g, ""))).padStart(4, "0");
}

export function labelOperacaoFiscal(descricao: string, cfop?: number | string | null): string {
  const codigo = fmtCfop(cfop);
  return codigo ? `${descricao} — CFOP ${codigo}` : descricao;
}
