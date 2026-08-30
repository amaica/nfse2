"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Pencil, Plus, Receipt, RefreshCw, Search, Trash2 } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { fiscalApi, type TributNfseServicoDto } from "@/lib/fiscal-api";
import {
  CST_PIS_COFINS,
  ISS_RETIDO,
  REGIME_ESPECIAL,
  SIMPLES_NACIONAL,
  TRIBUTACAO_ISSQN,
  fmtAliquota,
  labelOpcao,
} from "@/lib/nfse-servico-opcoes";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";

const PAGE_SIZE = 20;
const ENDPOINT = "/api/tribut-nfse-servico";

type TabId = "ident" | "iss" | "federal" | "reforma";
type StatusFiltro = "todos" | "ativos" | "inativos";

const emptyServico = (): TributNfseServicoDto => ({
  descricao: "",
  itemListaServico: "",
  codigoTributacaoMunicipio: "",
  nbs: "",
  cnae: "",
  descricaoServico: "",
  municipioPrestacaoIbge: "",
  aliquotaIss: 2,
  tributacaoIssqn: "1",
  issRetido: "1",
  simplesNacional: "1",
  regimeEspecial: "0",
  cstPisCofins: "08",
  aliquotaPis: undefined,
  aliquotaCofins: undefined,
  habilitarRetencoes: false,
  ibsCbsCst: "000",
  ibsCbsClassTrib: "000001",
  aliquotaIbs: 0.01,
  aliquotaCbs: 0.01,
  habilitarIbsCbs: true,
  principal: false,
  ativo: true,
});

function blank(v?: string | null): string | undefined {
  const t = (v ?? "").trim();
  return t ? t : undefined;
}

function numOrUndef(v: string): number | undefined {
  if (v.trim() === "") return undefined;
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
}

