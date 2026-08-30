"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, RefreshCw, FileText, Download } from "lucide-react";
import { fiscalApi } from "@/lib/fiscal-api";
import { getAppToken } from "@/lib/app-session";
import { apiBaseUrl } from "@/lib/api-base";
import { PageCard, PageHeader } from "@/components/ui/PageCard";

type Op = {
  id: number;
  nome: string;
  tomadorCnpj: string;
  tomadorRazao: string;
  valorServicos: number;
  ultimaEmissaoChave?: string | null;
  ultimaEmissaoEm?: string | null;
};

function money(v: number) {
  return v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export default function OperacoesMensaisPage() {
  const [ops, setOps] = useState<Op[]>([]);
  const [loading, setLoading] = useState(true);
  const [emitindo, setEmitindo] = useState<number | null>(null);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro("");
    try {
      const data = await fiscalApi.request<Op[]>("/api/nfse/operacoes-mensais");
      setOps(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao carregar operações");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function emitir(id: number) {
    setEmitindo(id);
    setErro("");
    setOk("");
    try {
      const hoje = new Date().toISOString().slice(0, 10);
      const res = await fiscalApi.request<Op & { chaveAcesso?: string }>(
        `/api/nfse/operacoes-mensais/${id}/emitir`,
        { method: "POST", body: JSON.stringify({ competencia: hoje }) },
      );
      setOk(`Emitida: ${res.nome} — chave ${res.chaveAcesso ?? res.ultimaEmissaoChave}`);
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha na emissão");
    } finally {
      setEmitindo(null);
    }
  }

  async function baixarPdf(chave: string, nome: string) {
    setErro("");
    try {
      const token = getAppToken();
      if (!token) throw new Error("Sessão expirada");
      const res = await fetch(`${apiBaseUrl()}/api/nfse/pdf/${chave}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("PDF ainda indisponível na ADN (tente em alguns minutos)");
      const blob = await res.blob();
      if (blob.size < 500) throw new Error("PDF ainda indisponível na ADN (tente em alguns minutos)");
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `DANFSe-${nome}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "PDF ainda indisponível na ADN (tente em alguns minutos)");
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="NFS-e mensais"
        subtitle="Operações salvas para clonar todo mês (Synki / MAICA)."
      />
      {erro && <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-900">{erro}</div>}
      {ok && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">{ok}</div>}
      <PageCard title="Operações cadastradas" subtitle="Clique em Emitir mês para clonar a competência atual.">
        <div className="mb-4 flex justify-end">
          <button type="button" onClick={() => void carregar()} className="fiscal-btn-secondary inline-flex items-center gap-2 text-sm">
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
        </div>
        {loading ? (
          <div className="flex items-center gap-2 py-10 text-gray-500">
            <Loader2 className="h-5 w-5 animate-spin" /> Carregando…
          </div>
        ) : ops.length === 0 ? (
          <p className="py-8 text-center text-gray-500">Nenhuma operação mensal cadastrada.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b text-gray-500">
                  <th className="py-2 pr-3">Operação</th>
                  <th className="py-2 pr-3">Tomador</th>
                  <th className="py-2 pr-3">Valor</th>
                  <th className="py-2 pr-3">Última emissão</th>
                  <th className="py-2">Ações</th>
                </tr>
              </thead>
              <tbody>
                {ops.map((op) => (
                  <tr key={op.id} className="border-b border-gray-100">
                    <td className="py-3 pr-3 font-medium">{op.nome}</td>
                    <td className="py-3 pr-3">
                      <div>{op.tomadorRazao}</div>
                      <div className="text-xs text-gray-500">{op.tomadorCnpj}</div>
                    </td>
                    <td className="py-3 pr-3">{money(Number(op.valorServicos))}</td>
                    <td className="py-3 pr-3 text-xs text-gray-600">
                      {op.ultimaEmissaoChave ? (
                        <>
                          <div className="font-mono">{op.ultimaEmissaoChave}</div>
                          <div>{op.ultimaEmissaoEm ? new Date(op.ultimaEmissaoEm).toLocaleString("pt-BR") : ""}</div>
                        </>
                      ) : (
                        "—"
                      )}
                    </td>
                    <td className="py-3">
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          disabled={emitindo === op.id}
                          onClick={() => void emitir(op.id)}
                          className="fiscal-btn-primary inline-flex items-center gap-1.5 text-xs"
                        >
                          {emitindo === op.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <FileText className="h-3.5 w-3.5" />}
                          Emitir mês
                        </button>
                        {op.ultimaEmissaoChave && (
                          <button
                            type="button"
                            onClick={() => void baixarPdf(op.ultimaEmissaoChave!, op.nome.replace(/\s+/g, "_"))}
                            className="fiscal-btn-secondary inline-flex items-center gap-1.5 text-xs"
                          >
                            <Download className="h-3.5 w-3.5" /> DANFSe
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </PageCard>
    </div>
  );
}
