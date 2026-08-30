"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  CheckCircle,
  FileDown,
  FileStack,
  Leaf,
  Package,
  Plus,
  RefreshCw,
  Search,
  Send,
  Trash2,
  Truck,
  UserRound,
  Wheat,
} from "lucide-react";
import { api, ApiError, formatarCnpjCpf, type NfeContexto, type NfeItemBody } from "@/lib/api";
import { fiscalApi, type VeiculoDto } from "@/lib/fiscal-api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { AssinaturaBanner } from "@/components/conta/AssinaturaBanner";
import { mapEmissaoError } from "@/lib/assinatura";
import { fmtCfop } from "@/lib/cfop";
import { MoedaInput, QtyInput, fmtMoeda } from "@/components/fiscal/MoedaInput";
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

  const [enderecoId, setEnderecoId] = useState<number | "">("");
  const [operacaoFiscalId, setOperacaoFiscalId] = useState<number | "">("");
  const [naturezaOperacao, setNaturezaOperacao] = useState("");

  const [destQuery, setDestQuery] = useState("");
  const [destSug, setDestSug] = useState<PessoaSug[]>([]);
  const [destinatario, setDestinatario] = useState<PessoaSug | null>(null);
  const [destEmail, setDestEmail] = useState("");
  const [destIe, setDestIe] = useState("");

  const [itens, setItens] = useState<ItemLinha[]>([]);
  const [prodQuery, setProdQuery] = useState("");
  const [prodSug, setProdSug] = useState<ProdutoSug[]>([]);
  const [prodSel, setProdSel] = useState<ProdutoSug | null>(null);
  const [addQtd, setAddQtd] = useState<number | undefined>(1);
  const [addValor, setAddValor] = useState<number | undefined>(undefined);
  const [addDesc, setAddDesc] = useState<number | undefined>(undefined);

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
  const [transpQuery, setTranspQuery] = useState("");
  const [transpSug, setTranspSug] = useState<PessoaSug[]>([]);
  const [placa, setPlaca] = useState("");
  const [placaUf, setPlacaUf] = useState("");
  const [rntc, setRntc] = useState("");
  const [valorFrete, setValorFrete] = useState<number | undefined>(undefined);
  const [volQtd, setVolQtd] = useState<number | undefined>(undefined);
  const [volEsp, setVolEsp] = useState("");
  const [volMarca, setVolMarca] = useState("");
  const [volNum, setVolNum] = useState("");
  const [pesoL, setPesoL] = useState("");
  const [pesoB, setPesoB] = useState("");
  const [reboques, setReboques] = useState<ReboqueLinha[]>([]);
  const [veiculos, setVeiculos] = useState<VeiculoDto[]>([]);

  const [referencias, setReferencias] = useState<RefLinha[]>([]);
  const [refTipo, setRefTipo] = useState<"NFE" | "NFP">("NFE");
  const [refChave, setRefChave] = useState("");
  const [refQuery, setRefQuery] = useState("");
  const [refSug, setRefSug] = useState<Array<{ rotulo: string; chave: string }>>([]);
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
      return;
    }
    setOperacaoFiscalId(op.id);
    setNaturezaOperacao(naturezaDaOperacao(op));
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
    setDestQuery("");
    setProdSel(null);
    setProdQuery("");
    setAddValor(undefined);
    setAddDesc(undefined);
    setAddQtd(1);
    setOperacaoFiscalId("");
    setNaturezaOperacao("");
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
    setPlaca("");
    setPlacaUf("");
    setRntc("");
    setValorFrete(undefined);
    setVolQtd(undefined);
    setVolEsp("");
    setVolMarca("");
    setVolNum("");
    setPesoL("");
    setPesoB("");
    setReboques([]);
    setReferencias([]);
    setRefChave("");
    setRefQuery("");
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

  const buscarDest = async (q: string) => {
    setDestQuery(q);
    setDestinatario(null);
    if (q.trim().length < 2) {
      setDestSug([]);
      return;
    }
    try {
      setDestSug(await fiscalApi.buscaPessoas(q));
    } catch {
      setDestSug([]);
    }
  };

  const selecionarDest = async (p: PessoaSug) => {
    setDestinatario(p);
    setDestQuery(`${p.nome} — ${formatarCnpjCpf(p.cpfCnpj || "")}`);
    setDestSug([]);
    try {
      const full = await fiscalApi.get<{ email?: string; inscricaoEstadual?: string }>(
        "/api/pessoas",
        p.id,
      );
      setDestEmail(full.email ?? "");
      setDestIe(full.inscricaoEstadual ?? "");
    } catch {
      /* opcional */
    }
  };

  const buscarProd = async (q: string) => {
    setProdQuery(q);
    setProdSel(null);
    if (q.trim().length < 2) {
      setProdSug([]);
      return;
    }
    try {
      setProdSug(await fiscalApi.buscaProdutos(q));
    } catch {
      setProdSug([]);
    }
  };

  const selecionarProd = async (p: ProdutoSug) => {
    setProdSel(p);
    setProdQuery(`${p.codigo} — ${p.nome}`);
    setProdSug([]);
    setAddValor(p.valorUnitario && p.valorUnitario > 0 ? p.valorUnitario : undefined);
    try {
      const full = await fiscalApi.get<{ valorUnitario?: number }>("/api/produto", p.id);
      if (full.valorUnitario && full.valorUnitario > 0) {
        setAddValor(full.valorUnitario);
      }
    } catch {
      /* usa valor da busca */
    }
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
          unidade: p.unidade || prodSel.unidade || "UN",
          quantidade: qtd,
          valorUnitario: vlr,
          valorDesconto: desc,
        },
      ]);
      setProdSel(null);
      setProdQuery("");
      setAddQtd(1);
      setAddValor(undefined);
      setAddDesc(undefined);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar produto");
    }
  };

  const removerItem = (key: string) => setItens((prev) => prev.filter((i) => i.key !== key));

  const buscarTransportadora = async (q: string) => {
    setTranspQuery(q);
    if (q.trim().length < 2) {
      setTranspSug([]);
      return;
    }
    try {
      setTranspSug(await fiscalApi.buscaPessoas(q));
    } catch {
      setTranspSug([]);
    }
  };

  const selecionarTransportadora = async (p: PessoaSug) => {
    setTranspNome(p.nome);
    setTranspDoc(p.cpfCnpj || "");
    setTranspQuery(`${p.nome} — ${formatarCnpjCpf(p.cpfCnpj || "")}`);
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

  const buscarRef = async (q: string) => {
    setRefQuery(q);
    if (!token || q.trim().length < 2) {
      setRefSug([]);
      return;
    }
    try {
      const [proprias, entradas] = await Promise.all([
        api.nfeListarNotas(token, q).catch(() => ({ itens: [] })),
        api.nfeListarEntradas(token, q).catch(() => []),
      ]);
      const lista: Array<{ rotulo: string; chave: string }> = [];
      for (const n of proprias.itens ?? []) {
        if (!n.chave) continue;
        lista.push({
          rotulo: `Emitida nº ${n.numero ?? "—"} / ${n.serie ?? "—"} · ${n.chave}`,
          chave: n.chave,
        });
      }
      for (const n of entradas) {
        if (!n.chave) continue;
        lista.push({
          rotulo: `Entrada nº ${n.numero ?? "—"} · ${n.nomeEmitente ?? "emitente"} · ${n.chave}`,
          chave: n.chave,
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
    setRefQuery("");
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
      setRefChave("");
    } else {
      setRefChave(parsed.chave);
    }
  };

  const incluirReferencia = () => {
    setErro("");
    if (refTipo === "NFE") {
      const parsed = parseChaveNfe(refChave || refQuery);
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
      setRefChave("");
      setRefQuery("");
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
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas/${chave}/danfe`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Falha ao abrir DANFE");
      const blob = await res.blob();
      window.open(URL.createObjectURL(blob), "_blank", "noopener,noreferrer");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao abrir DANFE");
    }
  };

  const transmitir = async () => {
    if (!token) return;
    if (!destinatario) {
      setErro("Selecione o destinatário (produtor, cerealista, cooperativa…).");
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
    if ((finalidade === "2" || finalidade === "4") && referencias.length === 0) {
      setErro("Devolução e complementar exigem ao menos um documento referenciado (chave NF-e ou NF de produtor rural).");
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
          volumeQuantidade: volQtd && volQtd > 0 ? volQtd : undefined,
          volumeEspecie: volEsp.trim() || undefined,
          volumeMarca: volMarca.trim() || undefined,
          volumeNumeracao: volNum.trim() || undefined,
          pesoLiquido: parseDecimal(pesoL) || undefined,
          pesoBruto: parseDecimal(pesoB) || undefined,
          valorFrete: valorFrete && valorFrete > 0 ? valorFrete : undefined,
          reboques: reboques
            .filter((r) => r.placa.trim())
            .map((r) => ({ placa: r.placa, uf: r.uf || undefined, rntc: r.rntc || undefined })),
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
      <EmitenteEmissaoBar dica="Troque o emitente para emitir pela fazenda, cerealista ou matriz certa — numeração e cadastros recarregam sozinhos." />
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
            <div className="col-span-12 md:col-span-6">
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Cliente / produtor / cerealista
              </label>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  className="fiscal-input nfe-search"
                  placeholder="Nome ou CPF/CNPJ…"
                  value={destQuery}
                  onChange={(e) => void buscarDest(e.target.value)}
                />
              </div>
              {destSug.length > 0 && (
                <ul className="nfe-sug">
                  {destSug.map((p) => (
                    <li key={p.id}>
                      <button type="button" onClick={() => void selecionarDest(p)}>
                        <strong>{p.nome}</strong>
                        <span className="ml-2 text-slate-500">{formatarCnpjCpf(p.cpfCnpj || "") || "—"}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <div className="col-span-12 md:col-span-3">
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                E-mail DANFE
              </label>
              <input className="fiscal-input" value={destEmail} onChange={(e) => setDestEmail(e.target.value)} />
            </div>
            <div className="col-span-12 md:col-span-3">
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Inscrição estadual
              </label>
              <input className="fiscal-input" value={destIe} onChange={(e) => setDestIe(e.target.value)} />
            </div>
            <div className="col-span-12 md:col-span-6">
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Local / IE do emitente
              </label>
              <select
                className="fiscal-input"
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
            </div>
          </div>
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
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
            Natureza da operação
          </label>
          <select
            className="fiscal-input"
            value={operacaoFiscalId}
            onChange={(e) => {
              const id = e.target.value ? Number(e.target.value) : "";
              aplicarOperacao(operacoes.find((o) => o.id === id));
            }}
          >
            <option value="">Selecione a natureza…</option>
            {saidas.length > 0 && (
              <optgroup label="Saídas (venda, remessa, devolução de compra)">
                {saidas.map((o) => (
                  <option key={o.id} value={o.id}>
                    {labelNatureza(o)}
                  </option>
                ))}
              </optgroup>
            )}
            {entradas.length > 0 && (
              <optgroup label="Entradas (compra, retorno, devolução de venda)">
                {entradas.map((o) => (
                  <option key={o.id} value={o.id}>
                    {labelNatureza(o)}
                  </option>
                ))}
              </optgroup>
            )}
            {outras.length > 0 && (
              <optgroup label="Outras">
                {outras.map((o) => (
                  <option key={o.id} value={o.id}>
                    {labelNatureza(o)}
                  </option>
                ))}
              </optgroup>
            )}
          </select>
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
            <label className="text-sm">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Produto / mercadoria
              </span>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  className="fiscal-input nfe-search"
                  placeholder="Soja, milho, suíno, código…"
                  value={prodQuery}
                  onChange={(e) => void buscarProd(e.target.value)}
                />
              </div>
              {prodSug.length > 0 && (
                <ul className="nfe-sug">
                  {prodSug.map((p) => (
                    <li key={p.id}>
                      <button type="button" onClick={() => void selecionarProd(p)}>
                        <span className="font-mono text-xs text-slate-500">{p.codigo}</span>{" "}
                        <strong>{p.nome}</strong>
                        {p.valorUnitario ? (
                          <span className="ml-2 text-slate-500">{fmtMoeda(p.valorUnitario)}</span>
                        ) : (
                          <span className="ml-2 text-amber-700">sem preço</span>
                        )}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
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
                <label className="text-sm md:col-span-2">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Transportadora
                  </span>
                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                    <input
                      className="fiscal-input nfe-search"
                      placeholder="Nome ou CNPJ…"
                      value={transpQuery || transpNome}
                      onChange={(e) => void buscarTransportadora(e.target.value)}
                    />
                  </div>
                  {transpSug.length > 0 && (
                    <ul className="nfe-sug">
                      {transpSug.map((p) => (
                        <li key={p.id}>
                          <button type="button" onClick={() => void selecionarTransportadora(p)}>
                            <strong>{p.nome}</strong>
                            <span className="ml-2 text-slate-500">{formatarCnpjCpf(p.cpfCnpj || "")}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </label>
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
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Placa</span>
                  <select className="fiscal-input" value={placa} onChange={(e) => setPlaca(e.target.value.toUpperCase())}>
                    <option value="">Digite ou escolha</option>
                    {veiculos.map((v) => (
                      <option key={v.placa} value={v.placa}>
                        {v.placa}
                        {v.modelo ? ` · ${v.modelo}` : ""}
                      </option>
                    ))}
                  </select>
                  <input
                    className="fiscal-input mt-1"
                    placeholder="ou informe a placa"
                    value={placa}
                    onChange={(e) => setPlaca(e.target.value.toUpperCase())}
                  />
                </label>
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
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Volume</p>
              <div className="nfe-config-grid">
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Qtd</span>
                  <QtyInput value={volQtd} onChange={setVolQtd} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Espécie
                  </span>
                  <input className="fiscal-input" value={volEsp} onChange={(e) => setVolEsp(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Marca</span>
                  <input className="fiscal-input" value={volMarca} onChange={(e) => setVolMarca(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Numeração
                  </span>
                  <input className="fiscal-input" value={volNum} onChange={(e) => setVolNum(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Peso líquido
                  </span>
                  <input className="fiscal-input" value={pesoL} onChange={(e) => setPesoL(e.target.value)} />
                </label>
                <label className="text-sm">
                  <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Peso bruto
                  </span>
                  <input className="fiscal-input" value={pesoB} onChange={(e) => setPesoB(e.target.value)} />
                </label>
              </div>
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

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>
            <FileStack className="h-4 w-4" /> Documentos referenciados
          </h2>
          {(finalidade === "2" || finalidade === "4") && (
            <span className="text-xs font-medium text-amber-700">Obrigatório nesta finalidade</span>
          )}
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
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">
                Buscar nota emitida / DF-e de entrada ou colar a chave
              </label>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  className="fiscal-input nfe-search"
                  placeholder="Número, chave ou emitente…"
                  value={refQuery || refChave}
                  onChange={(e) => {
                    const v = e.target.value;
                    setRefChave(v.replace(/\D/g, "").slice(0, 44));
                    void buscarRef(v);
                  }}
                />
              </div>
              {refSug.length > 0 && (
                <ul className="nfe-sug">
                  {refSug.map((n) => (
                    <li key={n.chave}>
                      <button type="button" onClick={() => aplicarChaveRef(n.chave, n.rotulo)}>
                        {n.rotulo}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
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
              Nenhuma nota referenciada. Use na devolução, complementar ou quando precisar apontar a NF original.
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

      <section className="nfe-panel">
        <div className="nfe-panel__head">
          <h2>Informações adicionais do contribuinte</h2>
        </div>
        <div className="nfe-panel__body">
          <textarea
            className="fiscal-input min-h-[5.5rem]"
            placeholder="Texto que vai no XML (infCpl) e no DANFE — observação da operação, dados do produtor, etc."
            value={informacoesAdicionais}
            onChange={(e) => setInformacoesAdicionais(e.target.value)}
            maxLength={5000}
          />
        </div>
      </section>

      {resultado && (
        <div className="nfe-panel border-[var(--primary-200)] bg-[var(--primary-50)]">
          <div className="nfe-panel__body space-y-2 text-sm">
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
            {resultado.chaveAcesso != null && (
              <button
                type="button"
                className="inline-flex items-center gap-2 rounded-lg border border-[var(--primary-300)] bg-white px-3 py-2 text-sm font-medium text-[var(--primary-800)] hover:bg-white"
                onClick={() => void abrirDanfe(String(resultado.chaveAcesso))}
              >
                <FileDown className="h-4 w-4" /> Abrir DANFE (PDF)
              </button>
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
