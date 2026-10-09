"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Copy,
  Download,
  FileArchive,
  Loader2,
  Printer,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { ApiError, empresaPortalApi } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";

type Nota = {
  id: number;
  chave: string;
  serie: string;
  numero: number;
  statusProtocolo?: string;
  motivoProtocolo?: string;
  createdAt?: string;
  temXml?: boolean;
  ambiente?: string;
  empresaId?: number;
  emitenteNome?: string;
  emitenteDocumento?: string;
  valorTotal?: number | string | null;
  cliente?: string | null;
  destinatarioNome?: string | null;
  cfop?: string | null;
  tipoOperacao?: string | null;
  produtos?: string[];
};

type EmitenteOpt = { id: number; nome: string; documento?: string };

type Filtros = {
  de: string;
  ate: string;
  q: string;
  serie: string;
  status: string;
  ambiente: string;
  emitente: string;
};

const PAGE_SIZE = 20;

const FILTROS_VAZIOS: Filtros = {
  de: "",
  ate: "",
  q: "",
  serie: "",
  status: "",
  ambiente: "todos",
  emitente: "",
};

function formatarData(iso?: string): string {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("pt-BR");
  } catch {
    return iso.slice(0, 19);
  }
}

function statusLabel(status?: string): string {
  if (!status) return "—";
  if (status === "100") return "Autorizada";
  if (status === "101") return "Cancelada";
  if (status === "110") return "Denegada";
  return status;
}

