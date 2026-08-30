"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Pencil, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { fiscalApi, type OperacaoFiscalDto } from "@/lib/fiscal-api";
import { fmtCfop } from "@/lib/cfop";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";

const PAGE_SIZE = 20;
const ENDPOINT = "/api/tribut-operacao-fiscal";

type TabId = "geral" | "reforma";

const emptyOperacao = (): OperacaoFiscalDto => ({
  descricao: "",
  tipoOperacao: "S",
  geraFinanceiro: "S",
  movimentaEstoque: "S",
  descricaoNaNf: "",
  cfop: undefined,
  observacao: "",
  principal: "N",
  indIntermed: "0",
  ibsCbsCst: "000",
  ibsCbsClassTrib: "000001",
  aliquotaIbsUf: 0.009,
  aliquotaIbsMun: 0.001,
  aliquotaCbs: 0.01,
  habilitarIbsCbs: true,
});

function blank(v?: string | null): string | undefined {
  const t = (v ?? "").trim();
  return t ? t : undefined;
}

function labelTipo(tipo?: string | null): string {
  if (tipo === "E") return "Entrada";
  if (tipo === "S") return "Saída";
  return tipo || "—";
}

export function CadastroOperacaoFiscalWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [operacoes, setOperacoes] = useState<OperacaoFiscalDto[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<OperacaoFiscalDto>(emptyOperacao());
  const [tab, setTab] = useState<TabId>("geral");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      setOperacoes(await fiscalApi.listOperacoesFiscais());
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar operações fiscais");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyOperacao());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const kpis = useMemo(() => {
    const total = operacoes.length;
    const saida = operacoes.filter((o) => o.tipoOperacao === "S").length;
    const ibs = operacoes.filter((o) => o.habilitarIbsCbs).length;
    const semCfop = operacoes.filter((o) => !fmtCfop(o.cfop)).length;
    return { total, saida, ibs, semCfop };
  }, [operacoes]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    if (!q) return operacoes;
    return operacoes.filter(
      (o) =>
        String(o.id ?? "").includes(q) ||
        (o.descricao ?? "").toLowerCase().includes(q) ||
        fmtCfop(o.cfop).includes(q) ||
        (o.descricaoNaNf ?? "").toLowerCase().includes(q),
    );
  }, [operacoes, filtro]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyOperacao());
    setTab("geral");
  };

  const novo = () => {
    setEditId(null);
    setForm(emptyOperacao());
    setTab("geral");
    setViewMode("form");
  };

  const editar = async (row: OperacaoFiscalDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<OperacaoFiscalDto>(ENDPOINT, row.id);
      setEditId(row.id);
      setForm({ ...emptyOperacao(), ...full });
      setTab("geral");
      setViewMode("form");
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const salvar = async () => {
    if (!form.descricao?.trim()) {
      setErro("Informe a descrição da operação.");
      setTab("geral");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const cfopNum = form.cfop != null && form.cfop >= 1000 ? Number(form.cfop) : undefined;
      const body: OperacaoFiscalDto = {
        ...form,
        descricao: form.descricao.trim(),
        descricaoNaNf: blank(form.descricaoNaNf),
        observacao: blank(form.observacao),
        cfop: cfopNum,
        cMunFGIBS: blank(form.cMunFGIBS),
        tpNFDebito: blank(form.tpNFDebito),
        tpNFCredito: blank(form.tpNFCredito),
        tpEnteGov: blank(form.tpEnteGov),
        tpOperGov: blank(form.tpOperGov),
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

  const excluir = async (row: OperacaoFiscalDto) => {
    if (!row.id) return;
    if (!window.confirm(`Excluir a operação "${row.descricao}"?`)) return;
    setErro("");
    try {
      await fiscalApi.remove(ENDPOINT, row.id);
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao excluir");
    }
  };

  if (viewMode === "form") {
    const titulo = editId ? `Operação ${form.descricao || editId}` : "Nova operação fiscal";
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

        <div className="erp-master">
          <FiscalField label="Descrição *" className="erp-span-2">
            <input
              className="fiscal-input"
              maxLength={255}
              value={form.descricao}
              autoFocus={!editId}
              onChange={(e) => setForm((f) => ({ ...f, descricao: e.target.value }))}
            />
          </FiscalField>
          <FiscalField label="Tipo">
            <select
              className="fiscal-input"
              value={form.tipoOperacao || "S"}
              onChange={(e) => setForm((f) => ({ ...f, tipoOperacao: e.target.value }))}
            >
              <option value="S">Saída (venda, remessa…)</option>
              <option value="E">Entrada (compra, retorno…)</option>
            </select>
          </FiscalField>
        </div>

        <div className="erp-tabs" role="tablist">
          <button type="button" className={`erp-tab ${tab === "geral" ? "active" : ""}`} onClick={() => setTab("geral")}>
            Identificação
          </button>
          <button
            type="button"
            className={`erp-tab ${tab === "reforma" ? "active" : ""}`}
            onClick={() => setTab("reforma")}
          >
            Reforma IBS / CBS
          </button>
        </div>

        {tab === "geral" && (
          <FiscalSection title="Dados da operação">
            <FiscalRow>
              <FiscalField label="CFOP padrão">
                <input
                  className="fiscal-input fiscal-input--mono"
                  inputMode="numeric"
                  maxLength={4}
                  value={form.cfop != null && form.cfop > 0 ? String(form.cfop) : ""}
                  onChange={(e) => {
                    const d = e.target.value.replace(/\D/g, "").slice(0, 4);
                    setForm((f) => ({ ...f, cfop: d ? Number(d) : undefined }));
                  }}
                  placeholder="Ex.: 5102"
                />
                <span className="erp-hint">Usado na NF-e quando não houver regra ICMS da UF de destino.</span>
              </FiscalField>
              <FiscalField label="Natureza na NF-e">
                <input
                  className="fiscal-input"
                  maxLength={255}
                  value={form.descricaoNaNf ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, descricaoNaNf: e.target.value }))}
                  placeholder="Texto da natureza da operação"
                />
              </FiscalField>
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="Gera financeiro">
                <select
                  className="fiscal-input"
                  value={form.geraFinanceiro || "S"}
                  onChange={(e) => setForm((f) => ({ ...f, geraFinanceiro: e.target.value }))}
                >
                  <option value="S">Sim</option>
                  <option value="N">Não</option>
                </select>
              </FiscalField>
              <FiscalField label="Movimenta estoque">
                <select
                  className="fiscal-input"
                  value={form.movimentaEstoque || "S"}
                  onChange={(e) => setForm((f) => ({ ...f, movimentaEstoque: e.target.value }))}
                >
                  <option value="S">Sim</option>
                  <option value="N">Não</option>
                </select>
              </FiscalField>
              <FiscalField label="Operação principal">
                <select
                  className="fiscal-input"
                  value={form.principal || "N"}
                  onChange={(e) => setForm((f) => ({ ...f, principal: e.target.value }))}
                >
                  <option value="N">Não</option>
                  <option value="S">Sim</option>
                </select>
              </FiscalField>
            </FiscalRow>
            <FiscalField label="Observação interna">
              <textarea
                className="fiscal-input"
                rows={3}
                maxLength={1000}
                value={form.observacao ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, observacao: e.target.value }))}
              />
            </FiscalField>
          </FiscalSection>
        )}

        {tab === "reforma" && (
          <FiscalSection title="IBS e CBS (Reforma Tributária)">
            <label className="erp-switch" style={{ marginBottom: "0.75rem" }}>
              <input
                type="checkbox"
                checked={form.habilitarIbsCbs}
                onChange={(e) => setForm((f) => ({ ...f, habilitarIbsCbs: e.target.checked }))}
              />
              <span>Incluir IBS/CBS nesta operação da NF-e</span>
            </label>
            <FiscalRow>
              <FiscalField label="CST IBS/CBS">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={3}
                  value={form.ibsCbsCst ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, ibsCbsCst: e.target.value.replace(/\D/g, "").slice(0, 3) }))}
                />
              </FiscalField>
              <FiscalField label="Classificação tributária (cClassTrib)">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={6}
                  value={form.ibsCbsClassTrib ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, ibsCbsClassTrib: e.target.value.replace(/\D/g, "").slice(0, 6) }))
                  }
                />
              </FiscalField>
              <FiscalField label="Intermediador">
                <select
                  className="fiscal-input"
                  value={form.indIntermed || "0"}
                  onChange={(e) => setForm((f) => ({ ...f, indIntermed: e.target.value }))}
                >
                  <option value="0">Sem intermediador</option>
                  <option value="1">Com intermediador (marketplace)</option>
                </select>
              </FiscalField>
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="Alíquota IBS estado">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaIbsUf ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, aliquotaIbsUf: e.target.value === "" ? undefined : Number(e.target.value) }))
                  }
                />
              </FiscalField>
              <FiscalField label="Alíquota IBS município">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaIbsMun ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      aliquotaIbsMun: e.target.value === "" ? undefined : Number(e.target.value),
                    }))
                  }
                />
              </FiscalField>
              <FiscalField label="Alíquota CBS">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaCbs ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, aliquotaCbs: e.target.value === "" ? undefined : Number(e.target.value) }))
                  }
                />
              </FiscalField>
            </FiscalRow>
            <p className="erp-hint">
              Alíquotas no padrão da NF-e (ex.: 0,0090 = 0,90%). Obrigatório nos DF-e a partir de 03/08/2026.
            </p>
            <FiscalRow>
              <FiscalField label="Município gerador do IBS (IBGE)">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={7}
                  value={form.cMunFGIBS ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, cMunFGIBS: e.target.value.replace(/\D/g, "").slice(0, 7) }))
                  }
                />
              </FiscalField>
              <FiscalField label="Ente governamental">
                <select
                  className="fiscal-input"
                  value={form.tpEnteGov ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, tpEnteGov: e.target.value }))}
                >
                  <option value="">— Não se aplica —</option>
                  <option value="1">União</option>
                  <option value="2">Estado</option>
                  <option value="3">Município</option>
                </select>
              </FiscalField>
            </FiscalRow>
          </FiscalSection>
        )}
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="erp-list-head">
        <div>
          <h1 className="erp-list-head__title">Operações fiscais</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
          <p className="erp-list-head__sub">Tipo da nota na emissão (venda, depósito, devolução…). IBS/CBS ficam aqui.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Nova operação
          </button>
          <button type="button" className="fiscal-btn-secondary" onClick={() => void carregarLista()}>
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
        </div>
      </div>

      <div className="erp-kpis">
        <div className="erp-kpi">
          <div className="erp-kpi__label">Cadastradas</div>
          <div className="erp-kpi__value">{kpis.total}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Saída</div>
          <div className="erp-kpi__value">{kpis.saida}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Com IBS/CBS</div>
          <div className="erp-kpi__value">{kpis.ibs}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Sem CFOP</div>
          <div className="erp-kpi__value">{kpis.semCfop}</div>
        </div>
      </div>

      {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

      <div className="fiscal-table-caption">
        <span>Filtro na grade</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Descrição, CFOP…"
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
              <th>Tipo</th>
              <th>CFOP</th>
              <th>IBS/CBS</th>
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
                  Nenhum registro neste filtro
                </td>
              </tr>
            ) : (
              pagina.map((row) => (
                <tr key={row.id} onDoubleClick={() => void editar(row)}>
                  <td className="font-medium">{row.descricao}</td>
                  <td>{labelTipo(row.tipoOperacao)}</td>
                  <td className="tabular-nums">{fmtCfop(row.cfop) || "—"}</td>
                  <td>
                    <span className={`erp-pill ${row.habilitarIbsCbs ? "ok" : "off"}`}>
                      {row.habilitarIbsCbs ? "Sim" : "Não"}
                    </span>
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
