"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Pencil, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import {
  fiscalApi,
  type ConfigOfGtDto,
  type GrupoTributarioDto,
  type IcmsUfDto,
  type OperacaoFiscalDto,
} from "@/lib/fiscal-api";
import { fmtCfop, labelOperacaoFiscal } from "@/lib/cfop";
import { labelOrigemMercadoria, ORIGENS_MERCADORIA } from "@/lib/origem-mercadoria";
import { UFS_BRASIL } from "@/lib/ufs-brasil";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";
import { PercentInput } from "@/components/fiscal/MoedaInput";

const PAGE_SIZE = 20;
const ENDPOINT = "/api/tribut-configura-of-gt";

const emptyIcms = (): IcmsUfDto => ({
  ufDestino: "RS",
  cfop: undefined,
  cst: "",
  csosn: "",
  aliquota: 0,
  origemMercadoria: "0",
});

const emptyCfg = (): ConfigOfGtDto => ({
  tributOperacaoFiscalId: undefined,
  tributGrupoTributarioId: undefined,
  listaIcmsUf: [emptyIcms()],
});

function aliqToPct(a?: number | null): number | undefined {
  if (a == null || Number.isNaN(a)) return undefined;
  return Math.round(a * 10000) / 100;
}

function pctToAliq(p?: number): number | undefined {
  if (p == null || Number.isNaN(p)) return undefined;
  return Math.round(p * 100) / 10000;
}

function resumoCfops(lista?: IcmsUfDto[]): string {
  const codes = [...new Set((lista ?? []).map((i) => fmtCfop(i.cfop)).filter(Boolean))];
  if (codes.length === 0) return "—";
  if (codes.length <= 3) return codes.join(", ");
  return `${codes.slice(0, 3).join(", ")} +${codes.length - 3}`;
}

export function CadastroConfiguraOfGtWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [configs, setConfigs] = useState<ConfigOfGtDto[]>([]);
  const [operacoes, setOperacoes] = useState<OperacaoFiscalDto[]>([]);
  const [grupos, setGrupos] = useState<GrupoTributarioDto[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [filtroOp, setFiltroOp] = useState("");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<ConfigOfGtDto>(emptyCfg());
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  const opPorId = useMemo(() => {
    const m = new Map<number, OperacaoFiscalDto>();
    for (const o of operacoes) if (o.id) m.set(o.id, o);
    return m;
  }, [operacoes]);

  const gtPorId = useMemo(() => {
    const m = new Map<number, GrupoTributarioDto>();
    for (const g of grupos) if (g.id) m.set(g.id, g);
    return m;
  }, [grupos]);

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      const [c, o, g] = await Promise.all([
        fiscalApi.listConfiguracoesOfGt(),
        fiscalApi.listOperacoesFiscais(),
        fiscalApi.listGruposTributarios(),
      ]);
      setConfigs(c);
      setOperacoes(o);
      setGrupos(g);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar configuração tributária");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyCfg());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    const opId = filtroOp ? Number(filtroOp) : 0;
    return configs.filter((c) => {
      if (opId && c.tributOperacaoFiscalId !== opId) return false;
      if (!q) return true;
      const op = opPorId.get(c.tributOperacaoFiscalId ?? 0);
      const gt = gtPorId.get(c.tributGrupoTributarioId ?? 0);
      return (
        (op?.descricao ?? "").toLowerCase().includes(q) ||
        (gt?.descricao ?? "").toLowerCase().includes(q) ||
        fmtCfop(op?.cfop).includes(q) ||
        resumoCfops(c.listaIcmsUf).toLowerCase().includes(q)
      );
    });
  }, [configs, filtro, filtroOp, opPorId, gtPorId]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyCfg());
  };

  const novo = () => {
    setEditId(null);
    setForm(emptyCfg());
    setViewMode("form");
  };

  const editar = async (row: ConfigOfGtDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<ConfigOfGtDto>(ENDPOINT, row.id);
      const icms = [...(full.listaIcmsUf ?? [])].sort((a, b) =>
        (a.ufDestino || "").localeCompare(b.ufDestino || ""),
      );
      setEditId(row.id);
      setForm({
        ...emptyCfg(),
        ...full,
        listaIcmsUf: icms.length ? icms : [emptyIcms()],
      });
      setViewMode("form");
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const setIcms = (idx: number, patch: Partial<IcmsUfDto>) => {
    setForm((f) => {
      const lista = [...(f.listaIcmsUf ?? [])];
      lista[idx] = { ...lista[idx], ...patch };
      return { ...f, listaIcmsUf: lista };
    });
  };

  const addIcms = () => setForm((f) => ({ ...f, listaIcmsUf: [...(f.listaIcmsUf ?? []), emptyIcms()] }));

  const removeIcms = (idx: number) =>
    setForm((f) => ({
      ...f,
      listaIcmsUf: (f.listaIcmsUf ?? []).filter((_, i) => i !== idx),
    }));

  const salvar = async () => {
    if (!form.tributOperacaoFiscalId) {
      setErro("Selecione a operação fiscal.");
      return;
    }
    if (!form.tributGrupoTributarioId) {
      setErro("Selecione o grupo tributário.");
      return;
    }
    const icms = (form.listaIcmsUf ?? [])
      .filter((r) => (r.ufDestino ?? "").trim())
      .map((r) => ({
        ufDestino: r.ufDestino.trim().toUpperCase().slice(0, 2),
        cfop: r.cfop && r.cfop >= 1000 ? Number(r.cfop) : undefined,
        cst: (r.cst ?? "").trim() || undefined,
        csosn: (r.csosn ?? "").trim() || undefined,
        aliquota: r.aliquota ?? 0,
        origemMercadoria: r.origemMercadoria || "0",
      }));
    if (icms.length === 0) {
      setErro("Informe ao menos uma regra de ICMS (UF de destino + CFOP).");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const body: ConfigOfGtDto = {
        tributOperacaoFiscalId: form.tributOperacaoFiscalId,
        tributGrupoTributarioId: form.tributGrupoTributarioId,
        listaIcmsUf: icms,
      };
      if (editId) await fiscalApi.update(ENDPOINT, editId, body);
      else await fiscalApi.create(ENDPOINT, body);
      await carregarLista();
      irPesquisa();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao salvar");
    } finally {
      setSalvando(false);
    }
  };

  const excluir = async (row: ConfigOfGtDto) => {
    if (!row.id) return;
    const op = opPorId.get(row.tributOperacaoFiscalId ?? 0)?.descricao ?? "esta configuração";
    if (!window.confirm(`Excluir o cruzamento de "${op}"? As regras de ICMS deste par serão removidas.`)) return;
    setErro("");
    try {
      await fiscalApi.remove(ENDPOINT, row.id);
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao excluir");
    }
  };

  if (viewMode === "form") {
    const opSel = opPorId.get(form.tributOperacaoFiscalId ?? 0);
    const gtSel = gtPorId.get(form.tributGrupoTributarioId ?? 0);
    const titulo = editId ? "ICMS da operação × grupo" : "Novo cruzamento tributário";
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

        <div className="erp-explain">
          <strong>Como funciona.</strong> O produto leva o <em>grupo tributário</em>. Na emissão você escolhe a{" "}
          <em>operação fiscal</em>. Esta tela junta os dois e define o <em>CFOP/ICMS por UF</em> do destinatário.
        </div>

        <FiscalSection title="Cruzamento">
          <FiscalRow>
            <FiscalField label="Operação fiscal *">
              <select
                className="fiscal-input"
                value={form.tributOperacaoFiscalId ?? ""}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    tributOperacaoFiscalId: e.target.value ? Number(e.target.value) : undefined,
                  }))
                }
              >
                <option value="">— Selecione a operação —</option>
                {operacoes.map((o) => (
                  <option key={o.id} value={o.id}>
                    {labelOperacaoFiscal(o.descricao, o.cfop)}
                  </option>
                ))}
              </select>
            </FiscalField>
            <FiscalField label="Grupo tributário *">
              <select
                className="fiscal-input"
                value={form.tributGrupoTributarioId ?? ""}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    tributGrupoTributarioId: e.target.value ? Number(e.target.value) : undefined,
                  }))
                }
              >
                <option value="">— Selecione o grupo —</option>
                {grupos.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.descricao} · {labelOrigemMercadoria(g.origemMercadoria)}
                  </option>
                ))}
              </select>
            </FiscalField>
          </FiscalRow>
          {(opSel || gtSel) && (
            <p className="erp-hint">
              {opSel ? `Operação: ${opSel.descricao}` : ""}
              {opSel && gtSel ? " → " : ""}
              {gtSel ? `Grupo: ${gtSel.descricao}` : ""}
            </p>
          )}
        </FiscalSection>

        <FiscalSection title="ICMS por UF de destino">
          <p className="erp-hint" style={{ marginBottom: "0.75rem" }}>
            Uma linha por estado do destinatário. Alíquota em percentual (17 = 17%). Simples Nacional usa CSOSN; regime
            normal usa CST.
          </p>
          <div className="overflow-x-auto">
            <table className="fiscal-table fiscal-table--dense erp-icms-table">
              <thead>
                <tr>
                  <th>UF</th>
                  <th>CFOP</th>
                  <th>CST</th>
                  <th>CSOSN</th>
                  <th>Alíq. %</th>
                  <th>Origem</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {(form.listaIcmsUf ?? []).map((row, idx) => (
                  <tr key={idx}>
                    <td>
                      <select
                        className="fiscal-input"
                        value={row.ufDestino}
                        onChange={(e) => setIcms(idx, { ufDestino: e.target.value })}
                      >
                        {UFS_BRASIL.map((uf) => (
                          <option key={uf} value={uf}>
                            {uf}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <input
                        className="fiscal-input fiscal-input--mono"
                        inputMode="numeric"
                        maxLength={4}
                        value={row.cfop != null && row.cfop > 0 ? String(row.cfop) : ""}
                        onChange={(e) => {
                          const d = e.target.value.replace(/\D/g, "").slice(0, 4);
                          setIcms(idx, { cfop: d ? Number(d) : undefined });
                        }}
                      />
                    </td>
                    <td>
                      <input
                        className="fiscal-input fiscal-input--mono"
                        maxLength={3}
                        value={row.cst ?? ""}
                        onChange={(e) => setIcms(idx, { cst: e.target.value.replace(/\D/g, "").slice(0, 3) })}
                      />
                    </td>
                    <td>
                      <input
                        className="fiscal-input fiscal-input--mono"
                        maxLength={3}
                        value={row.csosn ?? ""}
                        onChange={(e) => setIcms(idx, { csosn: e.target.value.replace(/\D/g, "").slice(0, 3) })}
                      />
                    </td>
                    <td>
                      <PercentInput
                        value={aliqToPct(row.aliquota)}
                        onChange={(v) => setIcms(idx, { aliquota: pctToAliq(v) ?? 0 })}
                      />
                    </td>
                    <td>
                      <select
                        className="fiscal-input"
                        value={row.origemMercadoria || "0"}
                        onChange={(e) => setIcms(idx, { origemMercadoria: e.target.value })}
                      >
                        {ORIGENS_MERCADORIA.map((o) => (
                          <option key={o.value} value={o.value}>
                            {o.label}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="fiscal-btn-icon danger"
                        title="Remover UF"
                        onClick={() => removeIcms(idx)}
                        disabled={(form.listaIcmsUf ?? []).length <= 1}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <button type="button" className="fiscal-btn-secondary mt-3" onClick={addIcms}>
            <Plus className="h-4 w-4" /> Adicionar UF
          </button>
        </FiscalSection>
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="erp-list-head">
        <div>
          <h1 className="erp-list-head__title">ICMS por operação × grupo</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Novo cruzamento
          </button>
          <button type="button" className="fiscal-btn-secondary" onClick={() => void carregarLista()}>
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
        </div>
      </div>

      <div className="erp-explain">
        <strong>Produto</strong> → grupo tributário &nbsp;·&nbsp; <strong>Emissão</strong> → operação fiscal &nbsp;·&nbsp;{" "}
        <strong>Aqui</strong> → CFOP e ICMS da UF do cliente. IBS/CBS se configura na operação fiscal.
      </div>

      {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

      <div className="fiscal-table-caption">
        <div className="erp-filters">
          <label className="erp-filter">
            <span>Operação</span>
            <select
              className="fiscal-input"
              value={filtroOp}
              onChange={(e) => {
                setFiltroOp(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Todas</option>
              {operacoes.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.descricao}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Operação, grupo, CFOP…"
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
              <th>Operação fiscal</th>
              <th>Grupo tributário</th>
              <th>CFOP (UFs)</th>
              <th>Regras</th>
              <th style={{ width: "6.5rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={5} className="text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center text-slate-500">
                  Nenhum cruzamento neste filtro
                </td>
              </tr>
            ) : (
              pagina.map((row) => {
                const op = opPorId.get(row.tributOperacaoFiscalId ?? 0);
                const gt = gtPorId.get(row.tributGrupoTributarioId ?? 0);
                const n = row.listaIcmsUf?.length ?? 0;
                return (
                  <tr key={row.id} onDoubleClick={() => void editar(row)}>
                    <td>
                      <div className="erp-prod-nome">{op?.descricao ?? `Operação #${row.tributOperacaoFiscalId}`}</div>
                      {fmtCfop(op?.cfop) && <div className="erp-prod-pdv">CFOP padrão {fmtCfop(op?.cfop)}</div>}
                    </td>
                    <td>
                      <div className="erp-prod-nome">{gt?.descricao ?? `Grupo #${row.tributGrupoTributarioId}`}</div>
                      <div className="erp-prod-pdv">{labelOrigemMercadoria(gt?.origemMercadoria)}</div>
                    </td>
                    <td className="tabular-nums">{resumoCfops(row.listaIcmsUf)}</td>
                    <td>
                      <span className="erp-pill ok">{n} UF{n === 1 ? "" : "s"}</span>
                    </td>
                    <td>
                      <div className="fiscal-table-actions">
                        <button type="button" className="fiscal-btn-icon" title="Editar" onClick={() => void editar(row)}>
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          className="fiscal-btn-icon danger"
                          title="Excluir"
                          onClick={() => void excluir(row)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
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
