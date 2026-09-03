"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2, Pencil, Plus, Trash2 } from "lucide-react";
import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { excluirMenu, listarCatalogoMenus } from "@/lib/menu/repository";
import { buildMenuTreeAdmin, resolveOutcome, FALLBACK_MENU } from "@/lib/menu/tree";
import type { MenuItemDto, MenuNode } from "@/lib/menu/types";

function outcomeHref(node: MenuNode): string {
  const resolved = resolveOutcome(node.outcome, node.label);
  if (!resolved) return node.outcome?.trim() || "—";
  return resolved.href;
}

function MenuTreePreview({ nodes, depth = 0 }: { nodes: MenuNode[]; depth?: number }) {
  if (!nodes.length) return null;
  return (
    <ul className={depth === 0 ? "space-y-1" : "mt-1 space-y-0.5 border-l border-[var(--surface-border)] pl-3"}>
      {nodes.map((node) => (
        <li key={node.id}>
          <div className="flex min-w-0 flex-wrap items-baseline gap-x-2 gap-y-0.5 py-0.5 text-sm">
            <span className="font-medium text-agro-body">{node.label}</span>
            <span className="break-all font-mono text-xs text-agro-muted">{outcomeHref(node)}</span>
          </div>
          {node.children.length > 0 ? <MenuTreePreview nodes={node.children} depth={depth + 1} /> : null}
        </li>
      ))}
    </ul>
  );
}

function ConfigurarMenuContent() {
  const router = useRouter();
  const [menus, setMenus] = useState<MenuItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setMenus(await listarCatalogoMenus());
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erro ao carregar menus");
      setMenus([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const previewTree = useMemo(() => {
    if (!menus.length) return FALLBACK_MENU;
    const tree = buildMenuTreeAdmin(menus);
    return tree.length ? tree : FALLBACK_MENU;
  }, [menus]);

  const handleDelete = async (item: MenuItemDto) => {
    if (item.id == null) return;
    if (!window.confirm(`Excluir o item "${item.label}"?`)) return;
    setDeletingId(item.id);
    try {
      await excluirMenu(item.id);
      setMenus((prev) => prev.filter((m) => m.id !== item.id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erro ao excluir");
    } finally {
      setDeletingId(null);
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
          <h1 className="text-xl font-semibold text-agro-body">Configurar Menu</h1>
          <p className="mt-1 text-sm text-agro-muted">
            Esquema dinâmico (pai/filho + submenus), igual ao AgrowSync.
          </p>
        </div>
        <Link
          href="/parametros/configurar-menu/cadastro"
          className="inline-flex items-center gap-2 rounded-lg bg-[var(--primary-color)] px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Novo item
        </Link>
      </div>

      {error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</div>
      ) : null}

      <div className="rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)] p-4 shadow-sm">
        <h2 className="text-sm font-semibold text-agro-body">Prévia da navegação</h2>
        <p className="mt-1 text-xs text-agro-muted">
          Árvore montada com resolução de rotas — o que o menu lateral usa.
        </p>
        <div className="mt-3 max-h-[380px] overflow-auto">
          <MenuTreePreview nodes={previewTree} />
        </div>
      </div>

      <div className="overflow-x-auto rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)]">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[var(--surface-border)] bg-[var(--surface-ground)] text-xs uppercase text-agro-muted">
            <tr>
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">Ícone</th>
              <th className="px-4 py-3">Ordem</th>
              <th className="px-4 py-3">Caminho</th>
              <th className="px-4 py-3">Operador</th>
              <th className="px-4 py-3">Ativo</th>
              <th className="px-4 py-3 text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            {menus.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-agro-muted">
                  Nenhum item cadastrado — o sidebar usa o menu padrão.
                </td>
              </tr>
            ) : (
              menus.map((m) => (
                <tr key={m.id} className="border-b border-[var(--surface-border)] last:border-0">
                  <td className="px-4 py-3 font-medium text-agro-body">{m.label}</td>
                  <td className="px-4 py-3 font-mono text-xs text-agro-muted">{m.icon || "—"}</td>
                  <td className="px-4 py-3">{m.ordemMenu ?? 0}</td>
                  <td className="max-w-[220px] truncate px-4 py-3 font-mono text-xs text-agro-muted">
                    {m.outcome || "—"}
                  </td>
                  <td className="px-4 py-3">{m.operadorTemAcesso ?? "SIM"}</td>
                  <td className="px-4 py-3">{m.ativo === false ? "Não" : "Sim"}</td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        className="rounded p-1.5 text-agro-muted hover:bg-[var(--surface-hover)] hover:text-agro-body"
                        title="Editar"
                        onClick={() => router.push(`/parametros/configurar-menu/cadastro?id=${m.id}`)}
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        type="button"
                        className="rounded p-1.5 text-agro-muted hover:bg-red-50 hover:text-red-600"
                        title="Excluir"
                        disabled={deletingId === m.id}
                        onClick={() => void handleDelete(m)}
                      >
                        {deletingId === m.id ? <Loader2 size={16} className="animate-spin" /> : <Trash2 size={16} />}
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

export default function ConfigurarMenuPage() {
  return (
    <GestaoGuard>
      <ConfigurarMenuContent />
    </GestaoGuard>
  );
}
