import { ApiError, type LoginResponse } from "./api";
import { apiBaseUrl } from "./api-base";
import { getAppToken, getRefreshToken, saveLoginResponse } from "./app-session";

const API_URL = apiBaseUrl();

async function tentarRefresh(): Promise<boolean> {
  const refresh = getRefreshToken();
  if (!refresh) return false;
  try {
    const res = await fetch(`${API_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: refresh }),
    });
    if (!res.ok) return false;
    const data = (await res.json()) as LoginResponse;
    saveLoginResponse(data);
    return true;
  } catch {
    return false;
  }
}

async function fiscalRequest<T>(path: string, init?: RequestInit, retried = false): Promise<T> {
  const token = getAppToken();
  if (!token) throw new ApiError("Sessão expirada — faça login novamente.", 401);
  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...init?.headers,
    },
  });
  if (res.status === 401 && !retried && (await tentarRefresh())) {
    return fiscalRequest(path, init, true);
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(
      (body as { erro?: string; message?: string }).erro ??
        (body as { message?: string }).message ??
        res.statusText,
      res.status,
    );
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** GET/POST autenticado com refresh automático em 401. */
async function fiscalApiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  return fiscalRequest<T>(path, init);
}

/** Requisição autenticada com token explícito (convites, etc.). */
export async function fiscalFetch<T>(path: string, token: string, init?: RequestInit): Promise<T> {
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
    throw new ApiError(
      (body as { erro?: string; message?: string }).erro ??
        (body as { message?: string }).message ??
        res.statusText,
      res.status,
    );
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export type GrupoTributarioDto = {
  id?: number;
  descricao: string;
  origemMercadoria?: string;
  observacao?: string;
};

export type OperacaoFiscalDto = {
  id?: number;
  descricao: string;
  tipoOperacao?: string;
  geraFinanceiro?: string;
  movimentaEstoque?: string;
  descricaoNaNf?: string;
  cfop?: number | null;
  observacao?: string;
  principal?: string;
  finalidadeOperacao?: string;
  cMunFGIBS?: string;
  tpNFDebito?: string;
  tpNFCredito?: string;
  tpEnteGov?: string;
  pRedutor?: number | null;
  tpOperGov?: string;
  indIntermed?: string;
  ibsCbsCst?: string;
  ibsCbsClassTrib?: string;
  aliquotaIbsUf?: number | null;
  aliquotaIbsMun?: number | null;
  aliquotaCbs?: number | null;
  habilitarIbsCbs: boolean;
};

export type IcmsUfDto = {
  id?: number;
  ufDestino: string;
  cfop?: number | null;
  cst?: string;
  csosn?: string;
  aliquota?: number | null;
  origemMercadoria?: string;
};

export type ConfigOfGtDto = {
  id?: number;
  tributOperacaoFiscalId?: number;
  tributGrupoTributarioId?: number;
  listaIcmsUf?: IcmsUfDto[];
};

export type FiscalField = {
  key: string;
  label: string;
  type?: "text" | "number" | "select" | "checkbox" | "textarea";
  options?: { value: string; label: string }[];
  col?: boolean;
  reforma?: boolean;
};

export type PessoaDto = {
  id?: number;
  nome: string;
  nomeFantasia?: string;
  tipo: string;
  cpfCnpj?: string;
  email?: string;
  fone?: string;
  celular?: string;
  inscricaoEstadual?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  municipio?: string;
  uf?: string;
  cep?: string;
  codigoMunicipioIbge?: string;
  latitude?: string;
  longitude?: string;
  observacoes?: string;
  ativo: boolean;
};

export type VeiculoDto = {
  id?: number;
  placa: string;
  modelo?: string;
  marca?: string;
  renavam?: string;
  tipoRodado?: string;
  tipoCarroceria?: string;
  ativo: boolean;
};

export type ProdutoDto = {
  id?: number;
  codigo: string;
  nome: string;
  descricaoPdv?: string;
  gtin?: string;
  codigoNcm?: string;
  cest?: string;
  exTipi?: string;
  unidade: string;
  origem?: string;
  tipo?: string;
  valorUnitario?: number;
  valorCusto?: number;
  markup?: number;
  peso?: number;
  estoqueMinimo?: number;
  estoqueAtual?: number;
  observacoes?: string;
  grupoTributarioId?: number;
  grupoId?: number;
  subgrupoId?: number;
  ativo: boolean;
};

export type TributNfseServicoDto = {
  id?: number;
  descricao: string;
  itemListaServico: string;
  codigoTributacaoMunicipio?: string;
  nbs?: string;
  cnae?: string;
  descricaoServico?: string;
  municipioPrestacaoIbge?: string;
  aliquotaIss?: number | null;
  tributacaoIssqn?: string;
  issRetido?: string;
  simplesNacional?: string;
  regimeEspecial?: string;
  cstPisCofins?: string;
  aliquotaPis?: number | null;
  aliquotaCofins?: number | null;
  habilitarRetencoes?: boolean;
  retencaoInss?: number | null;
  retencaoIrrf?: number | null;
  retencaoCsll?: number | null;
  ibsCbsCst?: string;
  ibsCbsClassTrib?: string;
  aliquotaIbs?: number | null;
  aliquotaCbs?: number | null;
  habilitarIbsCbs: boolean;
  principal: boolean;
  ativo: boolean;
};

export const fiscalApi = {
  request: fiscalApiRequest,
  list: <T>(endpoint: string) => fiscalRequest<T[]>(endpoint),
  listPessoas: (q?: string) =>
    fiscalRequest<PessoaDto[]>(
      q?.trim() ? `/api/pessoas?q=${encodeURIComponent(q.trim())}` : "/api/pessoas",
    ),
  listProdutos: () => fiscalRequest<ProdutoDto[]>("/api/produto"),
  listVeiculos: () => fiscalRequest<VeiculoDto[]>("/api/veiculo"),
  listGruposTributarios: () => fiscalRequest<GrupoTributarioDto[]>("/api/tribut-grupo-tributario"),
  listOperacoesFiscais: () => fiscalRequest<OperacaoFiscalDto[]>("/api/tribut-operacao-fiscal"),
  listConfiguracoesOfGt: () => fiscalRequest<ConfigOfGtDto[]>("/api/tribut-configura-of-gt"),
  get: <T>(endpoint: string, id: number) => fiscalRequest<T>(`${endpoint}/${id}`),
  create: <T>(endpoint: string, body: unknown) =>
    fiscalRequest<T>(endpoint, { method: "POST", body: JSON.stringify(body) }),
  update: <T>(endpoint: string, id: number, body: unknown) =>
    fiscalRequest<T>(`${endpoint}/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  remove: (endpoint: string, id: number) =>
    fiscalRequest<void>(`${endpoint}/${id}`, { method: "DELETE" }),

  buscaPessoas: (q: string) =>
    fiscalRequest<Array<{ id: number; nome: string; cpfCnpj: string }>>(
      `/api/pessoas/busca?q=${encodeURIComponent(q)}`,
    ),

  operacoesSimples: () =>
    fiscalRequest<
      Array<{
        id: number;
        descricao: string;
        descricaoNaNf?: string;
        tipoOperacao?: string;
        principal?: string;
        cfop?: number;
        geraFinanceiro?: string;
        finalidadeOperacao?: string;
        observacao?: string;
        habilitarIbsCbs: boolean;
      }>
    >("/api/tribut-operacao-fiscal?simple=true"),

  produtosSimples: () =>
    fiscalRequest<Array<{ id: number; codigo: string; nome: string }>>("/api/produto?simple=true"),

  buscaProdutos: (q: string) =>
    fiscalRequest<
      Array<{
        id: number;
        codigo: string;
        nome: string;
        unidade?: string;
        valorUnitario?: number;
      }>
    >(`/api/produto/busca?q=${encodeURIComponent(q)}`),

  gruposTributarios: () => fiscalRequest<Array<{ id: number; descricao: string }>>("/api/tribut-grupo-tributario"),

  buscaNcm: (q: string) =>
    fiscalRequest<Array<{ id: number; codigo: string; descricao: string }>>(
      `/api/ncm?q=${encodeURIComponent(q.trim())}`,
    ),

  produtoGrupos: () =>
    fiscalRequest<Array<{ id: number; nome: string }>>("/api/produto-grupo"),

  produtoSubgrupos: (grupoId?: number) =>
    fiscalRequest<Array<{ id: number; produtoGrupoId: number; nome: string }>>(
      grupoId
        ? `/api/produto-subgrupo?grupoId=${grupoId}`
        : "/api/produto-subgrupo",
    ),

  buscaCest: (q = "", ncm = "") => {
    const params = new URLSearchParams();
    if (q.trim()) params.set("q", q.trim());
    if (ncm.trim()) params.set("ncm", ncm.trim());
    const qs = params.toString();
    return fiscalRequest<Array<{ codigo: string; descricao: string; ncmPrefixo?: string }>>(
      `/api/cest${qs ? `?${qs}` : ""}`,
    );
  },

  nfseServicos: () =>
    fiscalRequest<Array<{ id: number; descricao: string; itemListaServico: string }>>(
      "/api/tribut-nfse-servico?simple=true",
    ),

  listTributNfseServicos: (todos = true) =>
    fiscalRequest<TributNfseServicoDto[]>(
      `/api/tribut-nfse-servico?ativos=${todos ? "false" : "true"}`,
    ),
};

/** Campos tributação NFS-e (ISS, PIS/COFINS, retenções) */
export const NFSE_TRIBUT_FIELDS: FiscalField[] = [
  { key: "descricao", label: "Descrição / nome do cadastro" },
  { key: "itemListaServico", label: "Código nacional (LC 116)" },
  { key: "codigoTributacaoMunicipio", label: "Cód. tributação municipal" },
  { key: "nbs", label: "NBS" },
  { key: "cnae", label: "CNAE" },
  { key: "descricaoServico", label: "Descrição padrão do serviço", type: "textarea" },
  { key: "municipioPrestacaoIbge", label: "Município prestação (IBGE)" },
  { key: "aliquotaIss", label: "Alíquota ISS (%)", type: "number" },
  {
    key: "tributacaoIssqn",
    label: "Tributação ISSQN",
    type: "select",
    options: [
      { value: "1", label: "1 — Operação tributável" },
      { value: "2", label: "2 — Imunidade" },
      { value: "3", label: "3 — Exportação" },
      { value: "4", label: "4 — Não incidência" },
      { value: "5", label: "5 — Isento" },
    ],
  },
  {
    key: "issRetido",
    label: "ISS retido",
    type: "select",
    options: [
      { value: "1", label: "1 — Não retido" },
      { value: "2", label: "2 — Retido" },
    ],
  },
  {
    key: "simplesNacional",
    label: "Simples Nacional",
    type: "select",
    options: [
      { value: "1", label: "1 — Não optante" },
      { value: "2", label: "2 — ME/EPP" },
      { value: "3", label: "3 — MEI" },
    ],
  },
  {
    key: "regimeEspecial",
    label: "Regime especial",
    type: "select",
    options: [
      { value: "0", label: "0 — Nenhum" },
      { value: "1", label: "1 — Microempresa municipal" },
      { value: "2", label: "2 — Estimativa" },
      { value: "3", label: "3 — Sociedade profissionais" },
      { value: "4", label: "4 — Cooperativa" },
      { value: "5", label: "5 — MEI" },
      { value: "6", label: "6 — ME/EPP" },
    ],
  },
  {
    key: "cstPisCofins",
    label: "CST PIS/COFINS",
    type: "select",
    options: [
      { value: "01", label: "01 — Tributável alíquota básica" },
      { value: "06", label: "06 — Sem incidência" },
      { value: "07", label: "07 — Isento" },
      { value: "08", label: "08 — Sem retenção" },
    ],
  },
  { key: "aliquotaPis", label: "Alíquota PIS (%)", type: "number" },
  { key: "aliquotaCofins", label: "Alíquota COFINS (%)", type: "number" },
  { key: "habilitarRetencoes", label: "Habilitar retenções federais", type: "checkbox" },
  { key: "retencaoInss", label: "Retenção INSS (R$)", type: "number" },
  { key: "retencaoIrrf", label: "Retenção IRRF (R$)", type: "number" },
  { key: "retencaoCsll", label: "Retenção CSLL (R$)", type: "number" },
  { key: "ibsCbsCst", label: "CST IBS/CBS", reforma: true },
  { key: "ibsCbsClassTrib", label: "cClassTrib", reforma: true },
  { key: "aliquotaIbs", label: "Alíq. IBS (teste 1%)", type: "number", reforma: true },
  { key: "aliquotaCbs", label: "Alíq. CBS (teste 1%)", type: "number", reforma: true },
  { key: "habilitarIbsCbs", label: "Incluir IBS/CBS na NFS-e", type: "checkbox", reforma: true },
  { key: "principal", label: "Cadastro principal", type: "checkbox" },
  { key: "ativo", label: "Ativo", type: "checkbox" },
];

/** Campos Reforma Tributária — IBS/CBS (obrigatório NF-e a partir de 03/08/2026) */
export const REFORMA_OBRIGATORIEDADE = "2026-08-03";

export const REFORMA_FIELDS: FiscalField[] = [
  { key: "cMunFGIBS", label: "cMunFGIBS — Município FG IBS (IBGE)", reforma: true },
  { key: "tpNFDebito", label: "tpNFDebito", reforma: true },
  { key: "tpNFCredito", label: "tpNFCredito", reforma: true },
  {
    key: "tpEnteGov",
    label: "tpEnteGov",
    type: "select",
    reforma: true,
    options: [
      { value: "", label: "—" },
      { value: "1", label: "1 — União" },
      { value: "2", label: "2 — Estado" },
      { value: "3", label: "3 — Município" },
    ],
  },
  { key: "pRedutor", label: "pRedutor (%)", type: "number", reforma: true },
  { key: "tpOperGov", label: "tpOperGov", reforma: true },
  {
    key: "indIntermed",
    label: "Intermediador",
    type: "select",
    reforma: true,
    options: [
      { value: "0", label: "0 — Sem intermediador" },
      { value: "1", label: "1 — Com intermediador" },
    ],
  },
  { key: "ibsCbsCst", label: "CST IBS/CBS", reforma: true },
  { key: "ibsCbsClassTrib", label: "cClassTrib", reforma: true },
  { key: "aliquotaIbsUf", label: "Alíq. IBS UF (teste)", type: "number", reforma: true },
  { key: "aliquotaIbsMun", label: "Alíq. IBS Mun (teste)", type: "number", reforma: true },
  { key: "aliquotaCbs", label: "Alíq. CBS (teste 1%)", type: "number", reforma: true },
  { key: "habilitarIbsCbs", label: "Incluir IBS/CBS na NF-e", type: "checkbox", reforma: true },
];
