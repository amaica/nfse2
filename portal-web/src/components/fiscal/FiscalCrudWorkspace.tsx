"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { fiscalApi, type FiscalField } from "@/lib/fiscal-api";

type Props = {
  title: string;
  endpoint: string;
  columns: { key: string; label: string }[];
  fields: FiscalField[];
  defaultForm?: Record<string, unknown>;
  reformaSection?: boolean;
};

export function FiscalCrudWorkspace({
  title,
  endpoint,
  columns,
  fields,
  defaultForm = {},
  reformaSection = false,
}: Props) {
  const [itens, setItens] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<Record<string, unknown>>(defaultForm);
  const [showForm, setShowForm] = useState(false);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro("");
    try {
      const data = await fiscalApi.list<Record<string, unknown>>(endpoint);
      setItens(Array.isArray(data) ? data : []);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar");
    } finally {
      setLoading(false);
    }
  }, [endpoint]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const abrirNovo = () => {
    setEditId(null);
    setForm({ ...defaultForm });
    setShowForm(true);
  };

  const abrirEditar = (row: Record<string, unknown>) => {
    setEditId(Number(row.id));
    const next: Record<string, unknown> = { ...defaultForm };
    for (const f of fields) {
      if (row[f.key] !== undefined) next[f.key] = row[f.key];
    }
    setForm(next);
    setShowForm(true);
  };

  const salvar = async () => {
    setErro("");
    try {
      const body = { ...form };
      for (const f of fields) {
        if (f.type === "number" && body[f.key] !== undefined && body[f.key] !== "") {
          body[f.key] = Number(body[f.key]);
        }
        if (f.type === "checkbox") {
          body[f.key] = Boolean(body[f.key]);
        }
      }
      if (editId) {
        await fiscalApi.update(endpoint, editId, body);
      } else {
        await fiscalApi.create(endpoint, body);
      }
      setShowForm(false);
      await carregar();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao salvar");
    }
  };

  const excluir = async (id: number) => {
    if (!confirm("Excluir registro?")) return;
    try {
      await fiscalApi.remove(endpoint, id);
      await carregar();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao excluir");
    }
  };

  const baseFields = fields.filter((f) => !f.reforma);
  const reformaFields = fields.filter((f) => f.reforma);

  const renderField = (f: FiscalField) => (
    <label key={f.key} className="block text-sm">
      <span className="mb-1 block font-medium text-slate-700">{f.label}</span>
      {f.type === "select" ? (
        <select
          className="fiscal-input w-full"
          value={String(form[f.key] ?? "")}
          onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
        >
          {f.options?.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      ) : f.type === "checkbox" ? (
        <input
          type="checkbox"
          checked={Boolean(form[f.key])}
          onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.checked }))}
          className="h-4 w-4"
        />
      ) : f.type === "textarea" ? (
        <textarea
          className="fiscal-input w-full"
          rows={3}
          value={String(form[f.key] ?? "")}
          onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
        />
      ) : (
        <input
          type={f.type === "number" ? "number" : "text"}
          step={f.type === "number" ? "any" : undefined}
          className="fiscal-input w-full"
          value={String(form[f.key] ?? "")}
          onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
        />
      )}
    </label>
  );

  return (
    <div className="fiscal-card">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800">{title}</h1>
        <div className="flex gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={abrirNovo}>
            Novo
          </button>
          <button
            type="button"
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50"
            onClick={carregar}
          >
            Atualizar
          </button>
        </div>
      </div>

      {erro && (
        <p className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{erro}</p>
      )}

      {showForm && (
        <div className="mb-6 rounded-xl border border-slate-200 bg-slate-50 p-4">
          <h2 className="mb-3 font-semibold text-slate-800">
            {editId ? "Editar" : "Novo"} registro
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {baseFields.map(renderField)}
          </div>
          {reformaSection && reformaFields.length > 0 && (
            <div className="mt-4 border-t border-amber-200 pt-4">
              <div className="mb-2 flex items-center gap-2">
                <h3 className="font-semibold text-amber-900">Reforma Tributária (IBS/CBS)</h3>
                <span className="rounded bg-amber-100 px-2 py-0.5 text-xs text-amber-800">
                  Obrigatório NF-e a partir de 03/08/2026 — alíquota teste 1%
                </span>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {reformaFields.map(renderField)}
              </div>
            </div>
          )}
          <div className="mt-4 flex gap-2">
            <button type="button" className="fiscal-btn-primary" onClick={salvar}>
              Salvar
            </button>
            <button
              type="button"
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              onClick={() => setShowForm(false)}
            >
              Cancelar
            </button>
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="fiscal-table">
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key}>{c.label}</th>
              ))}
              <th style={{ width: "8rem" }} />
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length + 1} className="py-8 text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : itens.length === 0 ? (
              <tr>
                <td colSpan={columns.length + 1} className="py-8 text-center text-slate-500">
                  Nenhum registro
                </td>
              </tr>
            ) : (
              itens.map((row) => (
                <tr key={String(row.id)}>
                  {columns.map((c) => (
                    <td key={c.key}>{String(row[c.key] ?? "")}</td>
                  ))}
                  <td className="space-x-1 whitespace-nowrap">
                    <button
                      type="button"
                      className="text-sm text-blue-600 hover:underline"
                      onClick={() => abrirEditar(row)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="text-sm text-red-600 hover:underline"
                      onClick={() => excluir(Number(row.id))}
                    >
                      Excluir
                    </button>
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
