"use client";

import { useCallback, useEffect, useState } from "react";
import { ClipboardList, Loader2 } from "lucide-react";
import { fiscalFetch } from "@/lib/fiscal-api";
import { getAppToken } from "@/lib/app-session";

type AuditItem = {
  id: number;
  empresaId?: number;
  usuarioId?: number;
  acao: string;
  recurso?: string;
  detalhe?: string;
  ip?: string;
  createdAt: string;
};

type AuditResponse = {
  pagina: number;
  limite: number;
  itens: AuditItem[];
};

export default function AuditoriaPage() {
  const [itens, setItens] = useState<AuditItem[]>([]);
  const [pagina, setPagina] = useState(0);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);

  const carregar = useCallback(async (p: number) => {
    const token = getAppToken();
    if (!token) return;
    setLoading(true);
    try {
      const res = await fiscalFetch<AuditResponse>(
        `/api/conta/auditoria?pagina=${p}&limite=40`,
        token,
      );
      setItens(res.itens);
      setPagina(res.pagina);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar auditoria");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar(0);
  }, [carregar]);

  return (
    <div className="animate-in mx-auto max-w-4xl space-y-6">
      <header>
        <p className="page-header__eyebrow">Conta</p>
        <h1 className="page-header__title">Auditoria</h1>
        <p className="page-header__subtitle">Registro de ações fiscais e administrativas</p>
      </header>

      <section className="fiscal-card overflow-hidden p-0">
        <div className="flex items-center gap-2 border-b border-[var(--border)] px-6 py-4">
          <ClipboardList className="h-4 w-4 text-[var(--brand)]" />
          <h2 className="font-semibold text-agro-body">Eventos recentes</h2>
        </div>

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-16 text-agro-muted">
            <Loader2 className="h-5 w-5 animate-spin" /> Carregando…
          </div>
        ) : itens.length === 0 ? (
          <p className="py-12 text-center text-sm text-agro-muted">Nenhum evento registrado.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-[var(--primary-50)] text-xs uppercase text-agro-muted">
                <tr>
                  <th className="px-4 py-3">Data</th>
                  <th className="px-4 py-3">Ação</th>
                  <th className="px-4 py-3">Detalhe</th>
                  <th className="px-4 py-3 hidden sm:table-cell">IP</th>
                </tr>
              </thead>
              <tbody>
                {itens.map((e) => (
                  <tr key={e.id} className="border-t border-[var(--border)]">
                    <td className="whitespace-nowrap px-4 py-3 text-agro-muted">
                      {new Date(e.createdAt).toLocaleString("pt-BR")}
                    </td>
                    <td className="px-4 py-3 font-medium">{e.acao}</td>
                    <td className="max-w-xs truncate px-4 py-3 text-agro-muted" title={e.detalhe}>
                      {e.detalhe || e.recurso || "—"}
                    </td>
                    <td className="hidden px-4 py-3 text-agro-muted sm:table-cell">{e.ip || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex justify-between border-t border-[var(--border)] px-6 py-3">
          <button
            type="button"
            className="btn-ghost text-sm"
            disabled={pagina <= 0 || loading}
            onClick={() => void carregar(pagina - 1)}
          >
            Anterior
          </button>
          <span className="text-sm text-agro-muted">Página {pagina + 1}</span>
          <button
            type="button"
            className="btn-ghost text-sm"
            disabled={itens.length < 40 || loading}
            onClick={() => void carregar(pagina + 1)}
          >
            Próxima
          </button>
        </div>
      </section>

      {erro && <p className="text-sm text-rose-600">{erro}</p>}
    </div>
  );
}
