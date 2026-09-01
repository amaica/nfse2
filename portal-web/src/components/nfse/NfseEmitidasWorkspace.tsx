"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { FileText, Loader2, Plus, RefreshCw, Search } from "lucide-react";
import { api } from "@/lib/api";
import { fiscalApi } from "@/lib/fiscal-api";
import { getAppToken } from "@/lib/app-session";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { imprimirDanfseNfse } from "@/lib/imprimir-danfse-nfse";

type NfseEmitida = {
  id: number;
  chave: string;
  descricao?: string;
  createdAt?: string;
  status?: string;
};

function fmtData(iso?: string) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("pt-BR");
  } catch {
    return iso.slice(0, 19);
  }
}

export function NfseEmitidasWorkspace() {
  const token = getAppToken();
  const { empresaId, empresaNome } = useEmpresaScope();
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
      const data = await fiscalApi.request<{ itens: NfseEmitida[] }>(`/api/nfse/emitidas?${params}`);
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

  const total = useMemo(() => itens.length, [itens]);

  const onPdf = async (n: NfseEmitida) => {
    if (!token || !n.chave) return;
    setErro("");
    try {
      await api.imprimirPdf(token, n.chave);
    } catch {
      try {
        imprimirDanfseNfse({
          chave: n.chave,
          status: n.status || "Emitida",
          descricaoServico: n.descricao,
          criadoEm: n.createdAt,
          prestadorNome: empresaNome,
        });
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Não foi possível gerar o PDF.");
      }
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">NFS-e emitidas</h1>
          {empresaNome ? (
            <p className="mt-1 text-sm text-slate-500">
              Emitente: <span className="font-medium text-slate-700">{empresaNome}</span>
            </p>
          ) : null}
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => void carregar()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm hover:bg-slate-50"
          >
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
          <Link
            href="/nfse/emissao"
            className="inline-flex items-center gap-2 rounded-lg bg-[#16c15e] px-3 py-2 text-sm font-semibold text-white hover:bg-[#13aa52]"
          >
            <Plus className="h-4 w-4" /> Nova emissão
          </Link>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <span className="text-sm text-slate-500">Filtrar por chave ou descrição</span>
          <div className="relative w-full max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={filtro}
              onChange={(e) => setFiltro(e.target.value)}
              placeholder="Buscar…"
              className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-[#16c15e]"
            />
          </div>
        </div>

        {erro ? <p className="mb-3 text-sm text-red-600">{erro}</p> : null}

        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400">
                <th className="px-3 py-2">Data</th>
                <th className="px-3 py-2">Descrição</th>
                <th className="px-3 py-2">Status</th>
                <th className="px-3 py-2">Chave</th>
                <th className="px-3 py-2 text-right">PDF</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-3 py-10 text-center text-slate-400">
                    <Loader2 className="mr-2 inline h-4 w-4 animate-spin" /> Carregando…
                  </td>
                </tr>
              ) : itens.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-3 py-10 text-center text-slate-400">
                    Nenhuma NFS-e emitida para este emitente
                  </td>
                </tr>
              ) : (
                itens.map((n) => (
                  <tr key={n.id} className="border-b border-slate-100">
                    <td className="whitespace-nowrap px-3 py-2.5 text-slate-600">{fmtData(n.createdAt)}</td>
                    <td className="max-w-xs truncate px-3 py-2.5 font-medium text-slate-800">{n.descricao || "—"}</td>
                    <td className="px-3 py-2.5">
                      <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                        {n.status || "Emitida"}
                      </span>
                    </td>
                    <td className="max-w-[180px] truncate px-3 py-2.5 font-mono text-[11px] text-slate-500">{n.chave}</td>
                    <td className="px-3 py-2.5 text-right">
                      <button
                        type="button"
                        title="Gerar PDF"
                        onClick={() => void onPdf(n)}
                        className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1.5 text-xs font-semibold text-emerald-800 hover:bg-emerald-100"
                      >
                        <FileText className="h-3.5 w-3.5" /> PDF
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <p className="mt-3 text-sm text-slate-500">
          {total} nota{total !== 1 ? "s" : ""} encontrada{total !== 1 ? "s" : ""}
        </p>
      </div>
    </div>
  );
}
