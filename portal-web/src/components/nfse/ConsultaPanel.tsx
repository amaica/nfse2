"use client";

import { useEffect, useState } from "react";
import { api, type LogItem } from "@/lib/api";
import { Card, GhostButton, Input, Label, PrimaryButton } from "./ui";
import { Download, Printer, Search } from "lucide-react";

function extrairChave(descricao: string): string | null {
  const m = descricao.match(/NFSe\s+(\d{50})/);
  return m ? m[1] : null;
}

export function ConsultaPanel({ token }: { token: string }) {
  const [chave, setChave] = useState("");
  const [logs, setLogs] = useState<LogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [consultando, setConsultando] = useState(false);
  const [detalhe, setDetalhe] = useState<unknown>(null);
  const chaveLimpa = chave.replace(/\D/g, "");

  useEffect(() => {
    api
      .historico(token)
      .then(setLogs)
      .catch(() => setLogs([]))
      .finally(() => setLoading(false));
  }, [token]);

  const notas = logs
    .filter((l) => l.acao === "EMISSAO")
    .map((l) => ({
      id: l.id,
      chave: extrairChave(l.descricao ?? ""),
      data: l.createdAt,
      status: "Emitida",
    }))
    .filter((n) => n.chave);

  async function consultarChave(k: string) {
    setConsultando(true);
    try {
      setDetalhe(await api.consulta(token, k));
    } catch {
      setDetalhe(null);
    } finally {
      setConsultando(false);
    }
  }

  return (
    <div className="w-full animate-in">
      <Card className="mb-4 w-full">
        <Label hint="50 dígitos">Chave de acesso</Label>
        <div className="mt-2 flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
            <Input
              className="pl-10 font-mono text-sm"
              placeholder="Cole a chave aqui"
              value={chave}
              onChange={(e) => setChave(e.target.value)}
            />
          </div>
          <PrimaryButton
            disabled={chaveLimpa.length !== 50 || consultando}
            onClick={() => consultarChave(chaveLimpa)}
          >
            Buscar
          </PrimaryButton>
        </div>
        {chaveLimpa.length === 50 && (
          <div className="mt-3 flex flex-wrap gap-2">
            <PrimaryButton onClick={() => api.imprimirPdf(token, chaveLimpa)}>
              <Printer className="mr-1.5 h-4 w-4" /> Imprimir DANFSe
            </PrimaryButton>
            <GhostButton onClick={() => api.downloadPdf(token, chaveLimpa)}>
              <Download className="mr-1.5 h-4 w-4" /> Baixar PDF
            </GhostButton>
            <GhostButton onClick={() => api.downloadXml(token, chaveLimpa)}>XML</GhostButton>
          </div>
        )}
      </Card>

      <Card className="w-full overflow-hidden p-0">
        <div className="border-b border-[var(--border)] px-6 py-4">
          <h2 className="text-sm font-semibold text-slate-900">Notas recentes</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-[var(--border)] bg-slate-50/80 text-xs font-medium uppercase tracking-wide text-[var(--muted)]">
                <th className="px-6 py-3">Número</th>
                <th className="px-6 py-3">Cliente</th>
                <th className="px-6 py-3">Data</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-[var(--muted)]">
                    Carregando...
                  </td>
                </tr>
              )}
              {!loading && notas.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-[var(--muted)]">
                    Nenhuma nota emitida nesta sessão ainda
                  </td>
                </tr>
              )}
              {notas.map((n) => (
                <tr key={n.id} className="border-b border-slate-100 transition hover:bg-slate-50/50">
                  <td className="px-6 py-4 font-mono text-xs text-slate-600">
                    …{n.chave?.slice(-8)}
                  </td>
                  <td className="px-6 py-4 text-slate-800">—</td>
                  <td className="px-6 py-4 text-[var(--muted)]">
                    {new Date(n.data).toLocaleString("pt-BR")}
                  </td>
                  <td className="px-6 py-4">
                    <span className="inline-flex rounded-full bg-[var(--brand-soft)] px-2.5 py-0.5 text-xs font-medium text-[var(--brand)]">
                      {n.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-1">
                      {n.chave && (
                        <>
                          <GhostButton
                            className="text-xs"
                            onClick={() => api.imprimirPdf(token, n.chave!)}
                            title="Imprimir DANFSe (PDF)"
                          >
                            <Printer className="h-3.5 w-3.5" />
                          </GhostButton>
                          <GhostButton
                            className="text-xs"
                            onClick={() => api.downloadPdf(token, n.chave!)}
                            title="Baixar PDF"
                          >
                            <Download className="h-3.5 w-3.5" />
                          </GhostButton>
                        </>
                      )}
                      <GhostButton
                        className="text-xs"
                        onClick={() => n.chave && consultarChave(n.chave)}
                      >
                        Ver
                      </GhostButton>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {detalhe != null && (
        <Card className="mt-6 w-full">
          <pre className="max-h-64 overflow-auto text-xs text-slate-600">
            {JSON.stringify(detalhe, null, 2)}
          </pre>
        </Card>
      )}
    </div>
  );
}
