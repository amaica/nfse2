import { api } from "./api";

export type CepResponse = {
  cep: string;
  logradouro: string;
  bairro: string;
  localidade: string;
  uf: string;
  ibge?: string;
  latitude?: string;
  longitude?: string;
};

export async function consultarCep(cep: string): Promise<CepResponse> {
  const digits = cep.replace(/\D/g, "");
  const res = await fetch(`/api/cep?cep=${encodeURIComponent(digits)}`);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error((data as { error?: string }).error ?? "Falha ao consultar CEP");
  }
  return data as CepResponse;
}

export async function consultarCnpjPessoa(cnpj: string) {
  return api.consultarCnpjPublico(cnpj);
}
