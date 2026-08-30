"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Download,
  FileArchive,
  Loader2,
  Printer,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { ApiError } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { PORTAL_EMPRESA_EVENT } from "@/lib/portal-empresa";

type Nota = {
  id: number;
  chave: string;
  serie: string;
  numero: number;
  statusProtocolo?: string;
  motivoProtocolo?: string;
  createdAt?: string;
  temXml?: boolean;
};

type Filtros = {
  de: string;
  ate: string;
  q: string;
  serie: string;
  status: string;
};

const PAGE_SIZE = 50;

const FILTROS_VAZIOS: Filtros = { de: "", ate: "", q: "", serie: "", status: "" };

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function inicioAnoIso(): string {
  return `${new Date().getFullYear()}-01-01`;
}

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

export function NfeNotasEmitidas() {
  const token = getAppToken();
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [itens, setItens] = useState<Nota[]>([]);
  const [filtros, setFiltros] = useState<Filtros>({ ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() });
  const [filtrosAplicados, setFiltrosAplicados] = useState<Filtros>(filtros);
  const [pagina, setPagina] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [selecionados, setSelecionados] = useState<Set<string>>(new Set());
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);
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
      return params;
    },
    [],
  );

  const buscarPagina = useCallback(
    async (page: number, f: Filtros) => {
      if (!token || !empresaId) return null;
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
      };
    },
    [token, empresaId, montarParams],
  );

  const carregar = useCallback(
    async (f: Filtros = filtrosAplicados) => {
      if (!token || !empresaId) {
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
        setPagina(0);
      } catch (e) {
        setErro(e instanceof ApiError ? e.message : "Erro ao listar");
        setItens([]);
        setHasMore(false);
        setTotalElements(0);
      } finally {
        setLoading(false);
      }
    },
    [buscarPagina, token, empresaId, filtrosAplicados],
  );

  const carregarMais = useCallback(async () => {
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
      setLoadingMore(false);
    }
  }, [buscarPagina, pagina, filtrosAplicados]);

  useEffect(() => {
    void carregar(filtrosAplicados);
  }, [empresaId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const onTroca = () => {
      setFiltrosAplicados({ ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() });
      setFiltros({ ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() });
      void carregar({ ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() });
    };
    window.addEventListener(PORTAL_EMPRESA_EVENT, onTroca);
    return () => window.removeEventListener(PORTAL_EMPRESA_EVENT, onTroca);
  }, [carregar]);

  const aplicarFiltros = () => {
    setFiltrosAplicados({ ...filtros });
    void carregar(filtros);
  };

  const limparFiltros = () => {
    const vazio = { ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() };
    setFiltros(vazio);
    setFiltrosAplicados(vazio);
    void carregar(vazio);
  };

  const abrirDanfe = async (chave: string) => {
    if (!token) return;
    try {
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas/${chave}/danfe`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Falha ao abrir DANFE");
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
      a.download = `nfe-xmls-${empresaCnpj?.replace(/\D/g, "") ?? "emitente"}.zip`;
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
      <EmitenteEmissaoBar dica="Lista, filtra e exporta XMLs das NF-e do emitente ativo. Imprima o DANFE de cada nota." />

      <div className="fiscal-card">
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="m-0 text-2xl font-semibold text-slate-800">NF-e — XMLs e DANFE</h1>
            {empresaNome && (
              <p className="mt-1 text-sm text-slate-500">
                Emitente: <span className="font-medium text-slate-700">{empresaNome}</span>
                {empresaCnpj ? ` · ${empresaCnpj}` : ""}
              </p>
            )}
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
              disabled={!!exportando}
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
              onClick={() => void carregar()}
            >
              <RefreshCw className="h-4 w-4" /> Atualizar
            </button>
          </div>
        </div>

        <div className="mb-4 rounded-xl border border-slate-200 bg-slate-50/80 p-4">
          <p className="mb-3 text-sm font-medium text-slate-700">Filtros</p>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
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
                  placeholder="Chave, número ou série…"
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
                <th>Número</th>
                <th>Série</th>
                <th>Chave</th>
                <th>Status</th>
                <th>Data</th>
                <th style={{ width: "7rem" }} />
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500">
                    <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                    Carregando…
                  </td>
                </tr>
              ) : itens.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500">
                    Nenhuma NF-e encontrada para este emitente e filtros
                  </td>
                </tr>
              ) : (
                itens.map((n) => (
                  <tr key={n.id}>
                    <td>
                      {n.temXml !== false && (
                        <input
                          type="checkbox"
                          checked={selecionados.has(n.chave)}
                          onChange={() => toggleUm(n.chave)}
                          aria-label={`Selecionar ${n.numero}`}
                        />
                      )}
                    </td>
                    <td className="whitespace-nowrap font-medium">{n.numero}</td>
                    <td>{n.serie ?? "—"}</td>
                    <td className="max-w-xs font-mono text-xs" title={n.chave}>
                      {n.chave}
                    </td>
                    <td title={n.motivoProtocolo}>
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
                    </td>
                    <td className="whitespace-nowrap text-sm text-slate-600">{formatarData(n.createdAt)}</td>
                    <td>
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
                          title="Imprimir DANFE"
                          onClick={() => void abrirDanfe(n.chave)}
                        >
                          <Printer className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm text-slate-500">
          <span>
            {totalElements} nota{totalElements !== 1 ? "s" : ""} encontrada{totalElements !== 1 ? "s" : ""}
            {selecionados.size > 0 ? ` · ${selecionados.size} selecionada(s)` : ""}
          </span>
          {hasMore && (
            <button
              type="button"
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50 disabled:opacity-50"
              disabled={loadingMore}
              onClick={() => void carregarMais()}
            >
              {loadingMore ? "Carregando…" : "Carregar mais"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
