"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Loader2, Printer, Search, X } from "lucide-react";
import { ApiError } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";

type EmitenteOpt = { id: number; nome: string };

type Evento = {
  id: number;
  emitenteNome?: string;
  tipo?: string;
  tipoLabel?: string;
  chave?: string;
  serie?: string;
  numeroInicial?: number;
  numeroFinal?: number;
  sequencial?: number;
  protocolo?: string;
  status?: string;
  motivo?: string;
  texto?: string;
  createdAt?: string;
};

type Filtros = { de: string; ate: string; q: string; emitente: string; tipo: string };

const PAGE_SIZE = 20;
const FILTROS_VAZIOS: Filtros = { de: "", ate: "", q: "", emitente: "", tipo: "" };

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function inicioAnoIso(): string {
  return `${new Date().getFullYear()}-01-01`;
}

function formatarData(iso?: string): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso.slice(0, 16);
  return d.toLocaleString("pt-BR");
}

function escaparHtml(valor: string): string {
  return valor
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function statusEvento(codigo?: string): string {
  switch ((codigo ?? "").trim()) {
    case "101":
    case "135":
    case "151":
    case "155":
      return "Homologado";
    case "136":
      return "Registrado";
    case "102":
      return "Inutilização homologada";
    default:
      return codigo || "—";
  }
}

function imprimirEvento(ev: Evento) {
  const linha = (rotulo: string, valor?: string | number | null) =>
    `<tr><th>${rotulo}</th><td>${escaparHtml(valor == null || valor === "" ? "—" : String(valor))}</td></tr>`;
  const titulo = ev.tipoLabel || ev.tipo || "Evento";
  const html = `<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="utf-8"><title>${escaparHtml(titulo)}</title>
<style>
  body { font-family: Georgia, serif; color: #1c1917; margin: 2rem; }
  h1 { font-size: 1.35rem; margin: 0 0 0.25rem; }
  p { margin: 0 0 1.25rem; color: #57534e; font-family: sans-serif; font-size: 0.85rem; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: left; vertical-align: top; padding: 0.45rem 0.6rem; border-bottom: 1px solid #e7e5e4; }
  th { width: 11rem; font-family: sans-serif; font-size: 0.75rem; color: #78716c; font-weight: 600; }
  td { font-size: 0.95rem; word-break: break-word; }
  @media print { body { margin: 1rem; } }
</style></head><body>
<h1>${escaparHtml(titulo)}</h1>
<p>Comprovante do evento registrado na SEFAZ</p>
<table>
${linha("Data", formatarData(ev.createdAt))}
${linha("Emitente", ev.emitenteNome)}
${linha("Situação", statusEvento(ev.status))}
${linha("Série", ev.serie)}
${linha("Número", formatarNumero(ev))}
${ev.sequencial != null ? linha("Sequência", ev.sequencial) : ""}
${linha("Protocolo", ev.protocolo)}
${linha("Chave", ev.chave)}
${linha("Descrição", ev.texto || ev.motivo)}
</table>
<script>window.addEventListener("load", function () { window.print(); });<\/script>
</body></html>`;
  const url = URL.createObjectURL(new Blob([html], { type: "text/html" }));
  const janela = window.open(url, "_blank");
  if (!janela) {
    URL.revokeObjectURL(url);
    return;
  }
  janela.addEventListener("afterprint", () => URL.revokeObjectURL(url));
}

function formatarNumero(ev: Evento): string {
  if (ev.numeroInicial == null) return "—";
  if (ev.numeroFinal != null && ev.numeroFinal !== ev.numeroInicial) {
    return `${ev.numeroInicial}–${ev.numeroFinal}`;
  }
  return String(ev.numeroInicial);
}

export function NfeEventosFiscais() {
  const token = getAppToken();
  const fimLista = useRef<HTMLDivElement | null>(null);
  const carregandoMais = useRef(false);
  const [emitentes, setEmitentes] = useState<EmitenteOpt[]>([]);
  const [filtros, setFiltros] = useState<Filtros>({ ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() });
  const [filtrosAplicados, setFiltrosAplicados] = useState<Filtros>(filtros);
  const [itens, setItens] = useState<Evento[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [erro, setErro] = useState("");

  const montarParams = useCallback((page: number, f: Filtros) => {
    const p = new URLSearchParams();
    p.set("page", String(page));
    p.set("size", String(PAGE_SIZE));
    if (f.de) p.set("de", f.de);
    if (f.ate) p.set("ate", f.ate);
    if (f.q.trim()) p.set("q", f.q.trim());
    if (f.emitente) p.set("emitente", f.emitente);
    if (f.tipo) p.set("tipo", f.tipo);
    return p;
  }, []);

  const buscarPagina = useCallback(
    async (page: number, f: Filtros) => {
      if (!token) return null;
      const res = await fetch(`${apiBaseUrl()}/api/nfe/eventos?${montarParams(page, f)}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
      }
      return body as {
        itens?: Evento[];
        totalElements?: number;
        hasMore?: boolean;
        emitentes?: EmitenteOpt[];
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
      try {
        const data = await buscarPagina(0, f);
        setItens(data?.itens ?? []);
        setTotalElements(data?.totalElements ?? 0);
        setHasMore(!!data?.hasMore);
        if (data?.emitentes) setEmitentes(data.emitentes);
        setPagina(0);
      } catch (e) {
        setErro(e instanceof ApiError ? e.message : "Erro ao listar eventos");
        setItens([]);
        setHasMore(false);
        setTotalElements(0);
      } finally {
        setLoading(false);
      }
    },
    [buscarPagina, token, filtrosAplicados],
  );

  const carregarMais = useCallback(async () => {
    if (carregandoMais.current) return;
    carregandoMais.current = true;
    setLoadingMore(true);
    try {
      const proxima = pagina + 1;
      const data = await buscarPagina(proxima, filtrosAplicados);
      setItens((atual) => [...atual, ...(data?.itens ?? [])]);
      setHasMore(!!data?.hasMore);
      setPagina(proxima);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar mais eventos");
    } finally {
      carregandoMais.current = false;
      setLoadingMore(false);
    }
  }, [buscarPagina, pagina, filtrosAplicados]);

  useEffect(() => {
    void carregar(filtrosAplicados);
  }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const alvo = fimLista.current;
    if (!alvo || !hasMore) return;
    const obs = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) void carregarMais();
      },
      { rootMargin: "240px" },
    );
    obs.observe(alvo);
    return () => obs.disconnect();
  }, [hasMore, carregarMais, itens.length]);

  const aplicarFiltros = () => {
    setFiltrosAplicados({ ...filtros });
    void carregar(filtros);
  };

  const limparFiltros = () => {
    const next = { ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() };
    setFiltros(next);
    setFiltrosAplicados(next);
    void carregar(next);
  };

  return (
    <div className="space-y-4">
      <EmitenteEmissaoBar dica="Pesquisa de cancelamentos, cartas de correção e inutilizações. O registro do evento fica em outra tela." />
      <div className="fiscal-card space-y-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold text-slate-800">Cancelamento/Correções/Inut. de NF-e</h1>
            <p className="mt-1 text-sm text-slate-500">
              Pesquisa de todos os emitentes e de todos os tipos de evento.
            </p>
          </div>
          <Link href="/nfe/registrar-evento" className="fiscal-btn-primary inline-flex items-center">
            Registrar evento
          </Link>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <label className="block text-sm sm:col-span-2">
            <span className="mb-1 block text-slate-500">Emitente</span>
            <select
              className="fiscal-input w-full"
              value={filtros.emitente}
              onChange={(e) => {
                const next = { ...filtros, emitente: e.target.value };
                setFiltros(next);
                setFiltrosAplicados(next);
                void carregar(next);
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
            <span className="mb-1 block text-slate-500">Evento</span>
            <select
              className="fiscal-input w-full"
              value={filtros.tipo}
              onChange={(e) => setFiltros((f) => ({ ...f, tipo: e.target.value }))}
            >
              <option value="">Todos</option>
              <option value="CANCELAMENTO">Cancelamento</option>
              <option value="CCE">Carta de correção</option>
              <option value="INUTILIZACAO">Inutilização</option>
            </select>
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-slate-500">Busca</span>
            <input
              className="fiscal-input w-full"
              placeholder="Chave, série, número…"
              value={filtros.q}
              onChange={(e) => setFiltros((f) => ({ ...f, q: e.target.value }))}
              onKeyDown={(e) => e.key === "Enter" && aplicarFiltros()}
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-slate-500">De</span>
            <input
              type="date"
              className="fiscal-input w-full"
              value={filtros.de}
              onChange={(e) => setFiltros((f) => ({ ...f, de: e.target.value }))}
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-slate-500">Até</span>
            <input
              type="date"
              className="fiscal-input w-full"
              value={filtros.ate}
              onChange={(e) => setFiltros((f) => ({ ...f, ate: e.target.value }))}
            />
          </label>
          <div className="flex items-end gap-2">
            <button type="button" className="fiscal-btn-primary inline-flex items-center gap-2" onClick={aplicarFiltros}>
              <Search className="h-4 w-4" /> Filtrar
            </button>
            <button type="button" className="btn-ghost inline-flex items-center gap-2 border border-slate-300" onClick={limparFiltros}>
              <X className="h-4 w-4" /> Limpar
            </button>
          </div>
        </div>
      </div>

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="fiscal-table min-w-full text-sm">
          <thead>
            <tr>
              <th>Data</th>
              <th>Evento</th>
              <th>Emitente</th>
              <th>Série</th>
              <th>Número</th>
              <th>Chave</th>
              <th>Status</th>
              <th>Detalhe</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={9} className="py-8 text-center text-slate-500">
                  <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                  Carregando…
                </td>
              </tr>
            ) : itens.length === 0 ? (
              <tr>
                <td colSpan={9} className="py-8 text-center text-slate-500">
                  Nenhum cancelamento, carta de correção ou inutilização nestes filtros.
                </td>
              </tr>
            ) : (
              itens.map((ev) => (
                <tr key={ev.id}>
                  <td className="whitespace-nowrap">{formatarData(ev.createdAt)}</td>
                  <td className="whitespace-nowrap font-medium">{ev.tipoLabel ?? ev.tipo}</td>
                  <td className="max-w-[12rem] truncate" title={ev.emitenteNome}>
                    {ev.emitenteNome ?? "—"}
                  </td>
                  <td>{ev.serie || "—"}</td>
                  <td className="whitespace-nowrap">{formatarNumero(ev)}</td>
                  <td className="max-w-[10rem] truncate font-mono text-xs" title={ev.chave}>
                    {ev.chave || "—"}
                  </td>
                  <td className="whitespace-nowrap">{ev.status || "—"}</td>
                  <td className="max-w-[16rem] truncate" title={ev.texto || ev.motivo}>
                    {ev.texto || ev.motivo || "—"}
                  </td>
                  <td className="whitespace-nowrap text-right">
                    <button
                      type="button"
                      className="inline-flex items-center gap-1 text-sm text-slate-700 underline"
                      onClick={() => imprimirEvento(ev)}
                    >
                      <Printer className="h-3.5 w-3.5" /> Imprimir
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <p className="text-xs text-slate-500">
        {totalElements === 0 ? "0 registros" : `${itens.length} de ${totalElements}`}
        {loadingMore ? " · carregando…" : hasMore ? " · role para carregar mais" : ""}
      </p>
      {erro && <p className="text-sm text-red-600">{erro}</p>}
      <div ref={fimLista} className="h-8" />
    </div>
  );
}
