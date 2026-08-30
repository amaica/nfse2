"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Ban, Pencil, Plus, RefreshCw, RotateCcw, Search, Truck } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { fiscalApi, type VeiculoDto } from "@/lib/fiscal-api";
import {
  TIPOS_CARROCERIA,
  TIPOS_RODADO,
  labelCarroceria,
  labelRodado,
  normalizarCodigoMdfe,
  normalizarPlaca,
  opcoesComValorAtual,
} from "@/lib/veiculo-mdfe";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";

const PAGE_SIZE = 20;
const ENDPOINT = "/api/veiculo";

type StatusFiltro = "todos" | "ativos" | "inativos";

const emptyVeiculo = (): VeiculoDto => ({
  placa: "",
  modelo: "",
  marca: "",
  renavam: "",
  tipoRodado: "",
  tipoCarroceria: "",
  ativo: true,
});

function blank(v?: string | null): string | undefined {
  const t = (v ?? "").trim();
  return t ? t : undefined;
}

export function CadastroVeiculosWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [veiculos, setVeiculos] = useState<VeiculoDto[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [statusFiltro, setStatusFiltro] = useState<StatusFiltro>("ativos");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<VeiculoDto>(emptyVeiculo());
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      setVeiculos(await fiscalApi.listVeiculos());
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar veículos");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyVeiculo());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const kpis = useMemo(() => {
    const total = veiculos.length;
    const ativos = veiculos.filter((v) => v.ativo).length;
    const semMdfe = veiculos.filter((v) => v.ativo && (!v.tipoRodado || !v.tipoCarroceria)).length;
    return { total, ativos, inativos: total - ativos, semMdfe };
  }, [veiculos]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    return veiculos.filter((v) => {
      if (statusFiltro === "ativos" && !v.ativo) return false;
      if (statusFiltro === "inativos" && v.ativo) return false;
      if (!q) return true;
      return (
        String(v.id ?? "").includes(q) ||
        (v.placa ?? "").toLowerCase().includes(q) ||
        (v.modelo ?? "").toLowerCase().includes(q) ||
        (v.marca ?? "").toLowerCase().includes(q) ||
        (v.renavam ?? "").includes(q)
      );
    });
  }, [veiculos, filtro, statusFiltro]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyVeiculo());
  };

  const novo = () => {
    setEditId(null);
    setForm(emptyVeiculo());
    setViewMode("form");
  };

  const editar = async (row: VeiculoDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<VeiculoDto>(ENDPOINT, row.id);
      setEditId(row.id);
      setForm({
        ...emptyVeiculo(),
        ...full,
        tipoRodado: normalizarCodigoMdfe(full.tipoRodado),
        tipoCarroceria: normalizarCodigoMdfe(full.tipoCarroceria),
      });
      setViewMode("form");
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const salvar = async () => {
    const placa = normalizarPlaca(form.placa);
    if (placa.length < 7) {
      setErro("Informe a placa com 7 caracteres (Mercosul ou antiga).");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const body: VeiculoDto = {
        ...form,
        placa,
        modelo: blank(form.modelo),
        marca: blank(form.marca),
        renavam: blank((form.renavam ?? "").replace(/\D/g, "")),
        tipoRodado: blank(normalizarCodigoMdfe(form.tipoRodado)),
        tipoCarroceria: blank(normalizarCodigoMdfe(form.tipoCarroceria)),
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

  const alternarAtivo = async (row: VeiculoDto) => {
    if (!row.id) return;
    if (
      !window.confirm(
        row.ativo
          ? `Inativar o veículo ${row.placa}? O cadastro é preservado.`
          : `Reativar o veículo ${row.placa}?`,
      )
    ) {
      return;
    }
    setErro("");
    try {
      const full = await fiscalApi.get<VeiculoDto>(ENDPOINT, row.id);
      await fiscalApi.update(ENDPOINT, row.id, { ...full, ativo: !row.ativo });
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao alterar situação");
    }
  };

  const rodados = opcoesComValorAtual(TIPOS_RODADO, form.tipoRodado);
  const carrocerias = opcoesComValorAtual(TIPOS_CARROCERIA, form.tipoCarroceria);

  if (viewMode === "form") {
    const titulo = editId ? `Veículo ${form.placa || editId}` : "Novo veículo";
    return (
      <div className="fiscal-card">
        <FiscalDetailToolbar
          title={titulo}
          icon={<Truck className="h-5 w-5 text-slate-500" />}
          onVoltar={irPesquisa}
          onNovo={novo}
          onCancelar={irPesquisa}
          onSalvar={salvar}
          saveDisabled={salvando}
        />
        {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

        <div className="erp-master">
          <FiscalField label="Placa *">
            <input
              className="fiscal-input fiscal-input--mono"
              maxLength={8}
              value={form.placa}
              autoFocus={!editId}
              onChange={(e) => setForm((f) => ({ ...f, placa: normalizarPlaca(e.target.value) }))}
              placeholder="ABC1D23"
            />
          </FiscalField>
          <FiscalField label="Modelo">
            <input
              className="fiscal-input"
              maxLength={100}
              value={form.modelo ?? ""}
              onChange={(e) => setForm((f) => ({ ...f, modelo: e.target.value }))}
            />
          </FiscalField>
          <div className="erp-master__flags">
            <label className="erp-switch">
              <input
                type="checkbox"
                checked={form.ativo}
                onChange={(e) => setForm((f) => ({ ...f, ativo: e.target.checked }))}
              />
              <span>{form.ativo ? "Ativo" : "Inativo"}</span>
            </label>
          </div>
        </div>

        <div className="fiscal-form-columns">
          <FiscalSection title="Identificação">
            <FiscalRow>
              <FiscalField label="Marca">
                <input
                  className="fiscal-input"
                  maxLength={100}
                  value={form.marca ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, marca: e.target.value }))}
                />
              </FiscalField>
              <FiscalField label="RENAVAM">
                <input
                  className="fiscal-input fiscal-input--mono"
                  inputMode="numeric"
                  maxLength={11}
                  value={form.renavam ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, renavam: e.target.value.replace(/\D/g, "").slice(0, 11) }))
                  }
                />
              </FiscalField>
            </FiscalRow>
          </FiscalSection>

          <FiscalSection title="MDFe / transporte">
            <FiscalField label="Tipo de rodado">
              <select
                className="fiscal-input"
                value={form.tipoRodado ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, tipoRodado: e.target.value }))}
              >
                <option value="">— Selecione —</option>
                {rodados.map((t) => (
                  <option key={t.codigo} value={t.codigo}>
                    {t.codigo} — {t.descricao}
                  </option>
                ))}
              </select>
            </FiscalField>
            <FiscalField label="Tipo de carroceria">
              <select
                className="fiscal-input"
                value={form.tipoCarroceria ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, tipoCarroceria: e.target.value }))}
              >
                <option value="">— Selecione —</option>
                {carrocerias.map((t) => (
                  <option key={t.codigo} value={t.codigo}>
                    {t.codigo} — {t.descricao}
                  </option>
                ))}
              </select>
            </FiscalField>
          </FiscalSection>
        </div>
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="erp-list-head">
        <div>
          <h1 className="erp-list-head__title">Veículos</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Novo veículo
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
          <div className="erp-kpi__label">Inativos</div>
          <div className="erp-kpi__value">{kpis.inativos}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Sem tipo MDFe</div>
          <div className="erp-kpi__value">{kpis.semMdfe}</div>
        </div>
      </div>

      {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

      <div className="fiscal-table-caption">
        <div className="erp-filters">
          <label className="erp-filter">
            <span>Situação</span>
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
            placeholder="Placa, marca, modelo, RENAVAM…"
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
              <th>Placa</th>
              <th>Marca</th>
              <th>Modelo</th>
              <th>RENAVAM</th>
              <th>Rodado</th>
              <th>Carroceria</th>
              <th>Situação</th>
              <th style={{ width: "6.5rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={8} className="text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={8} className="text-center text-slate-500">
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
                  <td className="whitespace-nowrap font-medium tabular-nums">{row.placa}</td>
                  <td>{row.marca || "—"}</td>
                  <td>{row.modelo || "—"}</td>
                  <td className="whitespace-nowrap tabular-nums">{row.renavam || "—"}</td>
                  <td className="whitespace-nowrap">{labelRodado(row.tipoRodado)}</td>
                  <td className="whitespace-nowrap">{labelCarroceria(row.tipoCarroceria)}</td>
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
                        className={`fiscal-btn-icon ${row.ativo ? "danger" : ""}`}
                        aria-label={row.ativo ? "Inativar" : "Reativar"}
                        title={row.ativo ? "Inativar (preserva o cadastro)" : "Reativar"}
                        onClick={() => void alternarAtivo(row)}
                      >
                        {row.ativo ? <Ban className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
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
