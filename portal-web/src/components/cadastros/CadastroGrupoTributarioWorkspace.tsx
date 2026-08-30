"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Pencil, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { fiscalApi, type GrupoTributarioDto } from "@/lib/fiscal-api";
import {
  labelOrigemMercadoria,
  opcoesOrigemComValorAtual,
} from "@/lib/origem-mercadoria";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalSection } from "@/components/fiscal/FiscalFormUi";

const PAGE_SIZE = 20;
const ENDPOINT = "/api/tribut-grupo-tributario";

const emptyGrupo = (): GrupoTributarioDto => ({
  descricao: "",
  origemMercadoria: "0",
  observacao: "",
});

function blank(v?: string | null): string | undefined {
  const t = (v ?? "").trim();
  return t ? t : undefined;
}

export function CadastroGrupoTributarioWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [grupos, setGrupos] = useState<GrupoTributarioDto[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<GrupoTributarioDto>(emptyGrupo());
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      setGrupos(await fiscalApi.listGruposTributarios());
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar grupos tributários");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyGrupo());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const kpis = useMemo(() => {
    const total = grupos.length;
    const semOrigem = grupos.filter((g) => !(g.origemMercadoria ?? "").trim()).length;
    const nacional = grupos.filter((g) => g.origemMercadoria === "0").length;
    return { total, semOrigem, nacional, outros: total - nacional - semOrigem };
  }, [grupos]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    if (!q) return grupos;
    return grupos.filter((g) => {
      const origemLabel = labelOrigemMercadoria(g.origemMercadoria).toLowerCase();
      return (
        String(g.id ?? "").includes(q) ||
        (g.descricao ?? "").toLowerCase().includes(q) ||
        (g.origemMercadoria ?? "").includes(q) ||
        origemLabel.includes(q) ||
        (g.observacao ?? "").toLowerCase().includes(q)
      );
    });
  }, [grupos, filtro]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyGrupo());
  };

  const novo = () => {
    setEditId(null);
    setForm(emptyGrupo());
    setViewMode("form");
  };

  const editar = async (row: GrupoTributarioDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<GrupoTributarioDto>(ENDPOINT, row.id);
      setEditId(row.id);
      setForm({ ...emptyGrupo(), ...full, origemMercadoria: full.origemMercadoria || "0" });
      setViewMode("form");
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const salvar = async () => {
    if (!form.descricao?.trim()) {
      setErro("Informe a descrição do grupo.");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const body: GrupoTributarioDto = {
        descricao: form.descricao.trim(),
        origemMercadoria: form.origemMercadoria || "0",
        observacao: blank(form.observacao),
      };
      if (editId) {
        await fiscalApi.update(ENDPOINT, editId, body);
      } else {
        await fiscalApi.create(ENDPOINT, body);
      }
      await carregarLista();
      irPesquisa();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao salvar");
    } finally {
      setSalvando(false);
    }
  };

  const excluir = async (row: GrupoTributarioDto) => {
    if (!row.id) return;
    if (!window.confirm(`Excluir o grupo "${row.descricao}"?`)) return;
    setErro("");
    try {
      await fiscalApi.remove(ENDPOINT, row.id);
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao excluir");
    }
  };

  const origens = opcoesOrigemComValorAtual(form.origemMercadoria);

  if (viewMode === "form") {
    const titulo = editId ? `Grupo ${form.descricao || editId}` : "Novo grupo tributário";
    return (
      <div className="fiscal-card">
        <FiscalDetailToolbar
          title={titulo}
          onVoltar={irPesquisa}
          onNovo={novo}
          onCancelar={irPesquisa}
          onSalvar={salvar}
          saveDisabled={salvando}
        />
        {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

        <div className="erp-master erp-master--single">
          <FiscalField label="Descrição *">
            <input
              className="fiscal-input"
              maxLength={255}
              value={form.descricao}
              autoFocus={!editId}
              onChange={(e) => setForm((f) => ({ ...f, descricao: e.target.value }))}
            />
          </FiscalField>
        </div>

        <FiscalSection title="Classificação ICMS">
          <FiscalField label="Origem da mercadoria">
            <select
              className="fiscal-input"
              value={form.origemMercadoria || "0"}
              onChange={(e) => setForm((f) => ({ ...f, origemMercadoria: e.target.value }))}
            >
              {origens.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
            <span className="erp-hint">
              Código gravado na NF-e (tag orig). O mesmo conjunto usado no cadastro de produtos.
            </span>
          </FiscalField>
          <FiscalField label="Observação interna">
            <textarea
              className="fiscal-input"
              rows={4}
              maxLength={1000}
              value={form.observacao ?? ""}
              onChange={(e) => setForm((f) => ({ ...f, observacao: e.target.value }))}
              placeholder="Uso interno — não vai para a NF-e"
            />
          </FiscalField>
        </FiscalSection>
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="erp-list-head">
        <div>
          <h1 className="erp-list-head__title">Grupos tributários</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Novo grupo
          </button>
          <button type="button" className="fiscal-btn-secondary" onClick={() => void carregarLista()}>
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
        </div>
      </div>

      <div className="erp-kpis">
        <div className="erp-kpi">
          <div className="erp-kpi__label">Cadastrados</div>
          <div className="erp-kpi__value">{kpis.total}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Origem nacional</div>
          <div className="erp-kpi__value">{kpis.nacional}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Outras origens</div>
          <div className="erp-kpi__value">{kpis.outros}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Sem origem</div>
          <div className="erp-kpi__value">{kpis.semOrigem}</div>
        </div>
      </div>

      {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

      <div className="fiscal-table-caption">
        <span>Filtro na grade</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Descrição, origem…"
            value={filtro}
            onChange={(e) => {
              setFiltro(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="fiscal-table striped fiscal-table--dense">
          <thead>
            <tr>
              <th>Descrição</th>
              <th>Origem da mercadoria</th>
              <th>Observação</th>
              <th style={{ width: "6.5rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={4} className="text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={4} className="text-center text-slate-500">
                  Nenhum registro neste filtro
                </td>
              </tr>
            ) : (
              pagina.map((row) => (
                <tr key={row.id} onDoubleClick={() => void editar(row)}>
                  <td className="font-medium">{row.descricao}</td>
                  <td>{labelOrigemMercadoria(row.origemMercadoria)}</td>
                  <td className="max-w-[18rem] truncate" title={row.observacao}>
                    {row.observacao || "—"}
                  </td>
                  <td>
                    <div className="fiscal-table-actions">
                      <button
                        type="button"
                        className="fiscal-btn-icon"
                        aria-label="Editar"
                        title="Editar"
                        onClick={() => void editar(row)}
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        className="fiscal-btn-icon danger"
                        aria-label="Excluir"
                        title="Excluir"
                        onClick={() => void excluir(row)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="fiscal-pagination">
        <span>
          {filtrados.length === 0
            ? "0 registros"
            : `${page * PAGE_SIZE + 1}–${Math.min((page + 1) * PAGE_SIZE, filtrados.length)} de ${filtrados.length}`}
          <span className="ml-2 text-slate-400">duplo clique abre o cadastro</span>
        </span>
        <div className="flex gap-2">
          <button
            type="button"
            className="rounded border border-slate-300 px-2 py-1 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Anterior
          </button>
          <button
            type="button"
            className="rounded border border-slate-300 px-2 py-1 disabled:opacity-40"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  );
}
