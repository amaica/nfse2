"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, Loader2, Plus, Trash2 } from "lucide-react";
import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { buscarMenu, excluirMenu, listarMenus, salvarMenu } from "@/lib/menu/repository";
import type { MenuItemDto, SubmenuItem } from "@/lib/menu/types";

type FormState = {
  label: string;
  icon: string;
  ordemMenu: string;
  outcome: string;
  operadorTemAcesso: "SIM" | "NAO";
};

const EMPTY: FormState = {
  label: "",
  icon: "",
  ordemMenu: "0",
  outcome: "",
  operadorTemAcesso: "SIM",
};

function CadastroMenuForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryId = searchParams.get("id");
  const [menuId, setMenuId] = useState<number | undefined>(
    queryId && Number.isInteger(Number(queryId)) ? Number(queryId) : undefined,
  );
  const [form, setForm] = useState<FormState>(EMPTY);
  const [ativo, setAtivo] = useState(true);
  const [parentId, setParentId] = useState<string>("");
  const [menus, setMenus] = useState<MenuItemDto[]>([]);
  const [submenus, setSubmenus] = useState<SubmenuItem[]>([]);
  const [loading, setLoading] = useState(Boolean(menuId));
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const [subLabel, setSubLabel] = useState("");
  const [subIcon, setSubIcon] = useState("");
  const [subOutcome, setSubOutcome] = useState("");

  useEffect(() => {
    let cancelled = false;
    void listarMenus()
      .then((list) => {
        if (!cancelled) setMenus(list);
      })
      .catch(() => {
        /* parent select vazio */
      });

    if (!menuId) {
      setLoading(false);
      return () => {
        cancelled = true;
      };
    }

    setLoading(true);
    buscarMenu(menuId)
      .then((menu) => {
        if (cancelled || !menu) return;
        setForm({
          label: menu.label ?? "",
          icon: menu.icon ?? "",
          ordemMenu: String(menu.ordemMenu ?? 0),
          outcome: menu.outcome ?? "",
          operadorTemAcesso: menu.operadorTemAcesso ?? "SIM",
        });
        setAtivo(menu.ativo ?? true);
        setSubmenus(menu.submenus ?? []);
        setParentId(menu.parent?.id != null ? String(menu.parent.id) : "");
      })
      .catch((e) => {
        if (!cancelled) {
          setMessage({ type: "error", text: e instanceof Error ? e.message : "Erro ao carregar" });
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [menuId]);

  const parentOptions = useMemo(
    () => menus.filter((m) => m.id != null && m.id !== menuId),
    [menuId, menus],
  );

  const handleSave = useCallback(async () => {
    const label = form.label.trim();
    const ordem = Number(form.ordemMenu);
    if (!label) {
      setMessage({ type: "error", text: "Nome é obrigatório." });
      return;
    }
    if (!Number.isInteger(ordem) || ordem < 0) {
      setMessage({ type: "error", text: "Ordem deve ser um inteiro >= 0." });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const saved = await salvarMenu({
        id: menuId,
        label,
        icon: form.icon.trim(),
        ordemMenu: ordem,
        outcome: form.outcome.trim(),
        operadorTemAcesso: form.operadorTemAcesso,
        ativo,
        parent: parentId ? { id: Number(parentId) } : null,
        submenus,
      });
      setMenuId(saved.id);
      setSubmenus(saved.submenus ?? submenus);
      setMessage({ type: "success", text: "Salvo com sucesso." });
      if (saved.id) router.replace(`/parametros/configurar-menu/cadastro?id=${saved.id}`);
    } catch (e) {
      setMessage({ type: "error", text: e instanceof Error ? e.message : "Erro ao salvar" });
    } finally {
      setSaving(false);
    }
  }, [ativo, form, menuId, parentId, router, submenus]);

  const handleDelete = useCallback(async () => {
    if (!menuId || !window.confirm("Excluir este item de menu?")) return;
    try {
      await excluirMenu(menuId);
      router.push("/parametros/configurar-menu");
    } catch (e) {
      setMessage({ type: "error", text: e instanceof Error ? e.message : "Erro ao excluir" });
    }
  }, [menuId, router]);

  const addSubmenu = () => {
    const label = subLabel.trim();
    const outcome = subOutcome.trim();
    if (!label || !outcome) {
      setMessage({ type: "error", text: "Submenu precisa de nome e caminho." });
      return;
    }
    setSubmenus((prev) => [...prev, { label, icon: subIcon.trim(), outcome }]);
    setSubLabel("");
    setSubIcon("");
    setSubOutcome("");
  };

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
            onClick={() => router.push("/parametros/configurar-menu")}
            className="rounded-lg border border-[var(--surface-border)] p-2 text-agro-muted hover:bg-[var(--surface-hover)]"
          >
            <ArrowLeft size={18} />
          </button>
          <h1 className="text-xl font-semibold text-agro-body">
            {menuId ? "Editar item de menu" : "Novo item de menu"}
          </h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {menuId ? (
            <button
              type="button"
              onClick={() => void handleDelete()}
              className="rounded-lg border border-red-200 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
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

      {message ? (
        <div
          className={`rounded-lg border px-4 py-3 text-sm ${
            message.type === "success"
              ? "border-green-200 bg-green-50 text-green-800"
              : "border-red-200 bg-red-50 text-red-700"
          }`}
        >
          {message.text}
        </div>
      ) : null}

      <div className="grid gap-4 rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)] p-4 md:grid-cols-2">
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Nome</span>
          <input
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            value={form.label}
            onChange={(e) => setForm((f) => ({ ...f, label: e.target.value }))}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Ícone (lucide key)</span>
          <input
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            placeholder="home, file, settings…"
            value={form.icon}
            onChange={(e) => setForm((f) => ({ ...f, icon: e.target.value }))}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Ordem</span>
          <input
            type="number"
            min={0}
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            value={form.ordemMenu}
            onChange={(e) => setForm((f) => ({ ...f, ordemMenu: e.target.value }))}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Caminho (outcome)</span>
          <input
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            placeholder="/painel"
            value={form.outcome}
            onChange={(e) => setForm((f) => ({ ...f, outcome: e.target.value }))}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-agro-muted">Menu pai</span>
          <select
            className="w-full rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2"
            value={parentId}
            onChange={(e) => setParentId(e.target.value)}
          >
            <option value="">(raiz)</option>
            {parentOptions.map((m) => (
              <option key={m.id} value={m.id}>
                {m.label}
              </option>
            ))}
          </select>
        </label>
        <fieldset className="text-sm">
          <legend className="mb-1 text-agro-muted">Operador tem acesso</legend>
          <div className="flex gap-4 pt-1">
            {(["SIM", "NAO"] as const).map((v) => (
              <label key={v} className="inline-flex items-center gap-2">
                <input
                  type="radio"
                  name="operador"
                  checked={form.operadorTemAcesso === v}
                  onChange={() => setForm((f) => ({ ...f, operadorTemAcesso: v }))}
                />
                {v}
              </label>
            ))}
          </div>
        </fieldset>
        <label className="inline-flex items-center gap-2 text-sm md:col-span-2">
          <input type="checkbox" checked={ativo} onChange={(e) => setAtivo(e.target.checked)} />
          Ativo
        </label>
      </div>

      <div className="rounded-xl border border-[var(--surface-border)] bg-[var(--surface-card)] p-4">
        <h2 className="mb-3 text-sm font-semibold text-agro-body">Submenus (legado / extras)</h2>
        <div className="mb-3 grid gap-2 md:grid-cols-4">
          <input
            className="rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2 text-sm"
            placeholder="Nome"
            value={subLabel}
            onChange={(e) => setSubLabel(e.target.value)}
          />
          <input
            className="rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2 text-sm"
            placeholder="Ícone"
            value={subIcon}
            onChange={(e) => setSubIcon(e.target.value)}
          />
          <input
            className="rounded-lg border border-[var(--surface-border)] bg-transparent px-3 py-2 text-sm"
            placeholder="/rota"
            value={subOutcome}
            onChange={(e) => setSubOutcome(e.target.value)}
          />
          <button
            type="button"
            onClick={addSubmenu}
            className="inline-flex items-center justify-center gap-1 rounded-lg border border-[var(--surface-border)] px-3 py-2 text-sm hover:bg-[var(--surface-hover)]"
          >
            <Plus size={16} /> Adicionar
          </button>
        </div>
        {submenus.length === 0 ? (
          <p className="text-sm text-agro-muted">Nenhum submenu.</p>
        ) : (
          <ul className="divide-y divide-[var(--surface-border)]">
            {submenus.map((s, i) => (
              <li key={`${s.label}-${i}`} className="flex items-center justify-between gap-2 py-2 text-sm">
                <span>
                  <strong>{s.label}</strong>{" "}
                  <span className="font-mono text-xs text-agro-muted">{s.outcome}</span>
                </span>
                <button
                  type="button"
                  className="rounded p-1 text-agro-muted hover:text-red-600"
                  onClick={() => setSubmenus((prev) => prev.filter((_, idx) => idx !== i))}
                >
                  <Trash2 size={16} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default function ConfigurarMenuCadastroPage() {
  return (
    <GestaoGuard>
      <Suspense fallback={<div className="py-12 text-sm text-agro-muted">Carregando…</div>}>
        <CadastroMenuForm />
      </Suspense>
    </GestaoGuard>
  );
}
