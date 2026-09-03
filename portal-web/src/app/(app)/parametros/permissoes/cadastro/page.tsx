"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, Loader2 } from "lucide-react";
import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { listarCatalogoMenus } from "@/lib/menu/repository";
import type { MenuItemDto } from "@/lib/menu/types";
import {
  buscarPerfil,
  excluirPerfil,
  salvarPerfil,
} from "@/lib/permissoes-api";

function menusOrdenados(catalogo: MenuItemDto[]): MenuItemDto[] {
  const byParent = new Map<number | null, MenuItemDto[]>();
  for (const m of catalogo) {
    if (m.ativo === false) continue;
    const pid = m.parent?.id ?? null;
    const list = byParent.get(pid) ?? [];
    list.push(m);
    byParent.set(pid, list);
  }
  const sortFn = (a: MenuItemDto, b: MenuItemDto) =>
    (a.ordemMenu ?? 0) - (b.ordemMenu ?? 0) ||
    (a.label ?? "").localeCompare(b.label ?? "", "pt-BR");
  const out: MenuItemDto[] = [];
  const walk = (parentId: number | null) => {
    for (const k of (byParent.get(parentId) ?? []).slice().sort(sortFn)) {
      out.push(k);
      if (k.id != null) walk(k.id);
    }
  };
  walk(null);
  return out;
}

function depthOf(item: MenuItemDto, byId: Map<number, MenuItemDto>): number {
  let d = 0;
  let pid = item.parent?.id;
  while (pid != null) {
    d += 1;
    pid = byId.get(pid)?.parent?.id;
    if (d > 8) break;
  }
  return d;
}

function CadastroPermissaoForm() {
  const router = useRouter();
  const params = useSearchParams();
  const editId = params.get("id");
  const perfilId = editId && Number.isInteger(Number(editId)) ? Number(editId) : undefined;

  const [nome, setNome] = useState("");
  const [descricao, setDescricao] = useState("");
  const [ativo, setAtivo] = useState(true);
  const [catalogo, setCatalogo] = useState<MenuItemDto[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(Boolean(perfilId));
  const [saving, setSaving] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const menus = await listarCatalogoMenus();
        if (cancelled) return;
        setCatalogo(menus);
        if (!perfilId) {
          setSelected(
            new Set(
              menus
                .filter(
                  (m) =>
                    m.id != null &&
                    m.ativo !== false &&
                    String(m.operadorTemAcesso ?? "SIM").toUpperCase() !== "NAO",
                )
                .map((m) => m.id as number),
            ),
          );
          return;
        }
        setLoading(true);
        const p = await buscarPerfil(perfilId);
        if (cancelled) return;
        setNome(p.nome ?? "");
        setDescricao(p.descricao ?? "");
        setAtivo(p.ativo !== false);
        setSelected(new Set(p.menuIds ?? []));
      } catch (e) {
        if (!cancelled) setErro(e instanceof Error ? e.message : "Erro ao carregar");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [perfilId]);

  const flat = useMemo(() => menusOrdenados(catalogo), [catalogo]);
  const byId = useMemo(() => {
    const map = new Map<number, MenuItemDto>();
    for (const m of catalogo) if (m.id != null) map.set(m.id, m);
    return map;
  }, [catalogo]);

  const toggle = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleSave = useCallback(async () => {
    if (!nome.trim()) {
      setErro("Nome é obrigatório.");
      return;
    }
    setSaving(true);
    setErro("");
    try {
      await salvarPerfil({
        id: perfilId,
        nome: nome.trim(),
        descricao: descricao.trim() || undefined,
        ativo,
        menuIds: Array.from(selected),
      });
      router.push("/parametros/permissoes");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar");
    } finally {
      setSaving(false);
    }
  }, [ativo, descricao, nome, perfilId, router, selected]);

  const handleDelete = useCallback(async () => {
    if (!perfilId || !window.confirm("Excluir este grupo?")) return;
    try {
      await excluirPerfil(perfilId);
      router.push("/parametros/permissoes");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao excluir");
    }
  }, [perfilId, router]);

  if (loading) {
    return (
      <div className="flex items-center gap-2 py-12 text-sm text-agro-muted">
        <Loader2 className="animate-spin" size={18} /> Carregando…
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => router.push("/parametros/permissoes")}
            className="rounded-lg border border-[var(--surface-border)] p-2 text-agro-muted hover:bg-[var(--surface-hover)]"
          >
            <ArrowLeft size={18} />
          </button>
          <h1 className="text-xl font-semibold text-agro-body">
            {perfilId ? "Editar Grupo de Permissão" : "Novo Grupo de Permissão"}
          </h1>
        </div>
        <div className="flex gap-2">
          {perfilId ? (
            <button
              type="button"
              onClick={() => void handleDelete()}
              className="rounded-lg border border-red-200 px-3 py-2 text-sm text-red-600"
            >
              Excluir
            </button>
          ) : null}
          <button
            type="button"
            disabled={saving}
            onClick={() => void handleSave()}
            className="inline-flex items-center gap-2 rounded-lg bg-[var(--primary-color)] px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
          >
            {saving ? <Loader2 size={16} className="animate-spin" /> : null}
            Salvar
          </button>
        </div>
      </div>

      {erro ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">{erro}</div>
      ) : null}

      <div className="grid gap-4 rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)] p-4 md:grid-cols-2">
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Nome / Grupo *</span>
          <input
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            placeholder="Ex: Operador Werlang"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Descrição</span>
          <input
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            placeholder="Descrição do grupo"
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
          />
        </label>
        <label className="inline-flex items-center gap-2 text-sm md:col-span-2">
          <input type="checkbox" checked={ativo} onChange={(e) => setAtivo(e.target.checked)} />
          Ativo
        </label>
      </div>

      <div className="rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)] p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-agro-body">Menus liberados</h2>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-lg border border-[var(--surface-border)] px-3 py-1.5 text-sm"
              onClick={() =>
                setSelected(
                  new Set(catalogo.filter((m) => m.id != null && m.ativo !== false).map((m) => m.id!)),
                )
              }
            >
              Marcar todos
            </button>
            <button
              type="button"
              className="rounded-lg border border-[var(--surface-border)] px-3 py-1.5 text-sm"
              onClick={() => setSelected(new Set())}
            >
              Desmarcar todos
            </button>
          </div>
        </div>
        <div className="max-h-[420px] space-y-1 overflow-y-auto">
          {flat.map((m) => {
            if (m.id == null) return null;
            const depth = depthOf(m, byId);
            return (
              <label
                key={m.id}
                className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-[var(--surface-hover)]"
                style={{ paddingLeft: `${0.5 + depth * 1.25}rem` }}
              >
                <input
                  type="checkbox"
                  checked={selected.has(m.id)}
                  onChange={() => toggle(m.id!)}
                />
                <span className="font-medium text-agro-body">{m.label}</span>
                {m.outcome ? (
                  <span className="font-mono text-xs text-agro-muted">{m.outcome}</span>
                ) : null}
              </label>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default function PermissoesCadastroPage() {
  return (
    <GestaoGuard>
      <Suspense fallback={<div className="py-12 text-sm text-agro-muted">Carregando…</div>}>
        <CadastroPermissaoForm />
      </Suspense>
    </GestaoGuard>
  );
}
