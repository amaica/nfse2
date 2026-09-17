"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { AutoCompleteCompleteEvent } from "primereact/autocomplete";
import {
  CheckCircle,
  FileDown,
  FileStack,
  Leaf,
  Loader2,
  Mail,
  Package,
  Plus,
  Printer,
  RefreshCw,
  Send,
  Trash2,
  Truck,
  UserRound,
  Wheat,
} from "lucide-react";
import { api, ApiError, formatarCnpjCpf, type NfeContexto, type NfeItemBody } from "@/lib/api";
import { fiscalApi, type PessoaDto, type PessoaEnderecoDto, type VeiculoDto } from "@/lib/fiscal-api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { AssinaturaBanner } from "@/components/conta/AssinaturaBanner";
import { mapEmissaoError } from "@/lib/assinatura";
import { fmtCfop } from "@/lib/cfop";
import { UNIDADES_PRODUTO } from "@/lib/produto-unidades";
import { MoedaInput, QtyInput, fmtMoeda } from "@/components/fiscal/MoedaInput";
import { AutoCompleteField, type AcOption } from "@/components/ui/AutoCompleteField";
import {
  CONSUMIDOR_FINAL,
  FINALIDADES_NFE,
  MEIOS_PAGAMENTO,
  MODALIDADES_FRETE,
  MODELOS_NFP,
  PRAZOS_PAGAMENTO,
  PRESENCA_COMPRADOR,
  UFS_IBGE,
  fmtAamm,
  parseChaveNfe,
} from "@/lib/nfe-emissao-opcoes";

function asOption(value: AcOption | string | null): AcOption | null {
  if (!value) return null;
  if (typeof value === "string") return { label: value, value };
  return value;
}

type PessoaSug = { id: number; nome: string; cpfCnpj: string };
type Operacao = {
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
};
type ProdutoSug = {
  id: number;
  codigo: string;
  nome: string;
  unidade?: string;
  valorUnitario?: number;
};

type ItemLinha = {
  key: string;
  produtoId?: number;
  codigo: string;
  descricao: string;
  ncm: string;
  cfop: string;
  unidade: string;
  quantidade: number;
  valorUnitario: number;
  valorDesconto: number;
};

type RefLinha = {
  key: string;
  tipo: "NFE" | "NFP" | "CTE";
  chave?: string;
  codigoUf?: string;
  anoMes?: string;
  cnpj?: string;
  cpf?: string;
  inscricaoEstadual?: string;
  modelo?: string;
  serie?: string;
  numero?: string;
  rotulo?: string;
};

type ReboqueLinha = { key: string; placa: string; uf: string; rntc: string };

type VolumeLinha = {
  key: string;
  quantidade?: number;
  especie: string;
  marca: string;
  numeracao: string;
  pesoLiquido: string;
  pesoBruto: string;
};

type DestEnderecoOpt = {
  key: string;
  label: string;
  inscricaoEstadual: string;
  logradouro: string;
  numero: string;
  complemento: string;
  bairro: string;
  municipio: string;
  uf: string;
  cep: string;
  codigoMunicipioIbge: string;
};

function rotuloIeEndereco(ie: string, logradouro: string, municipio: string, uf: string) {
  const ieLab = ie?.trim() || "—";
  const log = logradouro?.trim() || "";
  const cid = [municipio?.trim(), uf?.trim()].filter(Boolean).join("/");
  return `IE:${ieLab} - ${log}${cid ? ` - ${cid}` : ""}`;
}

function enderecosDoCliente(full: PessoaDto): DestEnderecoOpt[] {
  const out: DestEnderecoOpt[] = [];
  const temPrincipal =
    !!(full.logradouro?.trim() || full.inscricaoEstadual?.trim() || full.municipio?.trim());
  if (temPrincipal) {
    out.push({
      key: "principal",
      label: rotuloIeEndereco(
        full.inscricaoEstadual ?? "",
        full.logradouro ?? "",
        full.municipio ?? "",
        full.uf ?? "",
      ),
      inscricaoEstadual: full.inscricaoEstadual ?? "",
      logradouro: full.logradouro ?? "",
      numero: full.numero ?? "",
      complemento: full.complemento ?? "",
      bairro: full.bairro ?? "",
      municipio: full.municipio ?? "",
      uf: full.uf ?? "",
      cep: full.cep ?? "",
      codigoMunicipioIbge: full.codigoMunicipioIbge ?? "",
    });
  }
  for (const e of full.enderecos ?? []) {
    out.push(enderecoAdicionalParaOpt(e));
  }
  return out;
}

function enderecoAdicionalParaOpt(e: PessoaEnderecoDto): DestEnderecoOpt {
  const key = e.id != null ? `id-${e.id}` : `tmp-${e.inscricaoEstadual}-${e.logradouro}`;
  return {
    key,
    label:
      e.valores ||
      rotuloIeEndereco(e.inscricaoEstadual ?? "", e.logradouro ?? "", e.municipio ?? "", e.uf ?? ""),
    inscricaoEstadual: e.inscricaoEstadual ?? "",
    logradouro: e.logradouro ?? "",
    numero: e.numero ?? "",
    complemento: e.complemento ?? "",
    bairro: e.bairro ?? "",
    municipio: e.municipio ?? "",
    uf: e.uf ?? "",
    cep: e.cep ?? "",
    codigoMunicipioIbge: e.codigoMunicipioIbge ?? "",
  };
}

function geraFinanceiro(op?: Operacao | null) {
  const v = (op?.geraFinanceiro || "S").toUpperCase();
  return v === "S" || v === "1" || v === "SIM";
}

function finalidadeDaOperacao(op?: Operacao | null) {
  const f = (op?.finalidadeOperacao || "").trim();
  if (f === "1" || f === "2" || f === "3" || f === "4") return f;
  const d = `${op?.descricao || ""} ${op?.descricaoNaNf || ""}`.toLowerCase();
  if (d.includes("devol")) return "4";
  if (d.includes("complement")) return "2";
  return "1";
}

function naturezaDaOperacao(op?: Operacao | null) {
  const t = (op?.descricaoNaNf || op?.descricao || "").trim();
  return t || "Venda de mercadoria";
}

function labelNatureza(op: Operacao) {
  const nat = naturezaDaOperacao(op);
  const cfop = fmtCfop(op.cfop);
  return cfop ? `${nat} — CFOP ${cfop}` : nat;
}

function isSaida(tipo?: string) {
  return tipo === "S" || tipo === "1";
}

function isEntrada(tipo?: string) {
  return tipo === "E" || tipo === "0";
}

function bruto(i: ItemLinha) {
  return i.quantidade * i.valorUnitario;
}

function liquido(i: ItemLinha) {
  return Math.max(0, bruto(i) - (i.valorDesconto || 0));
}

function parseDecimal(raw: string) {
  const t = raw.trim().replace(/\./g, "").replace(",", ".");
  const n = Number(t);
  return Number.isFinite(n) ? n : 0;
}

function isAmbienteProducao(amb?: string | null) {
  if (!amb) return false;
  const a = amb.toUpperCase();
  return a === "1" || a === "PROD" || a === "PRODUCAO" || a === "PRODUÇÃO";
}

