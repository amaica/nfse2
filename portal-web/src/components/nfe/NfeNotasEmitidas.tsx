"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { getAppToken } from "@/lib/app-session";

type Nota = {
  id: number;
  chave: string;
  serie: string;
  numero: number;
  statusProtocolo?: string;
  motivoProtocolo?: string;
  createdAt?: string;
};

export function NfeNotasEmitidas() {
  const token = getAppToken();
  const [itens, setItens] = useState<Nota[]>([]);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);

  const carregar = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/nfe/notas`,
        { headers: { Authorization: `Bearer ${token}` } },
      );
      const data = (await res.json()) as { itens: Nota[] };
      setItens(data.itens ?? []);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao listar");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const abrirDanfe = async (chave: string) => {
    if (!token) return;
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/nfe/notas/${chave}/danfe`,
        { headers: { Authorization: `Bearer ${token}` } },
      );
      if (!res.ok) throw new Error("Falha ao abrir DANFE");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro DANFE");
    }
  };

  return (
    <div className="fiscal-card">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-800">NF-e — Notas emitidas</h1>
        <button type="button" className="fiscal-btn-primary" onClick={carregar}>
          Atualizar
        </button>
      </div>
      {erro && <p className="mb-3 text-sm text-red-600">{erro}</p>}
      <table className="fiscal-table">
        <thead>
          <tr>
            <th>Número</th>
            <th>Série</th>
            <th>Chave</th>
            <th>Status</th>
            <th>Data</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={6} className="py-8 text-center text-slate-500">
                Carregando…
              </td>
            </tr>
          ) : itens.length === 0 ? (
            <tr>
              <td colSpan={6} className="py-8 text-center text-slate-500">
                Nenhuma NF-e emitida
              </td>
            </tr>
          ) : (
            itens.map((n) => (
              <tr key={n.id}>
                <td>{n.numero}</td>
                <td>{n.serie}</td>
                <td className="font-mono text-xs">{n.chave}</td>
                <td>{n.statusProtocolo}</td>
                <td className="text-sm text-slate-500">{n.createdAt?.slice(0, 19)}</td>
                <td>
                  <button
                    type="button"
                    className="text-sm text-blue-600 hover:underline"
                    onClick={() => abrirDanfe(n.chave)}
                  >
                    DANFE
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