export function CadastroNfseServicoWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [servicos, setServicos] = useState<TributNfseServicoDto[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [statusFiltro, setStatusFiltro] = useState<StatusFiltro>("ativos");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<TributNfseServicoDto>(emptyServico());
  const [tab, setTab] = useState<TabId>("ident");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      setServicos(await fiscalApi.listTributNfseServicos(true));
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar tributação NFS-e");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyServico());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const kpis = useMemo(() => {
    const total = servicos.length;
    const ativos = servicos.filter((s) => s.ativo).length;
    const principais = servicos.filter((s) => s.principal && s.ativo).length;
    const semLc = servicos.filter((s) => s.ativo && !(s.itemListaServico ?? "").trim()).length;
    return { total, ativos, principais, semLc };
  }, [servicos]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    return servicos.filter((s) => {
      if (statusFiltro === "ativos" && !s.ativo) return false;
      if (statusFiltro === "inativos" && s.ativo) return false;
      if (!q) return true;
      const trib = labelOpcao(TRIBUTACAO_ISSQN, s.tributacaoIssqn).toLowerCase();
      const principalLabel = s.principal ? "sim principal" : "nao não";
      return (
        String(s.id ?? "").includes(q) ||
        (s.descricao ?? "").toLowerCase().includes(q) ||
        (s.itemListaServico ?? "").toLowerCase().includes(q) ||
        (s.descricaoServico ?? "").toLowerCase().includes(q) ||
        (s.cnae ?? "").includes(q) ||
        (s.nbs ?? "").toLowerCase().includes(q) ||
        trib.includes(q) ||
        principalLabel.includes(q)
      );
    });
  }, [servicos, filtro, statusFiltro]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyServico());
    setTab("ident");
  };

  const novo = () => {
    setEditId(null);
    setForm(emptyServico());
    setTab("ident");
    setViewMode("form");
  };

  const editar = async (row: TributNfseServicoDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<TributNfseServicoDto>(ENDPOINT, row.id);
      setEditId(row.id);
      setForm({ ...emptyServico(), ...full });
      setTab("ident");
      setViewMode("form");
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const salvar = async () => {
    if (!form.descricao?.trim() || !form.itemListaServico?.trim()) {
      setErro("Informe a descrição e o item da LC 116.");
      setTab("ident");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const body: TributNfseServicoDto = {
        ...form,
        descricao: form.descricao.trim(),
        itemListaServico: form.itemListaServico.trim(),
        codigoTributacaoMunicipio: blank(form.codigoTributacaoMunicipio),
        nbs: blank(form.nbs),
        cnae: blank(form.cnae)?.replace(/\D/g, "").slice(0, 7),
        descricaoServico: blank(form.descricaoServico),
        municipioPrestacaoIbge: blank(form.municipioPrestacaoIbge)?.replace(/\D/g, "").slice(0, 7),
        tributacaoIssqn: form.tributacaoIssqn || "1",
        issRetido: form.issRetido || "1",
        simplesNacional: form.simplesNacional || "1",
        regimeEspecial: form.regimeEspecial || "0",
        cstPisCofins: form.cstPisCofins || "08",
        ibsCbsCst: (form.ibsCbsCst || "000").replace(/\D/g, "").slice(0, 3),
        ibsCbsClassTrib: (form.ibsCbsClassTrib || "000001").replace(/\D/g, "").slice(0, 6),
        principal: Boolean(form.principal),
        ativo: form.ativo !== false,
        habilitarIbsCbs: form.habilitarIbsCbs !== false,
        habilitarRetencoes: Boolean(form.habilitarRetencoes),
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

  const excluir = async (row: TributNfseServicoDto) => {
    if (!row.id) return;
    if (!window.confirm(`Excluir o cadastro "${row.descricao}"?`)) return;
    setErro("");
    try {
      await fiscalApi.remove(ENDPOINT, row.id);
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao excluir");
    }
  };

  if (viewMode === "form") {
    const titulo = editId ? `Serviço ${form.descricao || editId}` : "Nova tributação NFS-e";
    return (
      <div className="fiscal-card">
        <FiscalDetailToolbar
          title={titulo}
          icon={<Receipt className="h-5 w-5 text-slate-500" />}
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
              placeholder="Nome interno deste cadastro de serviço"
            />
          </FiscalField>
          <div className="erp-master__flags">
            <label className="erp-switch">
              <input
                type="checkbox"
                checked={form.ativo}
                onChange={(e) => setForm((f) => ({ ...f, ativo: e.target.checked }))}
              />
              <span>Ativo</span>
            </label>
          </div>
        </div>

        <div className="erp-tabs" role="tablist">
          <button type="button" className={`erp-tab ${tab === "ident" ? "active" : ""}`} onClick={() => setTab("ident")}>
            Identificação
          </button>
          <button type="button" className={`erp-tab ${tab === "iss" ? "active" : ""}`} onClick={() => setTab("iss")}>
            ISS
          </button>
          <button
            type="button"
            className={`erp-tab ${tab === "federal" ? "active" : ""}`}
            onClick={() => setTab("federal")}
          >
            PIS / COFINS
          </button>
          <button
            type="button"
            className={`erp-tab ${tab === "reforma" ? "active" : ""}`}
            onClick={() => setTab("reforma")}
          >
            Reforma IBS / CBS
          </button>
        </div>

        {tab === "ident" && (
          <FiscalSection title="Classificação do serviço">
            <FiscalRow>
              <FiscalField label="Item da lista (LC 116) *">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={15}
                  value={form.itemListaServico}
                  onChange={(e) => setForm((f) => ({ ...f, itemListaServico: e.target.value }))}
                  placeholder="Ex.: 07.16.01.000"
                />
              </FiscalField>
              <FiscalField label="Cód. tributação municipal">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={20}
                  value={form.codigoTributacaoMunicipio ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, codigoTributacaoMunicipio: e.target.value }))}
                />
              </FiscalField>
              <FiscalField label="Município da prestação (IBGE)">
                <input
                  className="fiscal-input fiscal-input--mono"
                  inputMode="numeric"
                  maxLength={7}
                  value={form.municipioPrestacaoIbge ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      municipioPrestacaoIbge: e.target.value.replace(/\D/g, "").slice(0, 7),
                    }))
                  }
                />
              </FiscalField>
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="CNAE">
                <input
                  className="fiscal-input fiscal-input--mono"
                  inputMode="numeric"
                  maxLength={7}
                  value={form.cnae ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, cnae: e.target.value.replace(/\D/g, "").slice(0, 7) }))}
                />
              </FiscalField>
              <FiscalField label="NBS">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={9}
                  value={form.nbs ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, nbs: e.target.value }))}
                />
              </FiscalField>
              <FiscalField label="Cadastro principal">
                <select
                  className="fiscal-input"
                  value={form.principal ? "S" : "N"}
                  onChange={(e) => setForm((f) => ({ ...f, principal: e.target.value === "S" }))}
                >
                  <option value="N">Não</option>
                  <option value="S">Sim — usar como padrão na emissão</option>
                </select>
              </FiscalField>
            </FiscalRow>
            <FiscalField label="Descrição padrão do serviço na NFS-e">
              <textarea
                className="fiscal-input fiscal-textarea"
                rows={4}
                maxLength={2000}
                value={form.descricaoServico ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, descricaoServico: e.target.value }))}
                placeholder="Texto que entra na nota quando este cadastro for selecionado"
              />
            </FiscalField>
          </FiscalSection>
        )}

        {tab === "iss" && (
          <FiscalSection title="ISS e enquadramento">
            <FiscalRow>
              <FiscalField label="Alíquota ISS">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaIss ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, aliquotaIss: numOrUndef(e.target.value) }))}
                />
                <span className="erp-hint">Percentual municipal (ex.: 2 = 2%).</span>
              </FiscalField>
              <FiscalField label="Tributação ISSQN">
                <select
                  className="fiscal-input"
                  value={form.tributacaoIssqn || "1"}
                  onChange={(e) => setForm((f) => ({ ...f, tributacaoIssqn: e.target.value }))}
                >
                  {TRIBUTACAO_ISSQN.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </FiscalField>
              <FiscalField label="ISS retido">
                <select
                  className="fiscal-input"
                  value={form.issRetido || "1"}
                  onChange={(e) => setForm((f) => ({ ...f, issRetido: e.target.value }))}
                >
                  {ISS_RETIDO.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </FiscalField>
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="Simples Nacional">
                <select
                  className="fiscal-input"
                  value={form.simplesNacional || "1"}
                  onChange={(e) => setForm((f) => ({ ...f, simplesNacional: e.target.value }))}
                >
                  {SIMPLES_NACIONAL.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </FiscalField>
              <FiscalField label="Regime especial">
                <select
                  className="fiscal-input"
                  value={form.regimeEspecial || "0"}
                  onChange={(e) => setForm((f) => ({ ...f, regimeEspecial: e.target.value }))}
                >
                  {REGIME_ESPECIAL.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </FiscalField>
            </FiscalRow>
          </FiscalSection>
        )}

        {tab === "federal" && (
          <FiscalSection title="PIS, COFINS e retenções">
            <FiscalRow>
              <FiscalField label="CST PIS/COFINS">
                <select
                  className="fiscal-input"
                  value={form.cstPisCofins || "08"}
                  onChange={(e) => setForm((f) => ({ ...f, cstPisCofins: e.target.value }))}
                >
                  {CST_PIS_COFINS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </FiscalField>
              <FiscalField label="Alíquota PIS">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaPis ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, aliquotaPis: numOrUndef(e.target.value) }))}
                />
              </FiscalField>
              <FiscalField label="Alíquota COFINS">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaCofins ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, aliquotaCofins: numOrUndef(e.target.value) }))}
                />
              </FiscalField>
            </FiscalRow>
            <label className="erp-switch" style={{ marginBottom: "0.75rem" }}>
              <input
                type="checkbox"
                checked={Boolean(form.habilitarRetencoes)}
                onChange={(e) => setForm((f) => ({ ...f, habilitarRetencoes: e.target.checked }))}
              />
              <span>Habilitar retenções federais neste cadastro</span>
            </label>
            {form.habilitarRetencoes && (
              <FiscalRow>
                <FiscalField label="Retenção INSS (R$)">
                  <input
                    className="fiscal-input"
                    type="number"
                    step="0.01"
                    value={form.retencaoInss ?? ""}
                    onChange={(e) => setForm((f) => ({ ...f, retencaoInss: numOrUndef(e.target.value) }))}
                  />
                </FiscalField>
                <FiscalField label="Retenção IRRF (R$)">
                  <input
                    className="fiscal-input"
                    type="number"
                    step="0.01"
                    value={form.retencaoIrrf ?? ""}
                    onChange={(e) => setForm((f) => ({ ...f, retencaoIrrf: numOrUndef(e.target.value) }))}
                  />
                </FiscalField>
                <FiscalField label="Retenção CSLL (R$)">
                  <input
                    className="fiscal-input"
                    type="number"
                    step="0.01"
                    value={form.retencaoCsll ?? ""}
                    onChange={(e) => setForm((f) => ({ ...f, retencaoCsll: numOrUndef(e.target.value) }))}
                  />
                </FiscalField>
              </FiscalRow>
            )}
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
              <span>Incluir IBS/CBS nesta NFS-e</span>
            </label>
            <FiscalRow>
              <FiscalField label="CST IBS/CBS">
                <input
                  className="fiscal-input fiscal-input--mono"
                  maxLength={3}
                  value={form.ibsCbsCst ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, ibsCbsCst: e.target.value.replace(/\D/g, "").slice(0, 3) }))
                  }
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
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="Alíquota IBS">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaIbs ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, aliquotaIbs: numOrUndef(e.target.value) }))}
                />
              </FiscalField>
              <FiscalField label="Alíquota CBS">
                <input
                  className="fiscal-input"
                  type="number"
                  step="0.0001"
                  value={form.aliquotaCbs ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, aliquotaCbs: numOrUndef(e.target.value) }))}
                />
              </FiscalField>
            </FiscalRow>
            <p className="erp-hint">Alíquotas no padrão da NFS-e nacional (ex.: 0,0100 = 1%).</p>
          </FiscalSection>
        )}
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="erp-list-head">
        <div>
          <h1 className="erp-list-head__title">Tributação NFS-e</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
          <p className="erp-list-head__sub">Cadastro de serviço para emissão da NFS-e (LC 116, ISS e IBS/CBS).</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Novo serviço
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
          <div className="erp-kpi__label">Ativos</div>
          <div className="erp-kpi__value">{kpis.ativos}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Principal</div>
          <div className="erp-kpi__value">{kpis.principais}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Sem LC 116</div>
          <div className="erp-kpi__value">{kpis.semLc}</div>
        </div>
      </div>

      {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

      <div className="fiscal-table-caption">
        <div className="flex flex-wrap items-center gap-3">
          <span>Filtro na grade</span>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            Situação
            <select
              className="fiscal-input"
              value={statusFiltro}
              onChange={(e) => {
                setStatusFiltro(e.target.value as StatusFiltro);
                setPage(0);
              }}
            >
              <option value="ativos">Somente ativos</option>
              <option value="inativos">Somente inativos</option>
              <option value="todos">Todos</option>
            </select>
          </label>
        </div>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Descrição, LC 116, tributação…"
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
              <th>LC 116</th>
              <th className="text-right">ISS</th>
              <th>Tributação</th>
              <th>Principal</th>
              <th>Situação</th>
              <th style={{ width: "6.5rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={7} className="text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={7} className="text-center text-slate-500">
                  Nenhum registro neste filtro
                </td>
              </tr>
            ) : (
              pagina.map((row) => (
                <tr
                  key={row.id}
                  className={row.ativo ? undefined : "is-inactive"}
                  onDoubleClick={() => void editar(row)}
                >
                  <td>
                    <div className="erp-prod-nome">{row.descricao}</div>
                    {row.descricaoServico && row.descricaoServico !== row.descricao && (
                      <div className="erp-prod-pdv">{row.descricaoServico}</div>
                    )}
                  </td>
                  <td className="whitespace-nowrap tabular-nums">{row.itemListaServico || "—"}</td>
                  <td className="text-right whitespace-nowrap tabular-nums">{fmtAliquota(row.aliquotaIss)}</td>
                  <td>{labelOpcao(TRIBUTACAO_ISSQN, row.tributacaoIssqn)}</td>
                  <td>
                    <span className={`erp-pill ${row.principal ? "ok" : "off"}`}>
                      {row.principal ? "Sim" : "Não"}
                    </span>
                  </td>
                  <td>
                    <span className={`erp-pill ${row.ativo ? "ok" : "off"}`}>
                      {row.ativo ? "Ativo" : "Inativo"}
                    </span>
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
