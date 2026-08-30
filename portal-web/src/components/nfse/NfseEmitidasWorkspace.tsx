"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Download, Loader2, Printer, RefreshCw, Search } from "lucide-react";
import { api } from "@/lib/api";
import { fiscalApi } from "@/lib/fiscal-api";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";

type NfseEmitida = {
  id: number;
  chave: string;
  descricao?: string;
  createdAt?: string;
  status?: string;
};

export function NfseEmitidasWorkspace() {
  const token = getAppToken();
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [itens, setItens] = useState<NfseEmitida[]>([]);
  const [filtro, setFiltro] = useState("");
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setErro("");
    try {
      const params = new URLSearchParams({ limite: "200" });
      if (filtro.trim()) params.set("q", filtro.trim());
      const data = await fiscalApi.request<{ itens: NfseEmitida[] }>(
        `/api/nfse/emitidas?${params}`,
      );
      setItens(data.itens ?? []);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar NFS-e emitidas");
      setItens([]);
    } finally {
      setLoading(false);
    }
  }, [token, filtro, empresaId]);

  useEffect(() => {
    const t = window.setTimeout(() => void carregar(), filtro ? 300 : 0);
    return () => window.clearTimeout(t);
  }, [carregar, filtro]);

  const formatarData = useCallback((iso?: string) => {
    if (!iso) return "—";
    try {
      return new Date(iso).toLocaleString("pt-BR");
    } catch {
      return iso.slice(0, 19);
    }
  }, []);

  const total = useMemo(() => itens.length, [itens]);

  return (
    <div className="fiscal-card">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="m-0 text-2xl font-semibold text-slate-800">NFS-e emitidas</h1>
          {empresaNome && (
            <p className="mt-1 text-sm text-slate-500">
              Emitente: <span className="font-medium text-slate-700">{empresaNome}</span>
              {empresaCnpj ? ` · ${empresaCnpj}` : ""}
            </p>
          )}
        </div>
        <button type="button" className="fiscal-btn-primary inline-flex items-center gap-2" onClick={() => void carregar()}>
          <RefreshCw className="h-4 w-4" /> Atualizar
        </button>
      </div>

      <div className="fiscal-table-caption mb-4">
        <span>Filtrar por chave ou descrição</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="search"
            placeholder="Chave ou texto..."
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
          />
        </div>
      </div>

      {erro && <p className="mb-3 text-sm text-red-600">{erro}</p>}

      <div className="overflow-x-auto">
        <table className="fiscal-table striped">
          <thead>
            <tr>
              <th>Data</th>
              <th>Chave de acesso</th>
              <th>Status</th>
              <th style={{ width: "10rem" }} />
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={4} className="py-8 text-center text-slate-500">
                  <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                  Carregando…
                </td>
              </tr>
            ) : itens.length === 0 ? (
              <tr>
                <td colSpan={4} className="py-8 text-center text-slate-500">
                  Nenhuma NFS-e emitida para este emitente
                </td>
              </tr>
            ) : (
              itens.map((n) => (
                <tr key={n.id}>
                  <td className="whitespace-nowrap text-sm text-slate-600">{formatarData(n.createdAt)}</td>
                  <td className="max-w-md font-mono text-xs">{n.chave || "—"}</td>
                  <td>{n.status ?? "Emitida"}</td>
                  <td>
                    {n.chave && token ? (
                      <div className="flex flex-wrap gap-1">
                        <button
                          type="button"
                          className="fiscal-btn-icon"
                          title="Imprimir PDF"
                          onClick={() => void api.imprimirPdf(token, n.chave)}
                        >
                          <Printer className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          className="fiscal-btn-icon"
                          title="Baixar PDF"
                          onClick={() => void api.downloadPdf(token, n.chave)}
                        >
                          <Download className="h-4 w-4" />
                        </button>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-sm text-slate-500">{total} nota{total !== 1 ? "s" : ""} encontrada{total !== 1 ? "s" : ""}</p>
    </div>
  );
}
