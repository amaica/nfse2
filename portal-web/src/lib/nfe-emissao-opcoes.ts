import { CODIGO_UF_IBGE } from "@/components/admin/uf-ibge";

export const FINALIDADES_NFE = [
  { value: "1", label: "1 — NF-e normal" },
  { value: "2", label: "2 — NF-e complementar" },
  { value: "3", label: "3 — NF-e de ajuste" },
  { value: "4", label: "4 — Devolução de mercadoria" },
] as const;

export const CONSUMIDOR_FINAL = [
  { value: "0", label: "Normal" },
  { value: "1", label: "Consumidor final" },
] as const;

export const PRESENCA_COMPRADOR = [
  { value: "0", label: "Não se aplica" },
  { value: "1", label: "Presencial" },
  { value: "2", label: "Não presencial — internet" },
  { value: "3", label: "Não presencial — teleatendimento" },
  { value: "5", label: "Presencial fora do estabelecimento" },
  { value: "9", label: "Não presencial — outros" },
] as const;

export const MEIOS_PAGAMENTO = [
  { value: "01", label: "01 — Dinheiro" },
  { value: "02", label: "02 — Cheque" },
  { value: "03", label: "03 — Cartão de crédito" },
  { value: "04", label: "04 — Cartão de débito" },
  { value: "05", label: "05 — Crédito loja" },
  { value: "15", label: "15 — Boleto bancário" },
  { value: "16", label: "16 — Depósito" },
  { value: "17", label: "17 — PIX" },
  { value: "18", label: "18 — Transferência" },
  { value: "20", label: "20 — PIX estático" },
  { value: "90", label: "90 — Sem pagamento" },
  { value: "99", label: "99 — Outros" },
] as const;

export const PRAZOS_PAGAMENTO = [
  { value: "0", label: "À vista" },
  { value: "1", label: "A prazo" },
] as const;

export const MODALIDADES_FRETE = [
  { value: "0", label: "0 — Por conta do emitente (CIF)" },
  { value: "1", label: "1 — Por conta do destinatário (FOB)" },
  { value: "2", label: "2 — Por conta de terceiros" },
  { value: "3", label: "3 — Transporte próprio do emitente" },
  { value: "4", label: "4 — Transporte próprio do destinatário" },
  { value: "9", label: "9 — Sem ocorrência de transporte" },
] as const;

export const MODELOS_NFP = [
  { value: "04", label: "04 — NF produtor rural" },
  { value: "01", label: "01 — NF modelo 1/1A" },
] as const;

export const UFS_IBGE = Object.entries(CODIGO_UF_IBGE)
  .sort(([a], [b]) => a.localeCompare(b))
  .map(([sigla, ibge]) => ({ sigla, ibge }));

export function parseChaveNfe(chave: string) {
  const d = chave.replace(/\D/g, "");
  if (d.length !== 44) return null;
  return {
    chave: d,
    codigoUf: d.slice(0, 2),
    anoMes: d.slice(2, 6),
    cnpj: d.slice(6, 20),
    modelo: d.slice(20, 22),
    serie: String(Number(d.slice(22, 25))),
    numero: String(Number(d.slice(25, 34))),
  };
}

export function fmtAamm(aamm: string) {
  const d = aamm.replace(/\D/g, "");
  if (d.length !== 4) return aamm;
  return `${d.slice(0, 2)}/${d.slice(2)}`;
}
