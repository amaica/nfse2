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
    const msg =
      (body as { erro?: string }).erro ??
      (body as { message?: string }).message ??
      res.statusText;
    throw new ApiError(msg || "Erro na requisição", res.status);
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
  login: async (cred: { email?: string; cnpj?: string; senha: string }) => {
    const body: Record<string, string> = { senha: cred.senha };
    if (cred.cnpj) body.cnpj = cred.cnpj.replace(/\D/g, "");
    else if (cred.email) body.email = cred.email;
    const res = await fetch(`${API_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      const errBody = await res.json().catch(() => ({}));
      throw new ApiError((errBody as { erro?: string }).erro ?? "Credenciais invalidas", res.status);
    }
    return res.json() as Promise<{
      token: string;
      empresaId: number;
      empresaNome: string;
      empresaCnpj: string;
      usuarioId: number;
      nome: string;
      email: string;
    }>;
  },

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

  obterTributNfseServico: (token: string, id: number) =>
    request<TributNfseServicoCadastro>(`/api/tribut-nfse-servico/${id}`, token),

  listarTributNfseServicos: (token: string, q?: string) => {
    const params = new URLSearchParams({ simple: "true" });
    if (q?.trim()) params.set("q", q.trim());
    return request<NonNullable<EmissaoContexto["operacoesNfse"]>>(
      `/api/tribut-nfse-servico?${params}`,
      token,
    );
  },

  buscarServicos: (token: string, termo = "", limite = 400, grupo?: string, cnaes?: string[]) => {
    const params = new URLSearchParams({ limite: String(limite) });
    if (termo) params.set("termo", termo);
    if (grupo && grupo !== "todos") params.set("grupo", grupo);
    if (cnaes?.length) params.set("cnaes", cnaes.join(","));
    return request<ServicosLc116Response>(
      `/api/nfse/servicos?${params}`,
      token,
    );
  },

  consulta: (token: string, chave: string) =>
    request<unknown>(`/api/nfse/consulta/${chave}`, token),

  downloadPdf: (token: string, chave: string) =>
    download(`/api/nfse/pdf/${chave}`, token, "nfse.pdf"),

  enviarDanfeEmail: (
    token: string,
    chave: string,
    destinatario: string,
    mensagem?: string,
  ) =>
    request<{ ok: boolean; destinatario: string; anexoXml?: boolean; pdfRetryAgendado?: boolean }>(
      `/api/nfse/pdf/${chave}/email`,
      token,
      {
      method: "POST",
      body: JSON.stringify({ destinatario, mensagem: mensagem ?? "" }),
    }),

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

  nfeContexto: (token: string) =>
    request<NfeContexto>("/api/nfe/emissao/contexto", token),

  nfeStatusServico: (token: string) =>
    request<NfeStatusServico>("/api/nfe/status-servico", token),

  nfeEnviarLote: (token: string, body?: NfeEmitirLoteBody) =>
    request<NfeEmissaoResultado>("/api/nfe/lotes/enviar", token, {
      method: "POST",
      body: JSON.stringify(body ?? {}),
    }),

  nfeConsultarLote: (token: string, recibo: string) =>
    request<NfeConsultaResultado>(`/api/nfe/lotes/${encodeURIComponent(recibo)}`, token),

  nfeConsultarNota: (token: string, chave: string) =>
    request<NfeConsultaResultado>(`/api/nfe/notas/consultar/${chave}`, token),

  nfeCancelar: (token: string, body: NfeCancelarBody) =>
    request<NfeEventoResultado>("/api/nfe/notas/cancelar", token, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  nfeInutilizar: (token: string, body: NfeInutilizarBody) =>
    request<NfeEventoResultado>("/api/nfe/notas/inutilizar", token, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  nfeCartaCorrecao: (token: string, body: NfeCartaCorrecaoBody) =>
    request<NfeEventoResultado>("/api/nfe/notas/carta-correcao", token, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  nfeContingenciaEpec: (token: string, body?: NfeContingenciaEpecBody) =>
    request<NfeEventoResultado>("/api/nfe/contingencia/epec", token, {
      method: "POST",
      body: JSON.stringify(body ?? {}),
    }),

  nfceContexto: (token: string) =>
    request<NfeContexto>("/api/nfce/emissao/contexto", token),

  nfceStatusServico: (token: string) =>
    request<NfeStatusServico>("/api/nfce/status-servico", token),

  nfceEnviarLote: (token: string, body?: NfeEmitirLoteBody) =>
    request<NfeEmissaoResultado>("/api/nfce/lotes/enviar", token, {
      method: "POST",
      body: JSON.stringify(body ?? {}),
    }),
};

async function adminRequest<T>(path: string, adminKey: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-Admin-Key": adminKey,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
  }
  return res.json() as Promise<T>;
}

export type EmpresaResumo = {
  id: number;
  nome: string;
  cnpj: string;
  ativo: boolean;
  nomeFantasia?: string;
  municipio?: string;
  uf?: string;
  cnaePrincipal?: string;
  optanteSimples?: boolean;
  certificadoCadastrado?: boolean;
  logoCadastrado?: boolean;
  prefeitura?: string;
  codigoMunicipioIbge?: string;
  ambiente?: string;
  serieRps?: string;
  ultimoNumeroNfse?: number;
  proximoNumeroNfse?: number;
  nfeSerie?: string;
  nfeUltimoNumero?: number;
  nfceSerie?: string;
  nfceUltimoNumero?: number;
  emailIntegracao?: string;
  embedUrlCnpj?: string;
  embedUrlCnpjComSenha?: string;
  embedUrlEmail?: string;
  aviso?: string;
  email?: string;
  telefone?: string;
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;
  endereco?: {
    cep?: string;
    logradouro?: string;
    numero?: string;
    complemento?: string;
    bairro?: string;
  };
  cnaePrincipalDescricao?: string;
  situacaoCadastral?: string;
  enderecos?: EnderecoEmpresa[];
};

export type EnderecoEmpresa = {
  id?: number;
  apelido: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  municipio?: string;
  uf?: string;
  codigoMunicipioIbge?: string;
  inscricaoEstadual?: string;
  serieNfe?: string;
  ultimoNumeroNfe?: number;
  proximoNumeroNfe?: number;
  principal?: boolean;
  ativo?: boolean;
};

export type EmpresaDetalhe = EmpresaResumo;

export type DadosCnpjPublico = {
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string;
  email: string;
  telefone: string;
  situacaoCadastral: string;
  optanteSimples: boolean;
  endereco: {
    cep: string;
    logradouro: string;
    numero: string;
    complemento: string;
    bairro: string;
    municipio: string;
    uf: string;
    codigoMunicipioIbge: string;
  };
  prefeitura: string;
  codigoMunicipioIbge: string;
  cnaePrincipal: { codigo: string; descricao: string };
  cnaes: Array<{ codigo: string; descricao: string; principal: boolean }>;
  sugestaoEmailIntegracao: string;
};

export const adminApi = {
  listarEmpresas: (adminKey: string) =>
    adminRequest<{ itens: EmpresaResumo[] }>("/api/admin/empresas", adminKey),

  obterEmpresaPorCnpj: (adminKey: string, cnpj: string) =>
    adminRequest<EmpresaDetalhe>(
      `/api/admin/empresas/cnpj/${cnpj.replace(/\D/g, "")}`,
      adminKey,
    ),

  obterEmpresa: (adminKey: string, id: number) =>
    adminRequest<EmpresaDetalhe>(`/api/admin/empresas/${id}`, adminKey),

  consultarCnpj: (adminKey: string, cnpj: string) =>
    adminRequest<{ ok: boolean; dados: DadosCnpjPublico }>(
      `/api/admin/cnpj/${cnpj.replace(/\D/g, "")}`,
      adminKey,
    ),

  criarEmpresa: (
    adminKey: string,
    body: {
      cnpj: string;
      nome: string;
      nomeFantasia?: string;
      email?: string;
      telefone?: string;
      inscricaoEstadual?: string;
      inscricaoMunicipal?: string;
      cep?: string;
      logradouro?: string;
      numero?: string;
      complemento?: string;
      bairro?: string;
      municipio?: string;
      uf?: string;
      cnaePrincipal?: string;
      cnaePrincipalDescricao?: string;
      optanteSimples?: boolean;
      situacaoCadastral?: string;
      prefeitura: string;
      codigoMunicipioIbge: string;
      ambiente?: string;
      serieRps?: string;
      ultimoNumeroNfse?: number;
      emailIntegracao: string;
      senhaIntegracao: string;
      usuarioNome?: string;
      serieNfe?: string;
      ultimoNumeroNfe?: number;
      serieNfce?: string;
      ultimoNumeroNfce?: number;
      enderecos?: Array<{
        id?: number;
        apelido: string;
        cep?: string;
        logradouro?: string;
        numero?: string;
        complemento?: string;
        bairro?: string;
        municipio?: string;
        uf?: string;
        codigoMunicipioIbge?: string;
        inscricaoEstadual?: string;
        serieNfe?: string;
        ultimoNumeroNfe?: number;
        principal?: boolean;
        ativo?: boolean;
      }>;
    },
  ) => adminRequest<{ ok: boolean; empresa: EmpresaResumo }>("/api/admin/empresas", adminKey, {
    method: "POST",
    body: JSON.stringify(body),
  }),

  atualizarEmpresa: (
    adminKey: string,
    id: number,
    body: Partial<{
      nome: string;
      ativo: boolean;
      nomeFantasia: string;
      email: string;
      telefone: string;
      inscricaoEstadual: string;
      inscricaoMunicipal: string;
      cep: string;
      logradouro: string;
      numero: string;
      complemento: string;
      bairro: string;
      municipio: string;
      uf: string;
      cnaePrincipal: string;
      cnaePrincipalDescricao: string;
      optanteSimples: boolean;
      prefeitura: string;
      codigoMunicipioIbge: string;
      ambiente: string;
      serieRps: string;
      ultimoNumeroNfse: number;
      serieNfe: string;
      ultimoNumeroNfe: number;
      serieNfce: string;
      ultimoNumeroNfce: number;
      senhaIntegracao: string;
      enderecos?: Array<{
        id?: number;
        apelido: string;
        cep?: string;
        logradouro?: string;
        numero?: string;
        complemento?: string;
        bairro?: string;
        municipio?: string;
        uf?: string;
        codigoMunicipioIbge?: string;
        inscricaoEstadual?: string;
        serieNfe?: string;
        ultimoNumeroNfe?: number;
        principal?: boolean;
        ativo?: boolean;
      }>;
    }>,
  ) =>
    adminRequest<{ ok: boolean; empresa: EmpresaResumo }>(`/api/admin/empresas/${id}`, adminKey, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  statusCertificado: (adminKey: string, empresaId: number) =>
    adminRequest<CertificadoStatusAdmin>(`/api/admin/empresas/${empresaId}/certificado`, adminKey),

  uploadCertificado: async (adminKey: string, empresaId: number, arquivo: File, senha: string) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    fd.append("senha", senha);
    const res = await fetch(`${API_URL}/api/admin/empresas/${empresaId}/certificado`, {
      method: "POST",
      headers: { "X-Admin-Key": adminKey },
      body: fd,
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
    }
    return res.json() as Promise<CertificadoUploadResult>;
  },

  excluirEmpresa: (adminKey: string, id: number) =>
    adminRequest<{ ok: boolean }>(`/api/admin/empresas/${id}`, adminKey, { method: "DELETE" }),

  uploadLogo: async (adminKey: string, empresaId: number, arquivo: File) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    const res = await fetch(`${API_URL}/api/admin/empresas/${empresaId}/logo`, {
      method: "POST",
      headers: { "X-Admin-Key": adminKey },
      body: fd,
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
    }
    return res.json() as Promise<{ ok: boolean; empresaId: number }>;
  },
};

/** Cadastro de empresas autenticado com JWT do portal (menu fiscal). */
export const empresaPortalApi = {
  listarEmpresas: (token: string) =>
    request<{ itens: EmpresaResumo[] }>("/api/empresas", token),

  obterEmpresa: (token: string, id: number) =>
    request<EmpresaDetalhe>(`/api/empresas/${id}`, token),

  criarEmpresa: (
    token: string,
    body: Parameters<typeof adminApi.criarEmpresa>[1],
  ) =>
    request<{ ok: boolean; empresa: EmpresaResumo }>("/api/empresas", token, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  atualizarEmpresa: (
    token: string,
    id: number,
    body: Parameters<typeof adminApi.atualizarEmpresa>[2],
  ) =>
    request<{ ok: boolean; empresa: EmpresaResumo }>(`/api/empresas/${id}`, token, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  excluirEmpresa: (token: string, id: number) =>
    request<{ ok: boolean }>(`/api/empresas/${id}`, token, { method: "DELETE" }),

  uploadCertificado: async (token: string, empresaId: number, arquivo: File, senha: string) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    fd.append("senha", senha);
    const res = await fetch(`${API_URL}/api/empresas/${empresaId}/certificado`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: fd,
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
    }
    return res.json() as Promise<CertificadoUploadResult>;
  },

  uploadLogo: async (token: string, empresaId: number, arquivo: File) => {
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    const res = await fetch(`${API_URL}/api/empresas/${empresaId}/logo`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: fd,
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
    }
    return res.json() as Promise<{ ok: boolean; empresaId: number }>;
  },
};

export type CertificadoStatusAdmin = {
  cadastrado: boolean;
  documento?: string;
  titular?: string;
  pessoaFisica?: boolean;
  podeEmitir?: boolean;
};

export type CertificadoUploadResult = {
  ok: boolean;
  empresaId: number;
  tamanhoBytes: number;
  documento: string;
  titular: string;
  pessoaFisica: boolean;
  podeEmitir: boolean;
  validade?: string;
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
  filtradoPorCnae?: boolean;
  itens: ServicoLc116Item[];
};

export type EmissaoBody = import("@/types/emissao-form").EmissaoPayload;

export type TributNfseServicoCadastro = {
  id: number;
  descricao: string;
  itemListaServico: string;
  codigoTributacaoMunicipio?: string;
  nbs?: string;
  cnae?: string;
  descricaoServico?: string;
  municipioPrestacaoIbge?: string;
  aliquotaIss?: number;
  tributacaoIssqn?: string;
  issRetido?: string;
  simplesNacional?: string;
  regimeEspecial?: string;
  cstPisCofins?: string;
  aliquotaPis?: number;
  aliquotaCofins?: number;
  habilitarRetencoes?: boolean;
  retencaoInss?: number;
  retencaoIrrf?: number;
  retencaoCsll?: number;
  ibsCbsCst?: string;
  ibsCbsClassTrib?: string;
  aliquotaIbs?: number;
  aliquotaCbs?: number;
  habilitarIbsCbs?: boolean;
};

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
  empresaCnaes?: Array<{ codigo: string; descricao: string; principal: boolean }>;
  servicosSugeridos?: ServicoLc116Item[];
  operacoesNfse?: Array<{
    id: number;
    descricao: string;
    itemListaServico: string;
    nbs: string;
    aliquotaIss: number;
    principal: boolean;
  }>;
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

export type NfeDestinatarioBody = {
  nome?: string;
  documento?: string;
  email?: string;
  inscricaoEstadual?: string;
};

export type NfeItemBody = {
  produtoId?: number;
  codigo?: string;
  descricao?: string;
  ncm?: string;
  cfop?: string;
  unidade?: string;
  quantidade?: number;
  valorUnitario?: number;
  ibsCbs?: {
    cst?: string;
    classificacaoTributaria?: string;
    aliquotaIbsUf?: number;
    aliquotaIbsMun?: number;
    aliquotaCbs?: number;
    habilitar?: boolean;
  };
};

export type NfeEmitirLoteBody = {
  enderecoId?: number;
  operacaoFiscalId?: number;
  sincrono?: boolean;
  naturezaOperacao?: string;
  destinatario?: NfeDestinatarioBody;
  itens?: NfeItemBody[];
};

export type NfeCancelarBody = {
  chave: string;
  protocolo: string;
  motivo: string;
};

export type NfeInutilizarBody = {
  ano?: number;
  serie: string;
  numeroInicial: string;
  numeroFinal: string;
  justificativa: string;
};

export type NfeCartaCorrecaoBody = {
  chave: string;
  texto: string;
  sequencial?: number;
};

export type NfeContingenciaEpecBody = {
  enderecoId?: number;
  naturezaOperacao?: string;
  destinatario?: NfeDestinatarioBody;
  itens?: NfeItemBody[];
  justificativaContingencia?: string;
};

export type NfeContexto = {
  modelo: string;
  empresaNome: string;
  empresaCnpj: string;
  emitenteNome: string;
  emitenteDocumento: string;
  emitentePessoaFisica: boolean;
  ambiente: string;
  ufEmitente: string;
  certificadoCadastrado: boolean;
  documentoHabilitado: boolean;
  serie: string;
  ultimoNumero: number;
  proximoNumero: number;
  enderecos: Array<{
    id: number;
    apelido: string;
    municipio?: string;
    uf?: string;
    codigoMunicipioIbge?: string;
    inscricaoEstadual?: string;
    serieNfe?: string;
    ultimoNumeroNfe?: number;
    proximoNumeroNfe?: number;
    principal?: boolean;
  }>;
  optanteSimples: boolean;
  podeEmitir: boolean;
  aviso?: string;
};

export type NfeStatusServico = {
  status: string;
  motivo: string;
  uf: string;
  modelo: string;
  ambiente: string;
};

export type NfeEmissaoResultado = {
  sucesso: boolean;
  status: string;
  motivo: string;
  serie: string;
  numero: number;
  modelo: string;
  chaveAcesso?: string;
  protocolo?: string;
  statusProtocolo?: string;
  motivoProtocolo?: string;
  recibo?: string;
  tempoMedio?: string;
};

export type NfeConsultaResultado = {
  status: string;
  motivo: string;
  chaveAcesso?: string;
  protocolo?: string;
  statusProtocolo?: string;
  motivoProtocolo?: string;
};

export type NfeEventoResultado = {
  status: string;
  motivo: string;
  protocolo?: string;
  serie?: string;
  numero?: number;
  chaveAcesso?: string;
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
