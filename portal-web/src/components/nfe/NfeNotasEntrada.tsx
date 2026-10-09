"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  Download,
  FileArchive,
  Loader2,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { ApiError, empresaPortalApi, formatarCnpjCpf } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";

type NotaEntrada = {
  id: number;
  chave: string;
  nsu?: string;
  numero?: string;
  serie?: string;
  nomeEmitente?: string;
  cnpjEmitente?: string;
  dataEmissao?: string;
  natureza?: string;
  valor?: number;
  temXml?: boolean;
  createdAt?: string;
  empresaId?: number;
  destinatarioNome?: string;
  destinatarioDocumento?: string;
};

type EmitenteOpt = { id: number; nome: string; documento?: string };

type Filtros = { de: string; ate: string; q: string; emitente: string };

const PAGE_SIZE = 20;
const FILTROS_VAZIOS: Filtros = { de: "", ate: "", q: "", emitente: "" };

function formatarData(iso?: string): string {
  if (!iso) return "—";
  const [y, m, d] = iso.slice(0, 10).split("-");
  if (!y || !m || !d) return iso;
  return `${d}/${m}/${y}`;
}

function formatarValor(v?: number): string {
  if (v == null || Number.isNaN(Number(v))) return "—";
  return Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export function NfeNotasEntrada() {
  const token = getAppToken();
  const fimLista = useRef<HTMLDivElement | null>(null);
  const carregandoMais = useRef(false);
  const [itens, setItens] = useState<NotaEntrada[]>([]);
  const [emitentes, setEmitentes] = useState<EmitenteOpt[]>([]);
  const [filtros, setFiltros] = useState<Filtros>({ ...FILTROS_VAZIOS });
  const [filtrosAplicados, setFiltrosAplicados] = useState<Filtros>({ ...FILTROS_VAZIOS });
  /** Só true depois de Filtrar — não carrega tudo ao abrir. */
  const [consultaFeita, setConsultaFeita] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [selecionados, setSelecionados] = useState<Set<number>>(new Set());
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [baixando, setBaixando] = useState(false);
  const [sincronizando, setSincronizando] = useState(false);
  const [baixarXmlAtivo, setBaixarXmlAtivo] = useState(false);
  const [ultimoNsu, setUltimoNsu] = useState<string | null>(null);

  const montarParams = useCallback((page: number, f: Filtros) => {
    const p = new URLSearchParams();
    p.set("page", String(page));
    p.set("size", String(PAGE_SIZE));
    if (f.de) p.set("de", f.de);
    if (f.ate) p.set("ate", f.ate);
    if (f.q.trim()) p.set("q", f.q.trim());
    if (f.emitente) p.set("emitente", f.emitente);
    return p;
  }, []);

  const buscarPagina = useCallback(
    async (page: number, f: Filtros) => {
      if (!token) return null;
      const res = await fetch(`${apiBaseUrl()}/api/nfe/entradas?${montarParams(page, f)}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
      }
      return body as {
        itens?: NotaEntrada[];
        totalElements?: number;
        hasMore?: boolean;
        baixarXml?: boolean;
        ultimoNsu?: string | null;
        emitentes?: EmitenteOpt[];
      };
    },
    [token, montarParams],
  );

  const carregarMetaEmitente = useCallback(
    async (emitenteId: string) => {
      if (!token || !emitenteId) {
        setBaixarXmlAtivo(false);
        setUltimoNsu(null);
        return;
      }
      try {
        const emp = await empresaPortalApi.obterEmpresa(token, Number(emitenteId));
        setBaixarXmlAtivo(!!emp.baixarXml);
        setUltimoNsu(emp.ultimoNsu ?? null);
      } catch {
        setBaixarXmlAtivo(false);
        setUltimoNsu(null);
      }
    },
    [token],
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
        setTotalElements(data?.totalElements ?? 0);
        setHasMore(!!data?.hasMore);
        if (f.emitente) {
          setBaixarXmlAtivo(!!data?.baixarXml);
          setUltimoNsu(data?.ultimoNsu ?? null);
        }
        if (data?.emitentes?.length) setEmitentes(data.emitentes);
        setPagina(0);
        setConsultaFeita(true);
      } catch (e) {
        setErro(e instanceof ApiError ? e.message : "Erro ao carregar NF-e recebidas");
        setItens([]);
        setHasMore(false);
        setTotalElements(0);
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
      const proxima = pagina + 1;
      const data = await buscarPagina(proxima, filtrosAplicados);
      setItens((atual) => [...atual, ...(data?.itens ?? [])]);
      setHasMore(!!data?.hasMore);
      setPagina(proxima);
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
        /* select vazio até a 1ª consulta */
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
    const next = { ...FILTROS_VAZIOS };
    setFiltros(next);
    setFiltrosAplicados(next);
    setItens([]);
    setHasMore(false);
    setTotalElements(0);
    setSelecionados(new Set());
    setConsultaFeita(false);
    setErro("");
    setOk("");
    setBaixarXmlAtivo(false);
    setUltimoNsu(null);
    setPagina(0);
  };

  const toggle = (id: number) => {
    setSelecionados((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleTodos = () => {
    if (selecionados.size === itens.length) setSelecionados(new Set());
    else setSelecionados(new Set(itens.map((n) => n.id)));
  };

  const sincronizarSefaz = async () => {
    if (!token) return;
    setSincronizando(true);
    setErro("");
    setOk("");
    try {
      const emitenteSync = filtros.emitente || filtrosAplicados.emitente;
      const qs = emitenteSync ? `?emitente=${encodeURIComponent(emitenteSync)}` : "";
      const res = await fetch(`${apiBaseUrl()}/api/nfe/distribuicao/baixar${qs}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
      }
      const novas = (body as { novas?: number }).novas ?? 0;
      setOk(
        novas > 0
          ? `${novas} nova(s) NF-e gravada(s) da SEFAZ (DF-e).`
          : "Nenhuma nota nova. NSU atualizado.",
      );
      if (consultaFeita) await carregar();
      else await carregarMetaEmitente(filtrosAplicados.emitente);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Falha ao sincronizar DF-e");
    } finally {
      setSincronizando(false);
    }
  };

  const exportarZip = async () => {
    if (!token) return;
    setBaixando(true);
    setErro("");
    try {
      const p = new URLSearchParams();
      if (selecionados.size > 0) {
        for (const id of selecionados) p.append("ids", String(id));
      } else {
        if (filtrosAplicados.de) p.set("de", filtrosAplicados.de);
        if (filtrosAplicados.ate) p.set("ate", filtrosAplicados.ate);
        if (filtrosAplicados.q.trim()) p.set("q", filtrosAplicados.q.trim());
        if (filtrosAplicados.emitente) p.set("emitente", filtrosAplicados.emitente);
      }
      const res = await fetch(`${apiBaseUrl()}/api/nfe/entradas/export.zip?${p}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "nfe-recebidas.zip";
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Falha ao exportar ZIP");
    } finally {
      setBaixando(false);
    }
  };

  const baixarXml = async (id: number, chave?: string) => {
    if (!token) return;
    try {
      const res = await fetch(`${apiBaseUrl()}/api/nfe/entradas/${id}/xml`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${chave || `nfe-recebida-${id}`}.xml`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Falha ao baixar XML");
    }
  };

  return (
    <div className="animate-in space-y-4">
      <header>
        <p className="page-header__eyebrow">NF-e</p>
        <h1 className="page-header__title">NF-e recebidas</h1>
        <p className="page-header__subtitle">
          Notas emitidas por terceiros contra o CNPJ/CPF (DF-e). A lista só carrega depois de
          Filtrar — nada é buscado ao abrir a tela.
        </p>
      </header>

      <div className="fiscal-card space-y-4 p-4">
        <div className="flex flex-wrap items-end gap-3">
          <label className="block min-w-[16rem] flex-1 text-sm">
            <span className="mb-1 block text-agro-muted">Emitente</span>
            <select
              className="fiscal-input w-full"
              value={filtros.emitente}
              onChange={(e) => {
                const id = e.target.value;
                setFiltros((f) => ({ ...f, emitente: id }));
                void carregarMetaEmitente(id);
              }}
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
            <span className="mb-1 block text-agro-muted">De</span>
            <input
              type="date"
              className="fiscal-input"
              value={filtros.de}
              onChange={(e) => setFiltros((f) => ({ ...f, de: e.target.value }))}
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-agro-muted">Até</span>
            <input
              type="date"
              className="fiscal-input"
              value={filtros.ate}
              onChange={(e) => setFiltros((f) => ({ ...f, ate: e.target.value }))}
            />
          </label>
          <label className="block min-w-[14rem] flex-1 text-sm">
            <span className="mb-1 block text-agro-muted">Busca</span>
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                className="fiscal-input pl-9"
                placeholder="Nº, chave, emitente, CNPJ…"
                value={filtros.q}
                onChange={(e) => setFiltros((f) => ({ ...f, q: e.target.value }))}
                onKeyDown={(e) => e.key === "Enter" && aplicarFiltros()}
              />
            </div>
          </label>
          <button type="button" className="fiscal-btn-primary inline-flex items-center gap-2" onClick={aplicarFiltros}>
            <Search className="h-4 w-4" /> Filtrar
          </button>
          <button
            type="button"
            className="btn-ghost inline-flex items-center gap-2 border border-[var(--border)]"
            onClick={limparFiltros}
          >
            <X className="h-4 w-4" /> Limpar
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            className="fiscal-btn-primary inline-flex items-center gap-2"
            onClick={() => void sincronizarSefaz()}
            disabled={sincronizando || !filtros.emitente || !baixarXmlAtivo}
            title={
              !filtros.emitente
                ? "Selecione um emitente no filtro para baixar da SEFAZ"
                : baixarXmlAtivo
                  ? "Consulta distribuição DF-e na SEFAZ"
                  : "Ative «Baixar notas DF-e» no cadastro do emitente"
            }
          >
            {sincronizando ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
            {sincronizando ? "Sincronizando…" : "Atualizar da SEFAZ"}
          </button>
          <button
            type="button"
            className="btn-ghost inline-flex items-center gap-2 border border-[var(--border)]"
            onClick={() => void exportarZip()}
            disabled={baixando || !consultaFeita || (itens.length === 0 && selecionados.size === 0)}
          >
            {baixando ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileArchive className="h-4 w-4" />}
            Exportar ZIP{selecionados.size > 0 ? ` (${selecionados.size})` : ""}
          </button>
          <span className="text-xs text-agro-muted">
            {!filtros.emitente ? (
              "Escolha um emitente no filtro para atualizar a SEFAZ"
            ) : baixarXmlAtivo ? (
              <>
                DF-e ativo{ultimoNsu ? ` · NSU ${ultimoNsu}` : ""} · cron horário baixa automaticamente
              </>
            ) : (
              "DF-e desligado neste emitente — ative em Cadastros → Emitentes"
            )}
          </span>
        </div>

        {erro ? (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</div>
        ) : null}
        {ok ? (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
            {ok}
          </div>
        ) : null}
      </div>

      <div className="overflow-x-auto rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)]">
        <table className="fiscal-table striped min-w-full">
          <thead>
            <tr>
              <th style={{ width: "2.5rem" }}>
                <input
                  type="checkbox"
                  checked={itens.length > 0 && selecionados.size === itens.length}
                  onChange={toggleTodos}
                  aria-label="Selecionar todos"
                />
              </th>
              <th>Destinatário</th>
              <th>Nº</th>
              <th>Série</th>
              <th>Emitente</th>
              <th>CNPJ</th>
              <th>Data</th>
              <th className="text-right">Valor</th>
              <th>Chave</th>
              <th style={{ width: "6rem" }} />
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={10} className="py-8 text-center text-agro-muted">
                  <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                  Carregando…
                </td>
              </tr>
            ) : !consultaFeita ? (
              <tr>
                <td colSpan={10} className="py-8 text-center text-agro-muted">
                  Escolha os filtros e clique em <strong>Filtrar</strong> para carregar a lista.
                </td>
              </tr>
            ) : itens.length === 0 ? (
              <tr>
                <td colSpan={10} className="py-8 text-center text-agro-muted">
                  Nenhuma NF-e encontrada para estes filtros.
                </td>
              </tr>
            ) : (
              itens.map((n) => (
                <tr key={n.id}>
                  <td>
                    <input
                      type="checkbox"
                      checked={selecionados.has(n.id)}
                      onChange={() => toggle(n.id)}
                      aria-label={`Selecionar ${n.numero}`}
                    />
                  </td>
                  <td className="max-w-[14rem] truncate" title={n.destinatarioNome}>
                    {n.destinatarioNome || "—"}
                  </td>
                  <td>{n.numero || "—"}</td>
                  <td>{n.serie || "—"}</td>
                  <td className="max-w-[16rem] truncate" title={n.nomeEmitente}>
                    {n.nomeEmitente || "—"}
                  </td>
                  <td className="whitespace-nowrap">{formatarCnpjCpf(n.cnpjEmitente ?? "")}</td>
                  <td>{formatarData(n.dataEmissao)}</td>
                  <td className="text-right">{formatarValor(n.valor)}</td>
                  <td className="max-w-[12rem] truncate font-mono text-xs" title={n.chave}>
                    {n.chave}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="fiscal-btn-icon"
                      title="Baixar XML"
                      disabled={!n.temXml}
                      onClick={() => void baixarXml(n.id, n.chave)}
                    >
                      <Download className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {consultaFeita ? (
        <div className="fiscal-pagination">
          <span>
            {totalElements === 0
              ? "0 registros"
              : `${itens.length} de ${totalElements}`}
            {loadingMore
              ? " · carregando…"
              : hasMore
                ? " · role para carregar mais (lazy load)"
                : itens.length > 0
                  ? " · fim da lista"
                  : ""}
          </span>
        </div>
      ) : null}
      <div ref={fimLista} className="h-8" />
    </div>
  );
}
