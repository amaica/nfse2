const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}

async function request<T>(
  path: string,
  token: string,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

async function fetchPdfBlob(path: string, token: string): Promise<Blob> {
  const res = await fetch(`${API_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
  }
  return res.blob();
}

async function download(path: string, token: string, filename: string): Promise<void> {
  const blob = await fetchPdfBlob(path, token);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export const api = {
  validateEmbed: (t: string) =>
    fetch(`${API_URL}/api/auth/embed/validate?t=${encodeURIComponent(t)}`).then((r) =>
      r.ok ? r.json() : Promise.reject(new Error("Token invalido")),
    ),

  config: (token: string) => request<ConfigNfse>("/api/nfse/config", token),

  emissaoContexto: (token: string) =>
    request<EmissaoContexto>("/api/nfse/emissao/contexto", token),

  convenio: (token: string, ibge: string) =>
    request<ConvenioResponse>(`/api/nfse/convenio/${ibge}`, token),

  aliquota: (token: string, ibge: string, servico: string) =>
    request<{ aliquota: number }>(
      `/api/nfse/aliquota?codigoMunicipio=${encodeURIComponent(ibge)}&codigoServico=${encodeURIComponent(servico)}`,
      token,
    ),

  buscarCnae: (token: string, termo = "", limite = 40) => {
    const params = new URLSearchParams({ termo, limite: String(limite) });
    return request<CnaeResponse>(`/api/nfse/cnae?${params}`, token);
  },

  buscarNbs: (token: string, termo = "", limite = 40, lc116?: string) => {
    const params = new URLSearchParams({ termo, limite: String(limite) });
    if (lc116) params.set("lc116", lc116);
    return request<NbsResponse>(`/api/nfse/nbs?${params}`, token);
  },

  buscarServicos: (token: string, termo = "", limite = 400, grupo?: string) => {
    const params = new URLSearchParams({ termo, limite: String(limite) });
    if (grupo && grupo !== "todos") params.set("grupo", grupo);
    return request<ServicosLc116Response>(
      `/api/nfse/servicos?${params}`,
      token,
    );
  },

  consulta: (token: string, chave: string) =>
    request<unknown>(`/api/nfse/consulta/${chave}`, token),

  downloadPdf: (token: string, chave: string) =>
    download(`/api/nfse/pdf/${chave}`, token, "nfse.pdf"),

  imprimirPdf: async (token: string, chave: string) => {
    const blob = await fetchPdfBlob(`/api/nfse/pdf/${chave}?inline=true`, token);
    const url = URL.createObjectURL(blob);
    const w = window.open(url, "_blank", "noopener,noreferrer");
    if (!w) {
      URL.revokeObjectURL(url);
      throw new ApiError("Permita pop-ups para imprimir o PDF.", 0);
    }
    w.addEventListener("load", () => {
      try {
        w.print();
      } catch {
        /* o usuário pode usar Ctrl+P na aba do PDF */
      }
    });
    window.setTimeout(() => URL.revokeObjectURL(url), 120_000);
  },

  downloadXml: async (token: string, chave: string) => {
    const res = await fetch(`${API_URL}/api/nfse/xml/${chave}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
    }
    const xml = await res.text();
    const blob = new Blob([xml], { type: "application/xml" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "nfse.xml";
    a.click();
    URL.revokeObjectURL(url);
  },

  historico: (token: string) => request<LogItem[]>("/api/nfse/historico", token),

  emitir: (token: string, body: EmissaoBody) =>
    request<EmissaoSucesso>("/api/nfse/emitir", token, {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

export type CnaeItem = {
  codigo: string;
  codigoFormatado: string;
  descricao: string;
  label: string;
};

export type CnaeResponse = {
  total: number;
  exibidos: number;
  itens: CnaeItem[];
};

export type NbsItem = {
  codigo: string;
  codigoNacional: string;
  descricao: string;
  label: string;
};

export type NbsResponse = {
  total: number;
  exibidos: number;
  itens: NbsItem[];
};

export type ServicoLc116Item = {
  codigo: string;
  codigoNacional: string;
  descricao: string;
  label: string;
  grupos?: string[];
};

export type ServicosLc116Response = {
  total: number;
  totalAgro: number;
  totalMecanico: number;
  exibidos: number;
  grupo: string;
  itens: ServicoLc116Item[];
};

export type EmissaoBody = import("@/types/emissao-form").EmissaoPayload;

export type EmissaoContexto = {
  empresaNome: string;
  prestadorNome: string;
  prestadorDocumento: string;
  prestadorPessoaFisica: boolean;
  prefeitura: string;
  codigoMunicipioIbge: string;
  ambiente: string;
  certificadoCadastrado: boolean;
  podeEmitir: boolean;
  codigoServicoPadrao: string;
  descricaoServicoPadrao: string;
  aliquotaPadraoPercentual?: number | null;
  aviso?: string;
};

export type EmissaoSucesso = {
  sucesso: boolean;
  chaveAcesso: string;
  idDps: string;
  processadoEm: string;
};

export type ConfigNfse = {
  prefeitura: string;
  codigoMunicipioIbge: string;
  ambiente: string;
  certificadoCadastrado?: boolean;
};

export type ConvenioResponse = {
  aderenteAmbienteNacional?: boolean;
  aderenteEmissorNacional?: boolean;
};

export type LogItem = {
  id: number;
  acao: string;
  descricao: string;
  createdAt: string;
};

export function formatarCnpjCpf(doc: string): string {
  const d = doc.replace(/\D/g, "");
  if (d.length === 14) {
    return d.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, "$1.$2.$3/$4-$5");
  }
  if (d.length === 11) {
    return d.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, "$1.$2.$3-$4");
  }
  return doc;
}