function formatarValor(v?: number | string | null): string {
  if (v === null || v === undefined || v === "") return "—";
  const n = typeof v === "number" ? v : Number(String(v).replace(",", "."));
  if (Number.isNaN(n)) return "—";
  return n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function produtosDaNota(n: Nota): string[] {
  if (Array.isArray(n.produtos) && n.produtos.length > 0) return n.produtos;
  return ["—"];
}

export function NfeNotasEmitidas() {
  const router = useRouter();
  const token = getAppToken();
  const fimLista = useRef<HTMLDivElement | null>(null);
  const carregandoMais = useRef(false);
  const [itens, setItens] = useState<Nota[]>([]);
  const [emitentes, setEmitentes] = useState<EmitenteOpt[]>([]);
  const [totalHomologacao, setTotalHomologacao] = useState(0);
  const [filtros, setFiltros] = useState<Filtros>({ ...FILTROS_VAZIOS });
  const [filtrosAplicados, setFiltrosAplicados] = useState<Filtros>({ ...FILTROS_VAZIOS });
  /** Só true depois de clicar em Filtrar — evita carregar tudo no open da tela. */
  const [consultaFeita, setConsultaFeita] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [valorTotalSomado, setValorTotalSomado] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [selecionados, setSelecionados] = useState<Set<string>>(new Set());
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [exportando, setExportando] = useState<string | null>(null);

  const montarParams = useCallback(
    (page: number, f: Filtros) => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(PAGE_SIZE),
      });
      if (f.de) params.set("de", f.de);
      if (f.ate) params.set("ate", f.ate);
      if (f.q.trim()) params.set("q", f.q.trim());
      if (f.serie.trim()) params.set("serie", f.serie.trim());
      if (f.status.trim()) params.set("status", f.status.trim());
      if (f.emitente) params.set("emitente", f.emitente);
      params.set("ambiente", f.ambiente || "todos");
      return params;
    },
    [],
  );

  const buscarPagina = useCallback(
    async (page: number, f: Filtros) => {
      if (!token) return null;
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas?${montarParams(page, f)}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError(
          (body as { erro?: string; message?: string }).erro ??
            (body as { message?: string }).message ??
            "Falha ao listar notas",
          res.status,
        );
      }
      return (await res.json()) as {
        itens: Nota[];
        hasMore: boolean;
        totalElements: number;
        valorTotalSomado?: number | string;
        emitentes?: EmitenteOpt[];
        totalHomologacao?: number;
      };
    },
    [token, montarParams],
  );

  const carregar = useCallback(
    async (f: Filtros = filtrosAplicados) => {
      if (!token) {
        setItens([]);
        setLoading(false);
        return;
      }
      setLoading(true);
      setErro("");
      setSelecionados(new Set());
      try {
        const data = await buscarPagina(0, f);
        setItens(data?.itens ?? []);
        setHasMore(data?.hasMore ?? false);
        setTotalElements(data?.totalElements ?? 0);
        setValorTotalSomado(Number(data?.valorTotalSomado ?? 0) || 0);
        setTotalHomologacao(data?.totalHomologacao ?? 0);
        if (data?.emitentes?.length) setEmitentes(data.emitentes);
        setPagina(0);
        setConsultaFeita(true);
      } catch (e) {
        setErro(e instanceof ApiError ? e.message : "Erro ao listar");
        setItens([]);
        setHasMore(false);
        setTotalElements(0);
        setValorTotalSomado(0);
        setTotalHomologacao(0);
        setConsultaFeita(true);
      } finally {
        setLoading(false);
      }
    },
    [buscarPagina, token, filtrosAplicados],
  );

  const carregarMais = useCallback(async () => {
    if (!consultaFeita || carregandoMais.current || !hasMore) return;
    carregandoMais.current = true;
    setLoadingMore(true);
    try {
      const proximaPagina = pagina + 1;
      const data = await buscarPagina(proximaPagina, filtrosAplicados);
      setItens((atual) => [...atual, ...(data?.itens ?? [])]);
      setHasMore(data?.hasMore ?? false);
      setPagina(proximaPagina);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar mais notas");
    } finally {
      carregandoMais.current = false;
      setLoadingMore(false);
    }
  }, [buscarPagina, pagina, filtrosAplicados, consultaFeita, hasMore]);

  useEffect(() => {
    if (!token) return;
    let cancelado = false;
    void (async () => {
      try {
        const data = await empresaPortalApi.listarEmpresas(token);
        if (cancelado) return;
        setEmitentes(
          (data.itens ?? []).map((e) => ({
            id: e.id,
            nome: e.nome,
            documento: e.cnpj,
          })),
        );
      } catch {
        /* select fica vazio até a 1ª consulta */
      }
    })();
    return () => {
      cancelado = true;
    };
  }, [token]);

  useEffect(() => {
    const alvo = fimLista.current;
    if (!alvo || !consultaFeita || !hasMore) return;
    const obs = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) void carregarMais();
      },
      { rootMargin: "240px" },
    );
    obs.observe(alvo);
    return () => obs.disconnect();
  }, [hasMore, carregarMais, itens.length, consultaFeita]);

  const aplicarFiltros = () => {
    setFiltrosAplicados({ ...filtros });
    void carregar(filtros);
  };

  const limparFiltros = () => {
    const vazio = { ...FILTROS_VAZIOS };
    setFiltros(vazio);
    setFiltrosAplicados(vazio);
    setItens([]);
    setHasMore(false);
    setTotalElements(0);
    setValorTotalSomado(0);
    setTotalHomologacao(0);
    setSelecionados(new Set());
    setConsultaFeita(false);
    setErro("");
    setPagina(0);
  };

  const abrirDanfe = async (chave: string, status?: string) => {
    if (!token) return;
    const st = (status ?? "").trim();
    if (st && st !== "100") {
      const ok = window.confirm(
        `O status desta nota não está como Autorizada (100).\nStatus atual: ${statusLabel(st)} (${st}).\n\n` +
          "O DANFE só é gerado se o XML estiver autorizado (cStat 100).\nDeseja tentar mesmo assim?",
      );
      if (!ok) return;
    }
    setErro("");
    try {
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas/${chave}/danfe`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(
          (body as { erro?: string; message?: string }).erro ??
            (body as { message?: string }).message ??
            "Falha ao abrir DANFE",
        );
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro DANFE");
    }
  };

  const baixarXml = async (chave: string) => {
    if (!token) return;
    try {
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas/${chave}/xml`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Falha ao baixar XML");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${chave.replace("NFe", "")}-proc.xml`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao baixar XML");
    }
  };

  const exportarZip = async (chaves?: string[]) => {
    if (!token) return;
    const modo = chaves?.length ? "selecionados" : "filtrados";
    setExportando(modo);
    setErro("");
    try {
      const params = montarParams(0, filtrosAplicados);
      params.delete("page");
      params.delete("size");
      chaves?.forEach((c) => params.append("chaves", c));
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas/export.zip?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError(
          (body as { erro?: string; message?: string }).erro ??
            (body as { message?: string }).message ??
            "Falha ao exportar ZIP",
          res.status,
        );
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "nfe-xmls.zip";
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao exportar ZIP");
    } finally {
      setExportando(null);
    }
  };

  const todosSelecionaveis = useMemo(
    () => itens.filter((n) => n.temXml !== false).map((n) => n.chave),
    [itens],
  );

  const toggleTodos = () => {
    if (selecionados.size === todosSelecionaveis.length) {
      setSelecionados(new Set());
    } else {
      setSelecionados(new Set(todosSelecionaveis));
    }
  };

  const toggleUm = (chave: string) => {
    setSelecionados((atual) => {
      const next = new Set(atual);
      if (next.has(chave)) next.delete(chave);
      else next.add(chave);
      return next;
    });
  };

  return (
    <div className="space-y-4">
      <EmitenteEmissaoBar dica="A lista só carrega depois de Filtrar. Role a página para lazy load das próximas notas." />

      <div className="fiscal-card">
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="m-0 text-2xl font-semibold text-slate-800">NF-e — XMLs e DANFE</h1>
            <p className="mt-1 text-sm text-slate-500">
              Defina os filtros e clique em Filtrar. Nada é carregado ao abrir a tela.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="fiscal-btn-secondary inline-flex items-center gap-2"
              disabled={!!exportando || selecionados.size === 0}
              onClick={() => void exportarZip([...selecionados])}
            >
              {exportando === "selecionados" ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <FileArchive className="h-4 w-4" />
              )}
              Exportar selecionados
            </button>
            <button
              type="button"
              className="fiscal-btn-secondary inline-flex items-center gap-2"
              disabled={!!exportando || !consultaFeita}
              onClick={() => void exportarZip()}
            >
              {exportando === "filtrados" ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Download className="h-4 w-4" />
              )}
              Exportar filtrados (ZIP)
            </button>
            <button
              type="button"
              className="fiscal-btn-primary inline-flex items-center gap-2"
              disabled={!consultaFeita || loading}
              onClick={() => void carregar()}
            >
              <RefreshCw className="h-4 w-4" /> Atualizar
            </button>
          </div>
        </div>

        <div className="mb-4 rounded-xl border border-slate-200 bg-slate-50/80 p-4">
          <p className="mb-3 text-sm font-medium text-slate-700">Filtros</p>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <label className="block text-sm sm:col-span-2">
              <span className="mb-1 block text-slate-500">Emitente</span>
              <select
                className="fiscal-input w-full"
                value={filtros.emitente}
                onChange={(e) => setFiltros((f) => ({ ...f, emitente: e.target.value }))}
              >
                <option value="">Todos os emitentes</option>
                {emitentes.map((em) => (
                  <option key={em.id} value={String(em.id)}>
                    {em.nome}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-slate-500">Data de</span>
              <input
                type="date"
                className="fiscal-input w-full"
                value={filtros.de}
                onChange={(e) => setFiltros((f) => ({ ...f, de: e.target.value }))}
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-slate-500">Data até</span>
              <input
                type="date"
                className="fiscal-input w-full"
                value={filtros.ate}
                onChange={(e) => setFiltros((f) => ({ ...f, ate: e.target.value }))}
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-slate-500">Série</span>
              <input
                type="text"
                className="fiscal-input w-full"
                placeholder="Ex: 1"
                value={filtros.serie}
                onChange={(e) => setFiltros((f) => ({ ...f, serie: e.target.value }))}
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-slate-500">Ambiente</span>
              <select
                className="fiscal-input w-full"
                value={filtros.ambiente}
                onChange={(e) => setFiltros((f) => ({ ...f, ambiente: e.target.value }))}
              >
                <option value="todos">Todos</option>
                <option value="1">Produção</option>
                <option value="2">Homologação</option>
              </select>
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-slate-500">Status</span>
              <select
                className="fiscal-input w-full"
                value={filtros.status}
                onChange={(e) => setFiltros((f) => ({ ...f, status: e.target.value }))}
              >
                <option value="">Todos</option>
                <option value="100">Autorizada (100)</option>
                <option value="101">Cancelada (101)</option>
                <option value="110">Denegada (110)</option>
              </select>
            </label>
            <label className="block text-sm sm:col-span-2 lg:col-span-1">
              <span className="mb-1 block text-slate-500">Buscar</span>
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="search"
                  className="fiscal-input w-full !pl-9"
                  placeholder="Chave, número, série ou cliente…"
                  value={filtros.q}
                  onChange={(e) => setFiltros((f) => ({ ...f, q: e.target.value }))}
                  onKeyDown={(e) => e.key === "Enter" && aplicarFiltros()}
                />
              </div>
            </label>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <button type="button" className="fiscal-btn-primary" onClick={aplicarFiltros}>
              Filtrar
            </button>
            <button type="button" className="fiscal-btn-secondary inline-flex items-center gap-1" onClick={limparFiltros}>
              <X className="h-4 w-4" /> Limpar
            </button>
          </div>
        </div>

        {erro && <p className="mb-3 text-sm text-red-600">{erro}</p>}

        {consultaFeita ? (
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[#3d6b2f]/30 bg-[#3d6b2f]/5 px-4 py-3">
            <div className="text-sm text-slate-600">
              {loading ? "Calculando…" : (
                <>
                  {totalElements} nota{totalElements !== 1 ? "s" : ""}
                  {filtrosAplicados.status === "100" ? " autorizada(s)" : " no filtro"}
                </>
              )}
            </div>
            <div className="text-right">
              <p className="text-xs uppercase tracking-wide text-slate-500">Somatório</p>
              <p className="text-lg font-semibold text-[#3d6b2f]">
                {loading ? "…" : formatarValor(valorTotalSomado)}
              </p>
            </div>
          </div>
        ) : null}

        <div className="overflow-x-auto">
          <table className="fiscal-table striped">
            <thead>
              <tr>
                <th style={{ width: "2.5rem" }}>
                  <input
                    type="checkbox"
                    checked={todosSelecionaveis.length > 0 && selecionados.size === todosSelecionaveis.length}
                    onChange={toggleTodos}
                    aria-label="Selecionar todos"
                  />
                </th>
                <th>Emitente</th>
                <th>Número</th>
                <th>Série</th>
                <th>Cliente</th>
                <th className="text-right">Valor</th>
                <th>CFOP</th>
                <th>Tipo</th>
                <th>Produto</th>
                <th>Status</th>
                <th>Data</th>
                <th style={{ width: "7rem" }} />
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={12} className="py-8 text-center text-slate-500">
                    <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                    Carregando…
                  </td>
                </tr>
              ) : !consultaFeita ? (
                <tr>
                  <td colSpan={12} className="py-8 text-center text-slate-500">
                    Escolha os filtros e clique em <strong>Filtrar</strong> para carregar a lista.
                  </td>
                </tr>
              ) : itens.length === 0 ? (
                <tr>
                  <td colSpan={12} className="py-8 text-center text-slate-500">
                    {filtrosAplicados.ambiente === "1" && totalHomologacao > 0
                      ? `Nenhuma NF-e de produção. Há ${totalHomologacao} de homologação — escolha Homologação no filtro Ambiente.`
                      : "Nenhuma NF-e encontrada para estes filtros"}
                  </td>
                </tr>
              ) : (
                itens.flatMap((n) => {
                  const produtos = produtosDaNota(n);
                  const span = produtos.length;
                  const cliente = n.cliente || n.destinatarioNome || "—";
                  const emitente = n.emitenteNome || "—";
                  return produtos.map((produto, idx) => (
                    <tr key={`${n.id}-${idx}`}>
                      {idx === 0 && (
                        <>
                          <td rowSpan={span}>
                            {n.temXml !== false && (
                              <input
                                type="checkbox"
                                checked={selecionados.has(n.chave)}
                                onChange={() => toggleUm(n.chave)}
                                aria-label={`Selecionar ${n.numero}`}
                              />
                            )}
                          </td>
                          <td
                            rowSpan={span}
                            className="max-w-[12rem] truncate"
                            title={[emitente, n.emitenteDocumento].filter(Boolean).join(" · ")}
                          >
                            {emitente}
                          </td>
                          <td rowSpan={span} className="whitespace-nowrap font-medium">
                            {n.numero}
                          </td>
                          <td rowSpan={span}>{n.serie ?? "—"}</td>
                          <td rowSpan={span} className="max-w-[14rem] truncate" title={cliente}>
                            {cliente}
                          </td>
                          <td rowSpan={span} className="whitespace-nowrap text-right font-medium">
                            {formatarValor(n.valorTotal)}
                          </td>
                          <td rowSpan={span} className="whitespace-nowrap">
                            {n.cfop || "—"}
                          </td>
                          <td rowSpan={span}>{n.tipoOperacao || "—"}</td>
                        </>
                      )}
                      <td className="max-w-[16rem] truncate" title={produto}>
                        {produto}
                      </td>
                      {idx === 0 && (
                        <>
                          <td rowSpan={span} title={n.motivoProtocolo}>
                            <span
                              className={
                                n.statusProtocolo === "100"
                                  ? "text-emerald-700"
                                  : n.statusProtocolo === "101"
                                    ? "text-red-600"
                                    : ""
                              }
                            >
                              {statusLabel(n.statusProtocolo)}
                            </span>
                            {n.ambiente === "2" && (
                              <span className="ml-2 text-xs text-amber-700">Homologação</span>
                            )}
                          </td>
                          <td rowSpan={span} className="whitespace-nowrap text-sm text-slate-600">
                            {formatarData(n.createdAt)}
                          </td>
                          <td rowSpan={span}>
                            <div className="flex flex-wrap gap-1">
                              {n.temXml !== false && (
                                <button
                                  type="button"
                                  className="fiscal-btn-icon"
                                  title="Baixar XML"
                                  onClick={() => void baixarXml(n.chave)}
                                >
                                  <Download className="h-4 w-4" />
                                </button>
                              )}
                              <button
                                type="button"
                                className="fiscal-btn-icon"
                                title={
                                  n.statusProtocolo === "100" || !n.statusProtocolo
                                    ? "Imprimir DANFE"
                                    : `Status ${statusLabel(n.statusProtocolo)} — DANFE exige autorização (100)`
                                }
                                onClick={() => void abrirDanfe(n.chave, n.statusProtocolo)}
                              >
                                <Printer className="h-4 w-4" />
                              </button>
                              {n.temXml !== false && (
                                <button
                                  type="button"
                                  className="fiscal-btn-icon"
                                  title="Clonar nota para uma nova emissão"
                                  onClick={() =>
                                    router.push(`/nfe/emissao?clonar=${encodeURIComponent(n.chave)}`)
                                  }
                                >
                                  <Copy className="h-4 w-4" />
                                </button>
                              )}
                            </div>
                          </td>
                        </>
                      )}
                    </tr>
                  ));
                })
              )}
            </tbody>
          </table>
        </div>

        {consultaFeita ? (
          <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm text-slate-500">
            <span>
              {totalElements} nota{totalElements !== 1 ? "s" : ""} encontrada{totalElements !== 1 ? "s" : ""}
              {selecionados.size > 0 ? ` · ${selecionados.size} selecionada(s)` : ""}
              {" · "}
              <strong className="text-slate-700">
                Soma{filtrosAplicados.status === "100" ? " (autorizadas)" : ""}: {formatarValor(valorTotalSomado)}
              </strong>
            </span>
            <span>
              {loadingMore
                ? "Carregando mais…"
                : hasMore
                  ? "Role para carregar mais (lazy load)"
                  : itens.length > 0
                    ? "Fim da lista"
                    : ""}
            </span>
          </div>
        ) : null}
        <div ref={fimLista} className="h-8" />
      </div>
    </div>
  );
}
