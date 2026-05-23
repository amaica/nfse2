export type TomadorSalvo = {
  documento: string;
  razaoSocial: string;
  email?: string;
  telefone?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  bairro?: string;
  cidade?: string;
  uf?: string;
  codigoMunicipioIbge?: string;
};

const KEY = "synki-nfse-tomadores";

export function listarTomadores(): TomadorSalvo[] {
  if (typeof window === "undefined") return [];
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? "[]") as TomadorSalvo[];
  } catch {
    return [];
  }
}

export function salvarTomador(t: TomadorSalvo) {
  const doc = t.documento.replace(/\D/g, "");
  const lista = listarTomadores().filter((x) => x.documento.replace(/\D/g, "") !== doc);
  lista.unshift({ ...t, documento: doc });
  localStorage.setItem(KEY, JSON.stringify(lista.slice(0, 20)));
}

export async function buscarTomadorPorDocumento(documento: string): Promise<Partial<TomadorSalvo> | null> {
  const doc = documento.replace(/\D/g, "");
  const local = listarTomadores().find((t) => t.documento.replace(/\D/g, "") === doc);
  if (local) return local;

  if (doc.length === 14) {
    try {
      const res = await fetch(`https://brasilapi.com.br/api/cnpj/v1/${doc}`);
      if (!res.ok) return null;
      const data = (await res.json()) as {
        razao_social?: string;
        nome_fantasia?: string;
        email?: string | null;
        ddd_telefone_1?: string;
        cep?: string;
        logradouro?: string;
        numero?: string;
        bairro?: string;
        municipio?: string;
        uf?: string;
        codigo_municipio_ibge?: number;
      };
      return {
        documento: doc,
        razaoSocial: data.razao_social ?? data.nome_fantasia ?? "",
        email: data.email ?? undefined,
        telefone: data.ddd_telefone_1 ?? undefined,
        cep: data.cep?.replace(/\D/g, ""),
        logradouro: data.logradouro,
        numero: data.numero,
        bairro: data.bairro,
        cidade: data.municipio,
        uf: data.uf,
        codigoMunicipioIbge: data.codigo_municipio_ibge?.toString(),
      };
    } catch {
      return null;
    }
  }
  return null;
}
