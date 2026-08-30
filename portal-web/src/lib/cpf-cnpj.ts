/** Validação CPF/CNPJ — espelho do cadastro de pessoas do AgrowFront. */

const CPF_LENGTH = 11;
const CNPJ_LENGTH = 14;

const CPF_INVALIDOS = new Set([
  "00000000000",
  "11111111111",
  "22222222222",
  "33333333333",
  "44444444444",
  "55555555555",
  "66666666666",
  "77777777777",
  "88888888888",
  "99999999999",
]);

const CNPJ_INVALIDOS = new Set([
  "00000000000000",
  "11111111111111",
  "22222222222222",
  "33333333333333",
  "44444444444444",
  "55555555555555",
  "66666666666666",
  "77777777777777",
  "88888888888888",
  "99999999999999",
]);

export function apenasNumeros(doc: string): string {
  return (doc ?? "").replace(/\D/g, "");
}

export function cpfValido(cpf: string): boolean {
  const n = apenasNumeros(cpf);
  if (n.length !== CPF_LENGTH || CPF_INVALIDOS.has(n)) return false;
  let sm = 0;
  let peso = 10;
  for (let i = 0; i < 9; i++) {
    sm += Number.parseInt(n[i], 10) * peso;
    peso -= 1;
  }
  let r = 11 - (sm % 11);
  const dig10 = r >= 10 ? 0 : r;
  if (dig10 !== Number.parseInt(n[9], 10)) return false;
  sm = 0;
  peso = 11;
  for (let i = 0; i < 10; i++) {
    sm += Number.parseInt(n[i], 10) * peso;
    peso -= 1;
  }
  r = 11 - (sm % 11);
  const dig11 = r >= 10 ? 0 : r;
  return dig11 === Number.parseInt(n[10], 10);
}

export function cnpjValido(cnpj: string): boolean {
  const n = apenasNumeros(cnpj).padStart(CNPJ_LENGTH, "0");
  if (n.length !== CNPJ_LENGTH || CNPJ_INVALIDOS.has(n)) return false;
  const peso1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const peso2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  let sm = 0;
  for (let i = 0; i < 12; i++) sm += Number.parseInt(n[i], 10) * peso1[i];
  let r = sm % 11;
  const dig13 = r === 0 || r === 1 ? 0 : 11 - r;
  if (dig13 !== Number.parseInt(n[12], 10)) return false;
  sm = 0;
  for (let i = 0; i < 13; i++) sm += Number.parseInt(n[i], 10) * peso2[i];
  r = sm % 11;
  const dig14 = r === 0 || r === 1 ? 0 : 11 - r;
  return dig14 === Number.parseInt(n[13], 10);
}