function fmtDataHora() {
  return new Date().toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function NfeEmissaoWorkspace() {
  const token = getAppToken();
  const { empresaId } = useEmpresaScope();
  const [ctx, setCtx] = useState<NfeContexto | null>(null);
  const [operacoes, setOperacoes] = useState<Operacao[]>([]);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [resultado, setResultado] = useState<Record<string, unknown> | null>(null);
  const [emailDanfe, setEmailDanfe] = useState("");
  const [enviandoEmail, setEnviandoEmail] = useState(false);
  const [emailMsg, setEmailMsg] = useState("");
  const [emailErro, setEmailErro] = useState("");

  const [enderecoId, setEnderecoId] = useState<number | "">("");
  const [operacaoFiscalId, setOperacaoFiscalId] = useState<number | "">("");
  const [naturezaOperacao, setNaturezaOperacao] = useState("");

  const [destOpt, setDestOpt] = useState<AcOption | null>(null);
  const [destSug, setDestSug] = useState<AcOption[]>([]);
  const [destinatario, setDestinatario] = useState<PessoaSug | null>(null);
  const [destEmail, setDestEmail] = useState("");
  const [destIe, setDestIe] = useState("");
  const [destEnderecos, setDestEnderecos] = useState<DestEnderecoOpt[]>([]);
  const [destEndKey, setDestEndKey] = useState("");
  const [destEndereco, setDestEndereco] = useState<DestEnderecoOpt | null>(null);

  const [itens, setItens] = useState<ItemLinha[]>([]);
  const [prodOpt, setProdOpt] = useState<AcOption | null>(null);
  const [prodSug, setProdSug] = useState<AcOption[]>([]);
  const [prodSel, setProdSel] = useState<ProdutoSug | null>(null);
  const [addQtd, setAddQtd] = useState<number | undefined>(1);
  const [addValor, setAddValor] = useState<number | undefined>(undefined);
  const [addDesc, setAddDesc] = useState<number | undefined>(undefined);
  const [addUnidade, setAddUnidade] = useState("UN");
  const [natOpt, setNatOpt] = useState<AcOption | null>(null);
  const [natSug, setNatSug] = useState<AcOption[]>([]);

  const [finalidade, setFinalidade] = useState("1");
  const [consumidorFinal, setConsumidorFinal] = useState("1");
  const [indicadorPresenca, setIndicadorPresenca] = useState("9");
  const [meioPagamento, setMeioPagamento] = useState("01");
  const [indicadorPagamento, setIndicadorPagamento] = useState("0");
  const [informacoesAdicionais, setInformacoesAdicionais] = useState("");

  const [modFrete, setModFrete] = useState("9");
  const [transpNome, setTranspNome] = useState("");
  const [transpDoc, setTranspDoc] = useState("");
  const [transpIe, setTranspIe] = useState("");
  const [transpMun, setTranspMun] = useState("");
  const [transpUf, setTranspUf] = useState("");
  const [transpOpt, setTranspOpt] = useState<AcOption | null>(null);
  const [transpSug, setTranspSug] = useState<AcOption[]>([]);
  const [placa, setPlaca] = useState("");
  const [placaOpt, setPlacaOpt] = useState<AcOption | null>(null);
  const [placaSug, setPlacaSug] = useState<AcOption[]>([]);
  const [placaUf, setPlacaUf] = useState("");
  const [rntc, setRntc] = useState("");
  const [valorFrete, setValorFrete] = useState<number | undefined>(undefined);
  const [volumes, setVolumes] = useState<VolumeLinha[]>([]);
  const [reboques, setReboques] = useState<ReboqueLinha[]>([]);
  const [veiculos, setVeiculos] = useState<VeiculoDto[]>([]);

  const [referencias, setReferencias] = useState<RefLinha[]>([]);
  const [refTipo, setRefTipo] = useState<"NFE" | "NFP">("NFE");
  const [refOpt, setRefOpt] = useState<AcOption | null>(null);
  const [refSug, setRefSug] = useState<AcOption[]>([]);
  const [nfpUf, setNfpUf] = useState("");
  const [nfpAamm, setNfpAamm] = useState("");
  const [nfpCnpj, setNfpCnpj] = useState("");
  const [nfpCpf, setNfpCpf] = useState("");
  const [nfpIe, setNfpIe] = useState("");
  const [nfpModelo, setNfpModelo] = useState("04");
  const [nfpSerie, setNfpSerie] = useState("");
  const [nfpNumero, setNfpNumero] = useState("");

  const carregarContexto = useCallback(async () => {
    if (!token) return;
    setErro("");
    try {
      const c = await api.nfeContexto(token);
      setCtx(c);
      if (c.enderecos?.length === 1) {
        setEnderecoId(c.enderecos[0].id);
      } else if (c.enderecos?.length) {
        const principal = c.enderecos.find((e) => e.principal) ?? c.enderecos[0];
        setEnderecoId(principal.id);
      }
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : "Erro ao carregar contexto NF-e";
      if (msg.toLowerCase().includes("endereco")) {
        setErro(
          `${msg} — cadastre ao menos um endereço em Cadastros → Emitentes antes de emitir NF-e.`,
        );
      } else {
        setErro(msg);
      }
      setCtx(null);
    }
  }, [token]);

  const aplicarOperacao = useCallback((op: Operacao | undefined) => {
    if (!op) {
      setOperacaoFiscalId("");
      setNaturezaOperacao("");
      setNatOpt(null);
      return;
    }
    setOperacaoFiscalId(op.id);
    setNaturezaOperacao(naturezaDaOperacao(op));
    setNatOpt({
      label: labelNatureza(op),
      value: String(op.id),
      meta: naturezaDaOperacao(op),
      raw: op,
    });
    const fin = finalidadeDaOperacao(op);
    setFinalidade(fin);
    if (op.observacao?.trim()) {
      setInformacoesAdicionais(op.observacao.trim());
    }
    if (!geraFinanceiro(op) || fin === "4") {
      setMeioPagamento("90");
      setIndicadorPagamento("0");
    } else {
      setMeioPagamento((atual) => (atual === "90" ? "01" : atual));
    }
  }, []);

  useEffect(() => {
    if (!token || !empresaId) return;
    setResultado(null);
    setItens([]);
    setDestinatario(null);
    setDestOpt(null);
    setDestSug([]);
    setProdSel(null);
    setProdOpt(null);
    setProdSug([]);
    setAddValor(undefined);
    setAddDesc(undefined);
    setAddQtd(1);
    setOperacaoFiscalId("");
    setNaturezaOperacao("");
    setNatOpt(null);
    setEnderecoId("");
    setFinalidade("1");
    setConsumidorFinal("1");
    setIndicadorPresenca("9");
    setMeioPagamento("01");
    setIndicadorPagamento("0");
    setInformacoesAdicionais("");
    setModFrete("9");
    setTranspNome("");
    setTranspDoc("");
    setTranspIe("");
    setTranspMun("");
    setTranspUf("");
    setTranspOpt(null);
    setTranspSug([]);
    setPlaca("");
    setPlacaOpt(null);
    setPlacaSug([]);
    setPlacaUf("");
    setRntc("");
    setValorFrete(undefined);
    setVolumes([]);
    setReboques([]);
    setDestEnderecos([]);
    setDestEndKey("");
    setDestEndereco(null);
    setAddUnidade("UN");
    setReferencias([]);
    setRefOpt(null);
    setRefSug([]);
    carregarContexto();
    fiscalApi
      .operacoesSimples()
      .then((ops) => {
        setOperacoes(ops);
        const padrao =
          ops.find((o) => o.principal === "S") ??
          ops.find((o) => o.descricao.toLowerCase() === "venda") ??
          ops.find((o) => isSaida(o.tipoOperacao)) ??
          ops[0];
        if (padrao) aplicarOperacao(padrao);
      })
      .catch(() => {});
    fiscalApi
      .listVeiculos()
      .then((vs) => setVeiculos(vs.filter((v) => v.ativo !== false)))
      .catch(() => setVeiculos([]));
  }, [token, empresaId, carregarContexto, aplicarOperacao]);

  const operacaoSel = operacoes.find((o) => o.id === operacaoFiscalId);
  const cfopPadrao = fmtCfop(operacaoSel?.cfop) || "";

  const saidas = operacoes.filter((o) => isSaida(o.tipoOperacao));
  const entradas = operacoes.filter((o) => isEntrada(o.tipoOperacao));
  const outras = operacoes.filter((o) => !isSaida(o.tipoOperacao) && !isEntrada(o.tipoOperacao));

  const enderecoAtivo =
    ctx?.enderecos?.find((e) => e.id === enderecoId) ??
    ctx?.enderecos?.find((e) => e.principal) ??
    ctx?.enderecos?.[0];
  const ultimoDisplay = enderecoAtivo?.ultimoNumeroNfe ?? ctx?.ultimoNumero ?? null;
  const numeroDisplay = enderecoAtivo?.proximoNumeroNfe ?? ctx?.proximoNumero ?? "—";
  const serieDisplay = enderecoAtivo?.serieNfe ?? ctx?.serie ?? "—";
  const ambiente = ctx?.ambiente ?? "2";
  const ambienteProd = isAmbienteProducao(ambiente);
  const ufEmitente = (enderecoAtivo?.uf || ctx?.ufEmitente || "").toUpperCase();

  useEffect(() => {
    if (!ufEmitente) return;
    setPlacaUf((atual) => atual || ufEmitente);
    setTranspUf((atual) => atual || ufEmitente);
    const ibge = UFS_IBGE.find((u) => u.sigla === ufEmitente)?.ibge || "";
    setNfpUf((atual) => atual || ibge);
  }, [ufEmitente]);

  const totais = useMemo(() => {
    const qtd = itens.reduce((s, i) => s + i.quantidade, 0);
    const produtos = itens.reduce((s, i) => s + bruto(i), 0);
    const descontos = itens.reduce((s, i) => s + (i.valorDesconto || 0), 0);
    return { qtd, produtos, descontos, nota: Math.max(0, produtos - descontos) };
  }, [itens]);

  const pessoaParaOpt = (p: PessoaSug): AcOption => ({
    label: p.nome,
    value: String(p.id),
    meta: formatarCnpjCpf(p.cpfCnpj || "") || undefined,
    raw: p,
  });

  const produtoParaOpt = (p: ProdutoSug): AcOption => ({
    label: `${p.codigo} — ${p.nome}`,
    value: String(p.id),
    meta: p.valorUnitario && p.valorUnitario > 0 ? fmtMoeda(p.valorUnitario) : "sem preço",
    raw: p,
  });

  const buscarDest = async (event: AutoCompleteCompleteEvent) => {
    const q = event.query?.trim() ?? "";
    try {
      const pessoas = await fiscalApi.buscaPessoas(q);
      setDestSug(pessoas.slice(0, 50).map(pessoaParaOpt));
    } catch {
      setDestSug([]);
    }
  };

  const aplicarDestEndereco = (opt: DestEnderecoOpt | null) => {
    setDestEndereco(opt);
    setDestEndKey(opt?.key ?? "");
    setDestIe(opt?.inscricaoEstadual ?? "");
  };

  const selecionarDest = async (opt: AcOption) => {
    setDestOpt(opt);
    const p = (opt.raw as PessoaSug | undefined) ?? {
      id: Number(opt.value),
      nome: opt.label,
      cpfCnpj: "",
    };
    setDestinatario(p);
    setDestSug([]);
    setDestEnderecos([]);
    aplicarDestEndereco(null);
    try {
      const full = await fiscalApi.get<PessoaDto>("/api/pessoas", p.id);
      setDestEmail(full.email ?? "");
      if (full.email?.trim()) {
        setEmailDanfe(full.email.trim());
      }
      if (full.cpfCnpj) {
        setDestinatario({ id: p.id, nome: full.nome || p.nome, cpfCnpj: full.cpfCnpj });
      }
      const ends = enderecosDoCliente(full);
      setDestEnderecos(ends);
      if (ends.length === 1) {
        aplicarDestEndereco(ends[0]);
      } else if (ends.length > 1) {
        const principal = ends.find((e) => e.key === "principal") ?? ends[0];
        aplicarDestEndereco(principal);
      } else {
        setDestIe(full.inscricaoEstadual ?? "");
      }
    } catch {
      /* opcional */
    }
  };

  const buscarProd = async (event: AutoCompleteCompleteEvent) => {
    const q = event.query?.trim() ?? "";
    try {
      const produtos = await fiscalApi.buscaProdutos(q);
      setProdSug(produtos.slice(0, 50).map(produtoParaOpt));
    } catch {
      setProdSug([]);
    }
  };

  const selecionarProd = async (opt: AcOption) => {
    setProdOpt(opt);
    const p = (opt.raw as ProdutoSug | undefined) ?? {
      id: Number(opt.value),
      codigo: "",
      nome: opt.label,
    };
    setProdSel(p);
    setProdSug([]);
    setAddUnidade(p.unidade || "UN");
    setAddValor(p.valorUnitario && p.valorUnitario > 0 ? p.valorUnitario : undefined);
    try {
      const full = await fiscalApi.get<{ valorUnitario?: number; unidade?: string }>("/api/produto", p.id);
      if (full.valorUnitario && full.valorUnitario > 0) {
        setAddValor(full.valorUnitario);
      }
      if (full.unidade) {
        setAddUnidade(full.unidade);
      }
    } catch {
      /* usa valor da busca */
    }
  };

  const buscarNatureza = (event: AutoCompleteCompleteEvent) => {
    const q = (event.query ?? "").trim().toLowerCase();
    const lista = [...saidas, ...entradas, ...outras];
    setNatSug(
      lista
        .filter((o) => {
          if (!q) return true;
          const lab = labelNatureza(o).toLowerCase();
          const nat = naturezaDaOperacao(o).toLowerCase();
          return lab.includes(q) || nat.includes(q) || String(o.cfop ?? "").includes(q);
        })
        .slice(0, 40)
        .map((o) => ({
          label: labelNatureza(o),
          value: String(o.id),
          meta: naturezaDaOperacao(o),
          raw: o,
        })),
    );
  };

  const atualizarItem = (key: string, patch: Partial<ItemLinha>) => {
    setItens((prev) => prev.map((i) => (i.key === key ? { ...i, ...patch } : i)));
  };

  const adicionarItem = async () => {
    setErro("");
    if (!prodSel) {
      setErro("Busque e selecione o produto (soja, milho, animal…).");
      return;
    }
    const qtd = addQtd ?? 0;
    if (qtd <= 0) {
      setErro("Informe a quantidade.");
      return;
    }
    const vlr = addValor ?? 0;
    if (vlr <= 0) {
      setErro("Informe o valor unitário — o cadastro deste produto não tem preço.");
      return;
    }
    const desc = addDesc ?? 0;
    if (desc < 0 || desc > qtd * vlr) {
      setErro("Desconto inválido.");
      return;
    }
    try {
      const p = await fiscalApi.get<{
        id: number;
        codigo: string;
        nome: string;
        codigoNcm?: string;
        unidade?: string;
      }>("/api/produto", prodSel.id);
      setItens((prev) => [
        ...prev,
        {
          key: `${Date.now()}-${p.id}`,
          produtoId: p.id,
          codigo: p.codigo,
          descricao: p.nome,
          ncm: p.codigoNcm || "00000000",
          cfop: cfopPadrao,
          unidade: addUnidade || p.unidade || prodSel.unidade || "UN",
          quantidade: qtd,
          valorUnitario: vlr,
          valorDesconto: desc,
        },
      ]);
      setProdSel(null);
      setProdOpt(null);
      setAddQtd(1);
      setAddValor(undefined);
      setAddDesc(undefined);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar produto");
    }
  };

  const removerItem = (key: string) => setItens((prev) => prev.filter((i) => i.key !== key));

  const buscarTransportadora = async (event: AutoCompleteCompleteEvent) => {
    const q = event.query?.trim() ?? "";
    try {
      const pessoas = await fiscalApi.buscaPessoas(q);
      setTranspSug(pessoas.slice(0, 50).map(pessoaParaOpt));
    } catch {
      setTranspSug([]);
    }
  };

  const selecionarTransportadora = async (opt: AcOption) => {
    setTranspOpt(opt);
    const p = (opt.raw as PessoaSug | undefined) ?? {
      id: Number(opt.value),
      nome: opt.label,
      cpfCnpj: "",
    };
    setTranspNome(p.nome);
    setTranspDoc(p.cpfCnpj || "");
    setTranspSug([]);
    try {
      const full = await fiscalApi.get<{
        inscricaoEstadual?: string;
        municipio?: string;
        uf?: string;
      }>("/api/pessoas", p.id);
      setTranspIe(full.inscricaoEstadual ?? "");
      setTranspMun(full.municipio ?? "");
      if (full.uf) setTranspUf(full.uf.toUpperCase());
    } catch {
      /* opcional */
    }
  };

  const buscarPlaca = (event: AutoCompleteCompleteEvent) => {
    const q = (event.query ?? "").trim().toUpperCase();
    setPlacaSug(
      veiculos
        .filter((v) => !q || v.placa.toUpperCase().includes(q) || (v.modelo || "").toUpperCase().includes(q))
        .slice(0, 20)
        .map((v) => ({
          label: v.placa,
          value: v.placa,
          meta: v.modelo || undefined,
          raw: v,
        })),
    );
  };

  const buscarRef = async (event: AutoCompleteCompleteEvent) => {
    const q = event.query?.trim() ?? "";
    const digits = q.replace(/\D/g, "");
    if (digits.length === 44) {
      setRefSug([
        {
          label: `Colar chave · ${digits}`,
          value: digits,
          meta: "Chave de 44 dígitos",
        },
      ]);
      return;
    }
    if (!token) {
      setRefSug([]);
      return;
    }
    try {
      const [proprias, entradasLista] = await Promise.all([
        api.nfeListarNotas(token, q).catch(() => ({ itens: [] })),
        api.nfeListarEntradas(token, q).catch(() => []),
      ]);
      const lista: AcOption[] = [];
      for (const n of proprias.itens ?? []) {
        if (!n.chave) continue;
        lista.push({
          label: `Emitida nº ${n.numero ?? "—"} / ${n.serie ?? "—"}`,
          value: n.chave,
          meta: n.chave,
        });
      }
      for (const n of entradasLista) {
        if (!n.chave) continue;
        lista.push({
          label: `Entrada nº ${n.numero ?? "—"} · ${n.nomeEmitente ?? "emitente"}`,
          value: n.chave,
          meta: n.chave,
        });
      }
      setRefSug(lista.slice(0, 20));
    } catch {
      setRefSug([]);
    }
  };

  const aplicarChaveRef = (chave: string, rotulo?: string) => {
    const parsed = parseChaveNfe(chave);
    if (!parsed) {
      setErro("Chave deve ter 44 dígitos.");
      return;
    }
    setRefSug([]);
    setRefOpt(null);
    setNfpUf(parsed.codigoUf);
    setNfpAamm(fmtAamm(parsed.anoMes));
    if (parsed.cnpj.startsWith("000")) {
      setNfpCpf(parsed.cnpj.slice(-11));
      setNfpCnpj("");
    } else {
      setNfpCnpj(parsed.cnpj);
      setNfpCpf("");
    }
    setNfpModelo(parsed.modelo === "55" ? "04" : parsed.modelo);
    setNfpSerie(parsed.serie);
    setNfpNumero(parsed.numero);
    if (refTipo === "NFE") {
      setReferencias((prev) => [
        ...prev,
        {
          key: `${Date.now()}-${parsed.chave}`,
          tipo: parsed.modelo === "57" ? "CTE" : "NFE",
          chave: parsed.chave,
          rotulo: rotulo || `NF-e ${parsed.numero} · série ${parsed.serie}`,
        },
      ]);
    }
  };

  const incluirReferencia = () => {
    setErro("");
    if (refTipo === "NFE") {
      const raw =
        (typeof refOpt === "object" && refOpt?.value) ||
        (typeof refOpt === "string" ? refOpt : "") ||
        "";
      const parsed = parseChaveNfe(String(raw));
      if (!parsed) {
        setErro("Informe a chave de 44 dígitos da NF-e referenciada, ou busque uma nota já baixada/emitida.");
        return;
      }
      setReferencias((prev) => [
        ...prev,
        {
          key: `${Date.now()}-${parsed.chave}`,
          tipo: "NFE",
          chave: parsed.chave,
          rotulo: `NF-e ${parsed.numero} · série ${parsed.serie}`,
        },
      ]);
      setRefOpt(null);
      setRefSug([]);
      return;
    }
    const aamm = nfpAamm.replace(/\D/g, "");
    if (!nfpUf || aamm.length !== 4 || !nfpIe.trim() || !nfpSerie.trim() || !nfpNumero.trim()) {
      setErro("NF produtor rural: preencha UF, ano/mês, IE, série e número.");
      return;
    }
    const doc = (nfpCnpj || nfpCpf).replace(/\D/g, "");
    if (doc.length !== 11 && doc.length !== 14) {
      setErro("NF produtor rural: informe CNPJ ou CPF do emitente.");
      return;
    }
    setReferencias((prev) => [
      ...prev,
      {
        key: `${Date.now()}-nfp`,
        tipo: "NFP",
        codigoUf: nfpUf,
        anoMes: aamm,
        cnpj: doc.length === 14 ? doc : undefined,
        cpf: doc.length === 11 ? doc : undefined,
        inscricaoEstadual: nfpIe.trim(),
        modelo: nfpModelo || "04",
        serie: nfpSerie.trim(),
        numero: nfpNumero.trim(),
        rotulo: `NFP nº ${nfpNumero} · série ${nfpSerie} · ${fmtAamm(aamm)}`,
      },
    ]);
  };

  const abrirDanfe = async (chave: string) => {
    if (!token || !chave) return;
    try {
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas/${encodeURIComponent(chave)}/danfe`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Falha ao abrir DANFE");
      const blob = await res.blob();
      window.open(URL.createObjectURL(blob), "_blank", "noopener,noreferrer");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao abrir DANFE");
    }
  };

  const imprimirDanfe = async (chave: string) => {
    if (!token || !chave) return;
    try {
      await api.nfeImprimirDanfe(token, chave);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao imprimir DANFE");
    }
  };

  const enviarDanfeEmail = async () => {
    const chave = resultado?.chaveAcesso ? String(resultado.chaveAcesso) : "";
    if (!token || !chave || !emailDanfe.trim()) return;
    setEnviandoEmail(true);
    setEmailMsg("");
    setEmailErro("");
    try {
      await api.nfeEnviarDanfeEmail(token, chave, emailDanfe.trim());
      setEmailMsg(`DANFE enviado para ${emailDanfe.trim()}`);
    } catch (e) {
      setEmailErro(e instanceof ApiError ? e.message : "Falha ao enviar e-mail");
    } finally {
      setEnviandoEmail(false);
    }
  };

  const chaveAutorizada =
    resultado?.chaveAcesso != null &&
    String(resultado.statusProtocolo ?? resultado.status ?? "").startsWith("100");

  const transmitir = async () => {
    if (!token) return;
    if (!destinatario) {
      setErro("Selecione o destinatário.");
      return;
    }
    if (operacaoFiscalId === "") {
      setErro("Selecione a natureza da operação.");
      return;
    }
    if (itens.length === 0) {
      setErro("Inclua ao menos um item (grão, animal, insumo…).");
      return;
    }
    if ((finalidade === "4") && referencias.length === 0) {
      setErro("Devolução exige ao menos um documento referenciado (chave NF-e ou NF de produtor rural).");
      return;
    }
    setLoading(true);
    setErro("");
    setResultado(null);
    try {
      const bodyItens: NfeItemBody[] = itens.map((i) => ({
        produtoId: i.produtoId,
        codigo: i.codigo,
        descricao: i.descricao,
        ncm: i.ncm,
        cfop: i.cfop,
        unidade: i.unidade,
        quantidade: i.quantidade,
        valorUnitario: i.valorUnitario,
        valorDesconto: i.valorDesconto > 0 ? i.valorDesconto : undefined,
        ibsCbs: { habilitar: operacaoSel?.habilitarIbsCbs ?? true },
      }));
      const res = await api.nfeEnviarLote(token, {
        enderecoId: enderecoId === "" ? undefined : Number(enderecoId),
        operacaoFiscalId: Number(operacaoFiscalId),
        sincrono: true,
        naturezaOperacao: naturezaOperacao || naturezaDaOperacao(operacaoSel),
        destinatario: {
          nome: destinatario.nome,
          documento: destinatario.cpfCnpj || undefined,
          email: destEmail || undefined,
          inscricaoEstadual: destIe || undefined,
          logradouro: destEndereco?.logradouro || undefined,
          numero: destEndereco?.numero || undefined,
          complemento: destEndereco?.complemento || undefined,
          bairro: destEndereco?.bairro || undefined,
          municipio: destEndereco?.municipio || undefined,
          uf: destEndereco?.uf || undefined,
          cep: destEndereco?.cep || undefined,
          codigoMunicipioIbge: destEndereco?.codigoMunicipioIbge || undefined,
        },
        itens: bodyItens,
        finalidade,
        consumidorFinal,
        indicadorPresenca,
        informacoesAdicionais: informacoesAdicionais.trim() || undefined,
        pagamento: {
          meioPagamento,
          indicadorPagamento,
        },
        transporte: {
          modalidadeFrete: modFrete,
          transportadorNome: transpNome.trim() || undefined,
          transportadorDocumento: transpDoc.replace(/\D/g, "") || undefined,
          transportadorIe: transpIe.trim() || undefined,
          transportadorMunicipio: transpMun.trim() || undefined,
          transportadorUf: transpUf || undefined,
          placa: placa.replace(/[^A-Za-z0-9]/g, "") || undefined,
          placaUf: placaUf || undefined,
          rntc: rntc.trim() || undefined,
          valorFrete: valorFrete && valorFrete > 0 ? valorFrete : undefined,
          reboques: reboques
            .filter((r) => r.placa.trim())
            .map((r) => ({ placa: r.placa, uf: r.uf || undefined, rntc: r.rntc || undefined })),
          volumes: volumes
            .map((v) => ({
              quantidade: v.quantidade && v.quantidade > 0 ? v.quantidade : undefined,
              especie: v.especie.trim() || undefined,
              marca: v.marca.trim() || undefined,
              numeracao: v.numeracao.trim() || undefined,
              pesoLiquido: parseDecimal(v.pesoLiquido) || undefined,
              pesoBruto: parseDecimal(v.pesoBruto) || undefined,
            }))
            .filter(
              (v) =>
                v.quantidade ||
                v.especie ||
                v.marca ||
                v.numeracao ||
                v.pesoLiquido ||
                v.pesoBruto,
            ),
        },
        referencias: referencias.map((r) => ({
          tipo: r.tipo,
          chave: r.chave,
          codigoUf: r.codigoUf,
          anoMes: r.anoMes,
          cnpj: r.cnpj,
          cpf: r.cpf,
          inscricaoEstadual: r.inscricaoEstadual,
          modelo: r.modelo,
          serie: r.serie,
          numero: r.numero,
        })),
      });
      setResultado(res as unknown as Record<string, unknown>);
      setEmailDanfe(destEmail.trim());
      setEmailMsg("");
      setEmailErro("");
      requestAnimationFrame(() => {
        document.getElementById("nfe-sucesso")?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
      await carregarContexto();
    } catch (e) {
      setErro(mapEmissaoError(e instanceof ApiError ? e.message : "Falha na emissão"));
    } finally {
      setLoading(false);
    }
  };

  const podeEmitir = ctx == null || ctx.podeEmitir;

  return (
    <div className="nfe-emissao-page">
      <EmitenteEmissaoBar dica="Troque o emitente para emitir com a numeração e os cadastros corretos — tudo recarrega sozinho.">
        <div className="grid max-w-xl grid-cols-1 gap-2 sm:grid-cols-[auto_1fr] sm:items-end">
          <label className="block text-xs font-semibold uppercase tracking-wide text-[var(--primary-700)]">
            Local / IE do emitente
            <select
              className="fiscal-input mt-1 min-w-[16rem]"
              value={enderecoId}
              onChange={(e) => setEnderecoId(e.target.value ? Number(e.target.value) : "")}
            >
              <option value="">Principal</option>
              {ctx?.enderecos?.map((e) => (
                <option key={e.id} value={e.id}>
                  {e.apelido} — IE {e.inscricaoEstadual ?? "—"}
                  {e.municipio ? ` · ${e.municipio}/${e.uf}` : ""}
                </option>
              ))}
            </select>
          </label>
          <p className="text-xs text-agro-muted sm:pb-2">
            Endereço e IE usados na NF-e deste emitente (não do cliente).
          </p>
        </div>
      </EmitenteEmissaoBar>
      <AssinaturaBanner compact />

      {!ambienteProd && (
        <div className="flex items-center gap-3 rounded-xl border border-amber-300 bg-amber-50 px-4 py-2.5">
          <span className="text-lg text-amber-500">⚠</span>
          <div className="text-sm text-amber-800">
            <strong>Homologação</strong> — esta NF-e não tem valor fiscal. Use só para teste com a SEFAZ.
          </div>
        </div>
      )}

      <header className="nfe-hero">
        <div className="nfe-hero__row">
          <div>
            <div className="nfe-hero__kicker">
              <Wheat className="h-3.5 w-3.5" />
              Emissor NF-e rural
            </div>
            <h1 className="nfe-hero__title">Nova nota de saída / entrada</h1>
            <p className="nfe-hero__sub">
              {ctx?.emitenteNome ?? ctx?.empresaNome ?? "—"}
              {ctx?.emitenteDocumento ? ` · ${formatarCnpjCpf(ctx.emitenteDocumento)}` : ""}
            </p>
          </div>
          <div className="nfe-hero__meta">
            <div className="nfe-hero__stat">
              <span>Série</span>
              <strong>{serieDisplay}</strong>
            </div>
            <div className="nfe-hero__stat">
              <span>Próxima NF-e</span>
              <strong>{numeroDisplay}</strong>
            </div>
            <div className="nfe-hero__stat">
              <span>Emissão</span>
              <strong style={{ fontSize: "0.95rem" }}>{fmtDataHora()}</strong>
            </div>
          </div>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className={`nfe-tag ${ambienteProd ? "ghost" : "warn"}`}>
            {ambienteProd ? "Produção" : "Homologação"}
          </span>
          {ultimoDisplay != null && (
            <span className="nfe-tag ghost">Última emitida: {ultimoDisplay}</span>
          )}
          <button
            type="button"
            className="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-white/10 px-3 py-1.5 text-xs font-medium hover:bg-white/20"
            onClick={() => void carregarContexto()}
          >
            <RefreshCw className="h-3.5 w-3.5" /> Atualizar numeração
          </button>
        </div>
      </header>

      {ctx && !ctx.podeEmitir && (
        <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">
          {ctx.aviso ?? "Emissão bloqueada — verifique certificado e configuração."}
        </p>
      )}

      {erro && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>
      )}

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>
            <UserRound className="h-4 w-4" /> Destinatário
          </h2>
        </div>
        <div className="nfe-panel__body">
          <div className="grid grid-cols-12 gap-3">
            <div className="col-span-12 md:col-span-5">
              <AutoCompleteField
                id="nfe-dest"
                label="Destinatário"
                placeholder="Nome ou CPF/CNPJ…"
                value={destOpt}
                suggestions={destSug}
                completeMethod={(e) => void buscarDest(e)}
                dropdown
                onChange={(v) => {
                  const opt = asOption(v);
                  if (opt && typeof v !== "string") {
                    void selecionarDest(opt);
                    return;
                  }
                  setDestOpt(opt);
                  setDestinatario(null);
                  setDestEnderecos([]);
                  aplicarDestEndereco(null);
                }}
              />
            </div>
            <div className="col-span-12 md:col-span-5">
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                IE-Endereço
              </label>
              <select
                className="fiscal-input"
                value={destEndKey}
                disabled={!destinatario}
                onChange={(e) => {
                  const opt = destEnderecos.find((x) => x.key === e.target.value) ?? null;
                  aplicarDestEndereco(opt);
                }}
              >
                <option value="">
                  {destinatario
                    ? destEnderecos.length
                      ? "Selecione"
                      : "Sem endereços cadastrados"
                    : "Selecione o destinatário"}
                </option>
                {destEnderecos.map((e) => (
                  <option key={e.key} value={e.key}>
                    {e.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-span-12 md:col-span-2">
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                E-mail DANFE
              </label>
              <input className="fiscal-input" value={destEmail} onChange={(e) => setDestEmail(e.target.value)} />
            </div>
          </div>
          {destEndereco && (
            <p className="mt-2 text-xs text-slate-500">
              Endereço da NF-e:{" "}
              <span className="font-medium text-slate-700">
                {[destEndereco.logradouro, destEndereco.numero].filter(Boolean).join(", ")}
                {destEndereco.bairro ? ` — ${destEndereco.bairro}` : ""}
                {destEndereco.municipio
                  ? ` · ${destEndereco.municipio}${destEndereco.uf ? `/${destEndereco.uf}` : ""}`
                  : ""}
                {destIe ? ` · IE ${destIe}` : ""}
              </span>
            </p>
          )}
          {destinatario && (
            <div className="nfe-dest-card">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white shadow-sm">
                <Leaf className="h-5 w-5 text-[var(--primary-600)]" />
              </div>
              <div>
                <strong>{destinatario.nome}</strong>
                <span>
                  {formatarCnpjCpf(destinatario.cpfCnpj) || "sem documento"}
                  {destIe ? ` · IE ${destIe}` : ""}
                </span>
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>
            <Wheat className="h-4 w-4" /> Natureza da operação
          </h2>
          {cfopPadrao ? <span className="nfe-cfop-chip">CFOP {cfopPadrao}</span> : null}
        </div>
        <div className="nfe-panel__body">
          <AutoCompleteField
            id="nfe-natureza"
            label="Natureza da operação"
            placeholder="Buscar natureza / CFOP…"
            value={natOpt}
            suggestions={natSug}
            completeMethod={buscarNatureza}
            forceSelection
            dropdown
            onChange={(v) => {
              const opt = asOption(v);
              setNatOpt(opt);
              if (opt && typeof v !== "string") {
                const op = (opt.raw as Operacao | undefined) ?? operacoes.find((o) => o.id === Number(opt.value));
                aplicarOperacao(op);
              } else if (!opt) {
                aplicarOperacao(undefined);
              }
            }}
          />
          <p className="mt-2 text-xs text-slate-500">
            O texto da NF-e (<strong>natOp</strong>) vem da operação fiscal cadastrada
            {naturezaOperacao ? (
              <>
                : <span className="font-medium text-slate-700">{naturezaOperacao}</span>
              </>
            ) : (
              "."
            )}
            {operacaoSel?.habilitarIbsCbs ? " · IBS/CBS da reforma inclusos." : ""}
          </p>
          <div className="nfe-config-grid mt-4">
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Finalidade
              </span>
              <select
                className="fiscal-input"
                value={finalidade}
                onChange={(e) => {
                  const v = e.target.value;
                  setFinalidade(v);
                  if (v !== "4") {
                    setReferencias([]);
                    setRefOpt(null);
                    setRefSug([]);
                  }
                  if (v === "4" || !geraFinanceiro(operacaoSel)) {
                    setMeioPagamento("90");
                    setIndicadorPagamento("0");
                  } else if (meioPagamento === "90") {
                    setMeioPagamento("01");
                  }
                }}
              >
                {FINALIDADES_NFE.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Consumidor final
              </span>
              <select
                className="fiscal-input"
                value={consumidorFinal}
                onChange={(e) => setConsumidorFinal(e.target.value)}
              >
                {CONSUMIDOR_FINAL.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Presença
              </span>
              <select
                className="fiscal-input"
                value={indicadorPresenca}
                onChange={(e) => setIndicadorPresenca(e.target.value)}
              >
                {PRESENCA_COMPRADOR.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Tipo pagamento
              </span>
              <select
                className="fiscal-input"
                value={meioPagamento}
                onChange={(e) => setMeioPagamento(e.target.value)}
              >
                {MEIOS_PAGAMENTO.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Prazo
              </span>
              <select
                className="fiscal-input"
                value={indicadorPagamento}
                onChange={(e) => setIndicadorPagamento(e.target.value)}
              >
                {PRAZOS_PAGAMENTO.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </label>
            <div className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Tipo da operação
              </span>
              <p className="fiscal-input bg-slate-50 text-slate-700">
                {isEntrada(operacaoSel?.tipoOperacao) ? "Entrada" : "Saída"}
                {geraFinanceiro(operacaoSel) ? " · gera financeiro" : " · sem financeiro"}
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>
            <Package className="h-4 w-4" /> Itens da nota
          </h2>
          <span className="text-xs text-slate-500">{itens.length} item(ns)</span>
        </div>
        <div className="nfe-panel__body space-y-4">
          <div className="nfe-add-grid">
            <div className="text-sm">
              <AutoCompleteField
                id="nfe-produto"
                label="Produto / mercadoria"
                placeholder="Nome ou código…"
                value={prodOpt}
                suggestions={prodSug}
                completeMethod={(e) => void buscarProd(e)}
                dropdown
                onChange={(v) => {
                  const opt = asOption(v);
                  if (opt && typeof v !== "string") {
                    void selecionarProd(opt);
                    return;
                  }
                  setProdOpt(opt);
                  setProdSel(null);
                }}
              />
            </div>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Un.</span>
              <select
                className="fiscal-input"
                value={addUnidade}
                onChange={(e) => setAddUnidade(e.target.value)}
              >
                {UNIDADES_PRODUTO.map((u) => (
                  <option key={u.sigla} value={u.sigla}>
                    {u.sigla}
                  </option>
                ))}
                {addUnidade && !UNIDADES_PRODUTO.some((u) => u.sigla === addUnidade) ? (
                  <option value={addUnidade}>{addUnidade}</option>
                ) : null}
              </select>
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Qtd</span>
              <QtyInput value={addQtd} onChange={setAddQtd} />
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Valor unit.
              </span>
              <MoedaInput value={addValor} onChange={setAddValor} placeholder="se não vier do cadastro" />
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Desconto
              </span>
              <MoedaInput value={addDesc} onChange={setAddDesc} placeholder="0,00" />
            </label>
            <button type="button" className="fiscal-btn-primary h-[42px]" onClick={() => void adicionarItem()}>
              <Plus className="h-4 w-4" /> Incluir
            </button>
          </div>

          {itens.length === 0 ? (
            <div className="nfe-empty">
              <Wheat className="mx-auto mb-2 h-8 w-8 text-[var(--primary-400)]" />
              Nenhum item. Busque a mercadoria, informe quantidade, valor (se o cadastro não tiver) e desconto.
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-[var(--border)]">
              <table className="nfe-grid">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Código</th>
                    <th>Descrição</th>
                    <th>NCM</th>
                    <th>CFOP</th>
                    <th>Un</th>
                    <th className="num">Qtd</th>
                    <th className="num">Vlr unit.</th>
                    <th className="num">Desconto</th>
                    <th className="num">Total</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {itens.map((i, idx) => (
                    <tr key={i.key}>
                      <td className="text-slate-400">{idx + 1}</td>
                      <td className="font-mono text-xs">{i.codigo}</td>
                      <td className="font-medium">{i.descricao}</td>
                      <td className="font-mono text-xs">{i.ncm}</td>
                      <td>{i.cfop || "—"}</td>
                      <td>{i.unidade}</td>
                      <td className="num">
                        <input
                          className="cell-input"
                          defaultValue={String(i.quantidade).replace(".", ",")}
                          onBlur={(e) => {
                            const n = parseDecimal(e.target.value);
                            atualizarItem(i.key, { quantidade: n > 0 ? n : i.quantidade });
                          }}
                        />
                      </td>
                      <td className="num">
                        <input
                          className="cell-input"
                          defaultValue={i.valorUnitario.toLocaleString("pt-BR", {
                            minimumFractionDigits: 2,
                          })}
                          onBlur={(e) => {
                            const n = parseDecimal(e.target.value);
                            atualizarItem(i.key, { valorUnitario: n > 0 ? n : i.valorUnitario });
                          }}
                        />
                      </td>
                      <td className="num">
                        <input
                          className="cell-input"
                          defaultValue={i.valorDesconto.toLocaleString("pt-BR", {
                            minimumFractionDigits: 2,
                          })}
                          onBlur={(e) => {
                            const n = Math.max(0, parseDecimal(e.target.value));
                            atualizarItem(i.key, { valorDesconto: n });
                          }}
                        />
                      </td>
                      <td className="num font-semibold">{fmtMoeda(liquido(i))}</td>
                      <td>
                        <button
                          type="button"
                          className="fiscal-btn-icon danger"
                          onClick={() => removerItem(i.key)}
                          aria-label="Remover item"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={6}>Somatório</td>
                    <td className="num">{totais.qtd.toLocaleString("pt-BR")}</td>
                    <td className="num">{fmtMoeda(totais.produtos)}</td>
                    <td className="num">{fmtMoeda(totais.descontos)}</td>
                    <td className="num total-nota">{fmtMoeda(totais.nota)}</td>
                    <td className="total-nota" />
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </div>
      </section>

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>
            <Truck className="h-4 w-4" /> Transporte
          </h2>
        </div>
        <div className="nfe-panel__body space-y-3">
          <div className="nfe-config-grid">
            <label className="text-sm md:col-span-2">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Modalidade do frete
              </span>
              <select className="fiscal-input" value={modFrete} onChange={(e) => setModFrete(e.target.value)}>
                {MODALIDADES_FRETE.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {modFrete !== "9" && (
            <>
              <div className="nfe-config-grid">
                <div className="text-sm md:col-span-2">
                  <AutoCompleteField
                    id="nfe-transp"
                    label="Transportadora"
                    placeholder="Nome ou CNPJ…"
                    value={transpOpt}
                    suggestions={transpSug}
                    completeMethod={(e) => void buscarTransportadora(e)}
                    dropdown
                    onChange={(v) => {
                      const opt = asOption(v);
                      if (opt && typeof v !== "string") {
                        void selecionarTransportadora(opt);
                        return;
                      }
                      setTranspOpt(opt);
                      if (typeof v === "string") {
                        setTranspNome(v);
                      }
                    }}
                  />
                </div>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    CNPJ/CPF
                  </span>
                  <input className="fiscal-input" value={transpDoc} onChange={(e) => setTranspDoc(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">IE</span>
                  <input className="fiscal-input" value={transpIe} onChange={(e) => setTranspIe(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Município
                  </span>
                  <input className="fiscal-input" value={transpMun} onChange={(e) => setTranspMun(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">UF</span>
                  <select className="fiscal-input" value={transpUf} onChange={(e) => setTranspUf(e.target.value)}>
                    <option value="">—</option>
                    {UFS_IBGE.map((u) => (
                      <option key={u.sigla} value={u.sigla}>
                        {u.sigla}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="text-sm">
                  <AutoCompleteField
                    id="nfe-placa"
                    label="Placa"
                    placeholder="Digite ou escolha…"
                    value={placaOpt ?? (placa ? { label: placa, value: placa } : null)}
                    suggestions={placaSug}
                    completeMethod={buscarPlaca}
                    dropdown
                    onChange={(v) => {
                      const opt = asOption(v);
                      setPlacaOpt(opt);
                      if (opt) {
                        const placaVal = (typeof v === "string" ? v : opt.value).toUpperCase();
                        setPlaca(placaVal);
                      } else {
                        setPlaca("");
                      }
                    }}
                  />
                </div>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    UF placa
                  </span>
                  <select className="fiscal-input" value={placaUf} onChange={(e) => setPlacaUf(e.target.value)}>
                    <option value="">—</option>
                    {UFS_IBGE.map((u) => (
                      <option key={u.sigla} value={u.sigla}>
                        {u.sigla}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">RNTC</span>
                  <input className="fiscal-input" value={rntc} onChange={(e) => setRntc(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Valor do frete
                  </span>
                  <MoedaInput value={valorFrete} onChange={setValorFrete} placeholder="0,00" />
                </label>
              </div>
              <div className="flex items-center justify-between">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Volumes</p>
                <button
                  type="button"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-[var(--primary-700)]"
                  onClick={() =>
                    setVolumes((prev) => [
                      ...prev,
                      {
                        key: String(Date.now()),
                        quantidade: 1,
                        especie: "",
                        marca: "",
                        numeracao: "",
                        pesoLiquido: "",
                        pesoBruto: "",
                      },
                    ])
                  }
                >
                  <Plus className="h-3.5 w-3.5" /> Incluir volume
                </button>
              </div>
              {volumes.length === 0 ? (
                <p className="text-xs text-slate-400">Nenhum volume. Inclua qtd, espécie, marca, numeração e pesos.</p>
              ) : (
                volumes.map((v) => (
                  <div key={v.key} className="nfe-config-grid">
                    <label className="text-sm">
                      <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                        Qtd
                      </span>
                      <QtyInput
                        value={v.quantidade}
                        onChange={(n) =>
                          setVolumes((prev) =>
                            prev.map((x) => (x.key === v.key ? { ...x, quantidade: n } : x)),
                          )
                        }
                      />
                    </label>
                    <label className="text-sm">
                      <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                        Espécie
                      </span>
                      <input
                        className="fiscal-input"
                        value={v.especie}
                        onChange={(e) =>
                          setVolumes((prev) =>
                            prev.map((x) => (x.key === v.key ? { ...x, especie: e.target.value } : x)),
                          )
                        }
                      />
                    </label>
                    <label className="text-sm">
                      <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                        Marca
                      </span>
                      <input
                        className="fiscal-input"
                        value={v.marca}
                        onChange={(e) =>
                          setVolumes((prev) =>
                            prev.map((x) => (x.key === v.key ? { ...x, marca: e.target.value } : x)),
                          )
                        }
                      />
                    </label>
                    <label className="text-sm">
                      <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                        Numeração
                      </span>
                      <input
                        className="fiscal-input"
                        value={v.numeracao}
                        onChange={(e) =>
                          setVolumes((prev) =>
                            prev.map((x) => (x.key === v.key ? { ...x, numeracao: e.target.value } : x)),
                          )
                        }
                      />
                    </label>
                    <label className="text-sm">
                      <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                        Peso líquido
                      </span>
                      <input
                        className="fiscal-input"
                        value={v.pesoLiquido}
                        onChange={(e) =>
                          setVolumes((prev) =>
                            prev.map((x) => (x.key === v.key ? { ...x, pesoLiquido: e.target.value } : x)),
                          )
                        }
                      />
                    </label>
                    <label className="text-sm">
                      <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                        Peso bruto
                      </span>
                      <input
                        className="fiscal-input"
                        value={v.pesoBruto}
                        onChange={(e) =>
                          setVolumes((prev) =>
                            prev.map((x) => (x.key === v.key ? { ...x, pesoBruto: e.target.value } : x)),
                          )
                        }
                      />
                    </label>
                    <button
                      type="button"
                      className="fiscal-btn-icon danger self-end"
                      aria-label="Remover volume"
                      onClick={() => setVolumes((prev) => prev.filter((x) => x.key !== v.key))}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                ))
              )}
              <div className="flex items-center justify-between">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Reboques</p>
                <button
                  type="button"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-[var(--primary-700)]"
                  onClick={() =>
                    setReboques((prev) => [
                      ...prev,
                      { key: String(Date.now()), placa: "", uf: placaUf || ufEmitente, rntc: "" },
                    ])
                  }
                >
                  <Plus className="h-3.5 w-3.5" /> Incluir reboque
                </button>
              </div>
              {reboques.map((r) => (
                <div key={r.key} className="nfe-config-grid">
                  <input
                    className="fiscal-input"
                    placeholder="Placa"
                    value={r.placa}
                    onChange={(e) =>
                      setReboques((prev) =>
                        prev.map((x) => (x.key === r.key ? { ...x, placa: e.target.value.toUpperCase() } : x)),
                      )
                    }
                  />
                  <select
                    className="fiscal-input"
                    value={r.uf}
                    onChange={(e) =>
                      setReboques((prev) => prev.map((x) => (x.key === r.key ? { ...x, uf: e.target.value } : x)))
                    }
                  >
                    <option value="">UF</option>
                    {UFS_IBGE.map((u) => (
                      <option key={u.sigla} value={u.sigla}>
                        {u.sigla}
                      </option>
                    ))}
                  </select>
                  <input
                    className="fiscal-input"
                    placeholder="RNTC"
                    value={r.rntc}
                    onChange={(e) =>
                      setReboques((prev) => prev.map((x) => (x.key === r.key ? { ...x, rntc: e.target.value } : x)))
                    }
                  />
                  <button
                    type="button"
                    className="fiscal-btn-icon danger"
                    onClick={() => setReboques((prev) => prev.filter((x) => x.key !== r.key))}
                    aria-label="Remover reboque"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </>
          )}
        </div>
      </section>

      {finalidade === "4" && (
      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>
            <FileStack className="h-4 w-4" /> Documentos referenciados
          </h2>
          <span className="text-xs font-medium text-amber-700">Obrigatório na devolução</span>
        </div>
        <div className="nfe-panel__body space-y-3">
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className={`nfe-chip ${refTipo === "NFE" ? "on" : ""}`}
              onClick={() => setRefTipo("NFE")}
            >
              Chave NF-e (44 dígitos)
            </button>
            <button
              type="button"
              className={`nfe-chip ${refTipo === "NFP" ? "on" : ""}`}
              onClick={() => setRefTipo("NFP")}
            >
              NF produtor rural
            </button>
          </div>
          {refTipo === "NFE" ? (
            <div>
              <AutoCompleteField
                id="nfe-ref"
                label="Buscar nota emitida / DF-e de entrada ou colar a chave"
                placeholder="Número, chave ou emitente…"
                value={refOpt}
                suggestions={refSug}
                completeMethod={(e) => void buscarRef(e)}
                dropdown
                onChange={(v) => {
                  const opt = asOption(v);
                  if (opt && typeof v !== "string") {
                    aplicarChaveRef(opt.value, opt.label);
                    return;
                  }
                  setRefOpt(opt);
                  if (typeof v === "string") {
                    const digits = v.replace(/\D/g, "").slice(0, 44);
                    if (digits.length === 44) {
                      aplicarChaveRef(digits);
                    }
                  }
                }}
              />
            </div>
          ) : (
            <div className="nfe-config-grid">
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">UF</span>
                <select className="fiscal-input" value={nfpUf} onChange={(e) => setNfpUf(e.target.value)}>
                  <option value="">Selecione</option>
                  {UFS_IBGE.map((u) => (
                    <option key={u.sigla} value={u.ibge}>
                      {u.sigla} ({u.ibge})
                    </option>
                  ))}
                </select>
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Ano/mês (21/08)
                </span>
                <input
                  className="fiscal-input"
                  placeholder="21/08"
                  value={nfpAamm}
                  onChange={(e) => setNfpAamm(e.target.value)}
                />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">CNPJ</span>
                <input className="fiscal-input" value={nfpCnpj} onChange={(e) => setNfpCnpj(e.target.value)} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">CPF</span>
                <input className="fiscal-input" value={nfpCpf} onChange={(e) => setNfpCpf(e.target.value)} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">IE</span>
                <input className="fiscal-input" value={nfpIe} onChange={(e) => setNfpIe(e.target.value)} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Modelo</span>
                <select className="fiscal-input" value={nfpModelo} onChange={(e) => setNfpModelo(e.target.value)}>
                  {MODELOS_NFP.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Série</span>
                <input className="fiscal-input" value={nfpSerie} onChange={(e) => setNfpSerie(e.target.value)} />
              </label>
              <label className="text-sm">
                <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Número</span>
                <input className="fiscal-input" value={nfpNumero} onChange={(e) => setNfpNumero(e.target.value)} />
              </label>
            </div>
          )}
          <button type="button" className="fiscal-btn-primary h-[42px]" onClick={incluirReferencia}>
            <Plus className="h-4 w-4" /> Incluir referência
          </button>
          {referencias.length === 0 ? (
            <p className="text-sm text-slate-500">
              Nenhuma nota referenciada. Informe a NF-e ou NF de produtor rural original da devolução.
            </p>
          ) : (
            <ul className="space-y-2">
              {referencias.map((r) => (
                <li key={r.key} className="nfe-ref-item">
                  <div>
                    <strong>{r.tipo === "NFP" ? "NF produtor rural" : r.tipo === "CTE" ? "CT-e" : "NF-e"}</strong>
                    <span>{r.rotulo || r.chave}</span>
                  </div>
                  <button
                    type="button"
                    className="fiscal-btn-icon danger"
                    onClick={() => setReferencias((prev) => prev.filter((x) => x.key !== r.key))}
                    aria-label="Remover referência"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
      )}

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>Informações adicionais do contribuinte</h2>
        </div>
        <div className="nfe-panel__body">
          <textarea
            className="fiscal-input min-h-[5.5rem]"
            placeholder="Observações que vão no XML (infCpl) e no DANFE."
            value={informacoesAdicionais}
            onChange={(e) => setInformacoesAdicionais(e.target.value)}
            maxLength={5000}
          />
        </div>
      </section>

      {resultado && (
        <div id="nfe-sucesso" className="nfe-panel border-[var(--primary-200)] bg-[var(--primary-50)]">
          <div className="nfe-panel__body space-y-3 text-sm">
            <p className="flex items-center gap-2 text-base font-semibold text-[var(--primary-800)]">
              <CheckCircle className="h-5 w-5" /> NF-e transmitida
            </p>
            <p>
              <strong>Status:</strong> {String(resultado.statusProtocolo ?? resultado.status)}
            </p>
            <p>
              <strong>Chave:</strong>{" "}
              <span className="break-all font-mono text-xs">{String(resultado.chaveAcesso ?? "—")}</span>
            </p>
            <p>
              <strong>Protocolo:</strong> {String(resultado.protocolo ?? "—")}
            </p>
            {resultado.motivoProtocolo != null || resultado.motivo != null ? (
              <p className="text-slate-600">{String(resultado.motivoProtocolo ?? resultado.motivo)}</p>
            ) : null}
            {chaveAutorizada && resultado.chaveAcesso != null && (
              <>
                <div className="flex flex-wrap gap-2 pt-1">
                  <button
                    type="button"
                    className="inline-flex items-center gap-2 rounded-lg bg-[var(--primary-700)] px-3 py-2 text-sm font-medium text-white hover:bg-[var(--primary-800)]"
                    onClick={() => void imprimirDanfe(String(resultado.chaveAcesso))}
                  >
                    <Printer className="h-4 w-4" /> Imprimir DANFE
                  </button>
                  <button
                    type="button"
                    className="inline-flex items-center gap-2 rounded-lg border border-[var(--primary-300)] bg-white px-3 py-2 text-sm font-medium text-[var(--primary-800)] hover:bg-white"
                    onClick={() => void abrirDanfe(String(resultado.chaveAcesso))}
                  >
                    <FileDown className="h-4 w-4" /> Abrir PDF
                  </button>
                </div>
                <div className="rounded-xl border border-[var(--primary-200)] bg-white p-3">
                  <p className="mb-2 text-sm font-medium text-slate-800">Enviar DANFE por e-mail</p>
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <input
                      type="email"
                      className="fiscal-input flex-1"
                      placeholder="destinatario@empresa.com.br"
                      value={emailDanfe}
                      onChange={(e) => setEmailDanfe(e.target.value)}
                    />
                    <button
                      type="button"
                      className="inline-flex items-center justify-center gap-2 rounded-lg bg-[var(--primary-700)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-800)] disabled:opacity-50"
                      disabled={enviandoEmail || !emailDanfe.trim()}
                      onClick={() => void enviarDanfeEmail()}
                    >
                      {enviandoEmail ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Mail className="h-4 w-4" />
                      )}
                      Enviar
                    </button>
                  </div>
                  {emailMsg ? <p className="mt-2 text-sm text-green-700">{emailMsg}</p> : null}
                  {emailErro ? <p className="mt-2 text-sm text-red-600">{emailErro}</p> : null}
                </div>
              </>
            )}
          </div>
        </div>
      )}

      <div className="nfe-dock">
        <div className="nfe-dock__totais">
          <div>
            Itens
            <strong>{itens.length}</strong>
          </div>
          <div>
            Produtos
            <strong>{fmtMoeda(totais.produtos)}</strong>
          </div>
          <div>
            Descontos
            <strong>{fmtMoeda(totais.descontos)}</strong>
          </div>
          <div className="destaque">
            Total da NF-e
            <strong>{fmtMoeda(totais.nota)}</strong>
          </div>
        </div>
        <button
          type="button"
          className="fiscal-btn-primary px-6 py-2.5 text-sm"
          disabled={loading || !podeEmitir}
          onClick={() => void transmitir()}
        >
          <Send className="h-4 w-4" />
          {loading ? "Transmitindo à SEFAZ…" : "Transmitir NF-e"}
        </button>
      </div>
    </div>
  );
}
