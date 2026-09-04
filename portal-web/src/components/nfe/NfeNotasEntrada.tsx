"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Download,
  FileArchive,
  Loader2,
  RefreshCw,
  Search,
  X,
} from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { PORTAL_EMPRESA_EVENT } from "@/lib/portal-empresa";

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
};

type Filtros = { de: string; ate: string; q: string };

const PAGE_SIZE = 50;
const FILTROS_VAZIOS: Filtros = { de: "", ate: "", q: "" };

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function inicioAnoIso(): string {
  return `${new Date().getFullYear()}-01-01`;
}

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
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [itens, setItens] = useState<NotaEntrada[]>([]);
  const [filtros, setFiltros] = useState<Filtros>({ ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() });
  const [filtrosAplicados, setFiltrosAplicados] = useState<Filtros>(filtros);
  const [pagina, setPagina] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [selecionados, setSelecionados] = useState<Set<number>>(new Set());
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const [loading, setLoading] = useState(true);
  const [baixando, setBaixando] = useState(false);
  const [sincronizando, setSincronizando] = useState(false);
  const [baixarXmlAtivo, setBaixarXmlAtivo] = useState(false);
  const [ultimoNsu, setUltimoNsu] = useState<string | null>(null);

  const query = useMemo(() => {
    const p = new URLSearchParams();
    p.set("page", String(pagina));
    p.set("size", String(PAGE_SIZE));
    if (filtrosAplicados.de) p.set("de", filtrosAplicados.de);
    if (filtrosAplicados.ate) p.set("ate", filtrosAplicados.ate);
    if (filtrosAplicados.q.trim()) p.set("q", filtrosAplicados.q.trim());
    return p.toString();
  }, [filtrosAplicados, pagina]);

  const carregar = useCallback(async () => {
    if (!token || !empresaId) return;
    setLoading(true);
    setErro("");
    try {
      const res = await fetch(`${apiBaseUrl()}/api/nfe/entradas?${query}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new ApiError((body as { erro?: string }).erro ?? res.statusText, res.status);
      }
      const data = body as {
        itens?: NotaEntrada[];
        totalElements?: number;
        hasMore?: boolean;
        baixarXml?: boolean;
        ultimoNsu?: string | null;
      };
      setItens(Array.isArray(data.itens) ? data.itens : []);
      setTotalElements(data.totalElements ?? 0);
      setHasMore(!!data.hasMore);
      setBaixarXmlAtivo(!!data.baixarXml);
      setUltimoNsu(data.ultimoNsu ?? null);
      setSelecionados(new Set());
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar notas de entrada");
      setItens([]);
    } finally {
      setLoading(false);
    }
  }, [token, empresaId, query]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  useEffect(() => {
    const onEmp = () => {
      setPagina(0);
      void carregar();
    };
    window.addEventListener(PORTAL_EMPRESA_EVENT, onEmp);
    return () => window.removeEventListener(PORTAL_EMPRESA_EVENT, onEmp);
  }, [carregar]);

  const aplicarFiltros = () => {
    setFiltrosAplicados({ ...filtros });
    setPagina(0);
  };

  const limparFiltros = () => {
    const next = { ...FILTROS_VAZIOS, de: inicioAnoIso(), ate: hojeIso() };
    setFiltros(next);
    setFiltrosAplicados(next);
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
      const res = await fetch(`${apiBaseUrl()}/api/nfe/distribuicao/baixar`, {
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
      await carregar();
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
      a.download = `nfe-entrada-${empresaCnpj || empresaId}.zip`;
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
      a.download = `${chave || `nfe-entrada-${id}`}.xml`;
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
        <h1 className="page-header__title">Notas disponíveis para entrada</h1>
        <p className="page-header__subtitle">
          XMLs das NF-e emitidas contra o CNPJ do emitente (distribuição DF-e / SEFAZ)
          {empresaNome ? (
            <>
              {" "}
              · <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </>
          ) : null}
        </p>
      </header>

      <EmitenteEmissaoBar dica="O download automático exige «Baixar notas DF-e» no cadastro do emitente." />

      <div className="fiscal-card space-y-4 p-4">
        <div className="flex flex-wrap items-end gap-3">
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
            disabled={sincronizando || !baixarXmlAtivo}
            title={
              baixarXmlAtivo
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
            disabled={baixando || (itens.length === 0 && selecionados.size === 0)}
          >
            {baixando ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileArchive className="h-4 w-4" />}
            Exportar ZIP{selecionados.size > 0 ? ` (${selecionados.size})` : ""}
          </button>
          <span className="text-xs text-agro-muted">
            {baixarXmlAtivo ? (
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
                <td colSpan={9} className="py-8 text-center text-agro-muted">
                  <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                  Carregando…
                </td>
              </tr>
            ) : itens.length === 0 ? (
              <tr>
                <td colSpan={9} className="py-8 text-center text-agro-muted">
                  Nenhuma NF-e encontrada para os filtros. Use «Atualizar da SEFAZ» se o DF-e estiver ativo.
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

      <div className="fiscal-pagination">
        <span>
          {totalElements === 0
            ? "0 registros"
            : `${pagina * PAGE_SIZE + 1}–${Math.min((pagina + 1) * PAGE_SIZE, totalElements)} de ${totalElements}`}
        </span>
        <div className="flex gap-2">
          <button
            type="button"
            className="rounded border border-slate-300 px-2 py-1 disabled:opacity-40"
            disabled={pagina === 0}
            onClick={() => setPagina((p) => p - 1)}
          >
            Anterior
          </button>
          <button
            type="button"
            className="rounded border border-slate-300 px-2 py-1 disabled:opacity-40"
            disabled={!hasMore}
            onClick={() => setPagina((p) => p + 1)}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  );
}
