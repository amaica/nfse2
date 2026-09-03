"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, Pencil, Plus, Trash2 } from "lucide-react";
import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { excluirPerfil, listarPerfis, type PortalPerfilDto } from "@/lib/permissoes-api";

function PermissoesContent() {
  const router = useRouter();
  const [lista, setLista] = useState<PortalPerfilDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [deleting, setDeleting] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro("");
    try {
      setLista(await listarPerfis());
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar");
      setLista([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const handleDelete = async (p: PortalPerfilDto) => {
    if (p.id == null) return;
    if (!window.confirm(`Excluir o grupo "${p.nome}"?`)) return;
    setDeleting(p.id);
    try {
      await excluirPerfil(p.id);
      setLista((prev) => prev.filter((x) => x.id !== p.id));
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao excluir");
    } finally {
      setDeleting(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center gap-2 py-12 text-sm text-agro-muted">
        <Loader2 className="animate-spin" size={18} /> Carregando…
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-agro-body">Permissões de Acesso</h1>
          <p className="mt-1 text-sm text-agro-muted">
            Cadastre grupos (perfis) e marque quais itens do menu cada grupo vê. Depois vincule o
            grupo ao usuário em Cadastros → Usuários.
          </p>
        </div>
        <Link
          href="/parametros/permissoes/cadastro"
          className="inline-flex items-center gap-2 rounded-lg bg-[var(--primary-color)] px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Novo Grupo
        </Link>
      </div>

      {erro ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">{erro}</div>
      ) : null}

      <div className="overflow-x-auto rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)]">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[var(--surface-border)] bg-[var(--surface-ground)] text-xs uppercase text-agro-muted">
            <tr>
              <th className="px-4 py-3">Grupo</th>
              <th className="px-4 py-3">Descrição</th>
              <th className="px-4 py-3">Menus</th>
              <th className="px-4 py-3 text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            {lista.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-agro-muted">
                  Nenhum grupo cadastrado. Crie um (ex.: Operador Werlang) e marque os menus.
                </td>
              </tr>
            ) : (
              lista.map((p) => (
                <tr key={p.id} className="border-b border-[var(--surface-border)] last:border-0">
                  <td className="px-4 py-3 font-medium text-agro-body">{p.nome}</td>
                  <td className="px-4 py-3 text-agro-muted">{p.descricao || "—"}</td>
                  <td className="px-4 py-3">{p.totalMenus ?? p.menuIds?.length ?? 0}</td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        className="rounded p-1.5 text-agro-muted hover:bg-[var(--surface-hover)]"
                        onClick={() => router.push(`/parametros/permissoes/cadastro?id=${p.id}`)}
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        type="button"
                        className="rounded p-1.5 text-agro-muted hover:bg-red-50 hover:text-red-600"
                        disabled={deleting === p.id}
                        onClick={() => void handleDelete(p)}
                      >
                        {deleting === p.id ? (
                          <Loader2 size={16} className="animate-spin" />
                        ) : (
                          <Trash2 size={16} />
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function PermissoesPage() {
  return (
    <GestaoGuard>
      <PermissoesContent />
    </GestaoGuard>
  );
}
