"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Ban,
  Copy,
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
} from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { fiscalApi, type ProdutoDto } from "@/lib/fiscal-api";
import { UNIDADES_PRODUTO, labelUnidade } from "@/lib/produto-unidades";
import { ORIGENS_MERCADORIA } from "@/lib/origem-mercadoria";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";
import {
  MoedaInput,
  PercentInput,
  QtyInput,
  calcMarkup,
  calcPrecoVenda,
  fmtMoeda,
} from "@/components/fiscal/MoedaInput";

const PAGE_SIZE = 20;
const ENDPOINT = "/api/produto";

type TabId = "ident" | "fiscal" | "preco" | "estoque";
type StatusFiltro = "todos" | "ativos" | "inativos";
type NcmOpt = { codigo: string; descricao: string };
type CestOpt = { codigo: string; descricao: string };

const emptyProduto = (): ProdutoDto => ({
  codigo: "",
  nome: "",
  descricaoPdv: "",
  gtin: "",
  codigoNcm: "",
  cest: "",
  exTipi: "",
  unidade: "UN",
  origem: "0",
  tipo: "P",
  valorUnitario: undefined,
  valorCusto: undefined,
  markup: undefined,
  peso: undefined,
  estoqueMinimo: undefined,
  estoqueAtual: undefined,
  observacoes: "",
  grupoTributarioId: undefined,
  grupoId: undefined,
  subgrupoId: undefined,
  ativo: true,
});

function blank(v?: string | null): string | undefined {
  const t = (v ?? "").trim();
  return t ? t : undefined;
}

function digits(v?: string | null, max?: number): string | undefined {
  const d = (v ?? "").replace(/\D/g, "");
  if (!d) return undefined;
  return max ? d.slice(0, max) : d;
}

export function CadastroProdutosWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [produtos, setProdutos] = useState<ProdutoDto[]>([]);
  const [gruposTrib, setGruposTrib] = useState<Array<{ id: number; descricao: string }>>([]);
  const [grupos, setGrupos] = useState<Array<{ id: number; nome: string }>>([]);
  const [subgrupos, setSubgrupos] = useState<Array<{ id: number; produtoGrupoId: number; nome: string }>>([]);
  const [subgruposTodos, setSubgruposTodos] = useState<Array<{ id: number; produtoGrupoId: number; nome: string }>>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [statusFiltro, setStatusFiltro] = useState<StatusFiltro>("ativos");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<ProdutoDto>(emptyProduto());
  const [tab, setTab] = useState<TabId>("ident");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [ncmBusca, setNcmBusca] = useState("");
  const [ncmOpcoes, setNcmOpcoes] = useState<NcmOpt[]>([]);
  const [ncmLabel, setNcmLabel] = useState("");
  const [ncmAberto, setNcmAberto] = useState(false);
  const [cestBusca, setCestBusca] = useState("");
  const [cestOpcoes, setCestOpcoes] = useState<CestOpt[]>([]);
  const [cestLabel, setCestLabel] = useState("");
  const [cestAberto, setCestAberto] = useState(false);
  const [precoManual, setPrecoManual] = useState(false);

  const grupoPorId = useMemo(() => {
    const m = new Map<number, string>();
    for (const g of grupos) m.set(g.id, g.nome);
    return m;
  }, [grupos]);

  const subgrupoPorId = useMemo(() => {
    const m = new Map<number, string>();
    for (const s of subgruposTodos) m.set(s.id, s.nome);
    return m;
  }, [subgruposTodos]);

  const tribPorId = useMemo(() => {
    const m = new Map<number, string>();
    for (const g of gruposTrib) m.set(g.id, g.descricao);
    return m;
  }, [gruposTrib]);

  const unidadesSelect = useMemo(() => {
    const atual = form.unidade;
    if (atual && !UNIDADES_PRODUTO.some((u) => u.sigla === atual)) {
      return [{ sigla: atual, descricao: "Unidade do cadastro" }, ...UNIDADES_PRODUTO];
    }
    return UNIDADES_PRODUTO;
  }, [form.unidade]);

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      const [lista, trib, g, s] = await Promise.all([
        fiscalApi.listProdutos(),
        fiscalApi.gruposTributarios(),
        fiscalApi.produtoGrupos(),
        fiscalApi.produtoSubgrupos(),
      ]);
      setProdutos(lista);
      setGruposTrib(trib);
      setGrupos(g);
      setSubgruposTodos(s);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar produtos");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyProduto());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  useEffect(() => {
    const q = ncmBusca.replace(/\D/g, "");
    if (q.length < 2) {
      setNcmOpcoes([]);
      return;
    }
    const t = window.setTimeout(() => {
      void fiscalApi
        .buscaNcm(q)
        .then((rows) => {
          const opts = rows.map((r) => ({ codigo: r.codigo, descricao: r.descricao }));
          setNcmOpcoes(opts);
          const hit = opts.find((n) => n.codigo === q);
          if (hit) setNcmLabel(hit.descricao);
        })
        .catch(() => setNcmOpcoes([]));
    }, 280);
    return () => window.clearTimeout(t);
  }, [ncmBusca]);

  useEffect(() => {
    if (!form.grupoId) {
      setSubgrupos([]);
      return;
    }
    void fiscalApi
      .produtoSubgrupos(form.grupoId)
      .then(setSubgrupos)
      .catch(() => setSubgrupos([]));
  }, [form.grupoId]);

  useEffect(() => {
    const ncm = (form.codigoNcm ?? "").replace(/\D/g, "");
    const t = window.setTimeout(() => {
      void fiscalApi
        .buscaCest(cestBusca, ncm)
        .then((rows) => {
          setCestOpcoes(rows);
          const hit = rows.find((c) => c.codigo === (form.cest ?? "").replace(/\D/g, ""));
          if (hit) setCestLabel(hit.descricao);
        })
        .catch(() => setCestOpcoes([]));
    }, 220);
    return () => window.clearTimeout(t);
  }, [cestBusca, form.codigoNcm, form.cest]);

  const kpis = useMemo(() => {
    const total = produtos.length;
    const ativos = produtos.filter((p) => p.ativo).length;
    const semNcm = produtos.filter((p) => p.ativo && !(p.codigoNcm ?? "").trim()).length;
    const semGrupo = produtos.filter((p) => p.ativo && !p.grupoTributarioId).length;
    return { total, ativos, inativos: total - ativos, semNcm, semGrupo };
  }, [produtos]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    return produtos.filter((p) => {
      if (statusFiltro === "ativos" && !p.ativo) return false;
      if (statusFiltro === "inativos" && p.ativo) return false;
      if (!q) return true;
      return (
        String(p.id ?? "").includes(q) ||
        (p.codigo ?? "").toLowerCase().includes(q) ||
        (p.nome ?? "").toLowerCase().includes(q) ||
        (p.descricaoPdv ?? "").toLowerCase().includes(q) ||
        (p.gtin ?? "").includes(q) ||
        (p.codigoNcm ?? "").includes(q) ||
        (p.unidade ?? "").toLowerCase().includes(q) ||
        (p.grupoId ? grupoPorId.get(p.grupoId) ?? "" : "").toLowerCase().includes(q) ||
        (p.subgrupoId ? subgrupoPorId.get(p.subgrupoId) ?? "" : "").toLowerCase().includes(q)
      );
    });
  }, [produtos, filtro, statusFiltro, grupoPorId, subgrupoPorId]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const resetFormState = (next: ProdutoDto, id: number | null) => {
    setEditId(id);
    setForm(next);
    setNcmBusca(next.codigoNcm ?? "");
    setNcmLabel("");
    setNcmAberto(false);
    setCestBusca(next.cest ?? "");
    setCestLabel("");
    setCestAberto(false);
    setPrecoManual(Boolean(next.valorUnitario));
    setTab("ident");
    setViewMode("form");
  };

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyProduto());
    setNcmBusca("");
    setNcmLabel("");
    setNcmAberto(false);
    setCestBusca("");
    setCestLabel("");
    setCestAberto(false);
    setPrecoManual(false);
    setTab("ident");
  };

  const novo = () => resetFormState(emptyProduto(), null);

  const editar = async (row: ProdutoDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<ProdutoDto>(ENDPOINT, row.id);
      resetFormState({ ...emptyProduto(), ...full }, row.id);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const duplicar = async (row: ProdutoDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<ProdutoDto>(ENDPOINT, row.id);
      const codigo = (full.codigo || "").slice(0, 52);
      resetFormState(
        {
          ...emptyProduto(),
          ...full,
          id: undefined,
          codigo: codigo ? `${codigo}-C` : "",
          ativo: true,
        },
        null,
      );
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao duplicar");
    }
  };

  function aplicarCustoMarkup(custo: number | undefined, markup: number | undefined) {
    setForm((f) => {
      const next = { ...f, valorCusto: custo, markup };
      if (!precoManual && custo != null && custo > 0) {
        next.valorUnitario = calcPrecoVenda(custo, markup);
      }
      return next;
    });
  }

  function aplicarPrecoVenda(venda: number | undefined) {
    setPrecoManual(true);
    setForm((f) => {
      const next = { ...f, valorUnitario: venda };
      if (f.valorCusto != null && f.valorCusto > 0 && venda != null) {
        next.markup = calcMarkup(f.valorCusto, venda);
      }
      return next;
    });
  }

  const salvar = async () => {
    if (!form.codigo?.trim() || !form.nome?.trim()) {
      setErro("Informe código e descrição comercial.");
      setTab("ident");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      const body: ProdutoDto = {
        ...form,
        codigo: form.codigo.trim(),
        nome: form.nome.trim(),
        descricaoPdv: blank(form.descricaoPdv) ?? form.nome.trim().slice(0, 120),
        gtin: digits(form.gtin, 14),
        codigoNcm: digits(form.codigoNcm, 8),
        cest: digits(form.cest, 7),
        exTipi: digits(form.exTipi, 3),
        unidade: form.unidade || "UN",
        origem: form.origem || "0",
        tipo: form.tipo || "P",
        observacoes: blank(form.observacoes),
        grupoTributarioId: form.grupoTributarioId || undefined,
        grupoId: form.grupoId || undefined,
        subgrupoId: form.subgrupoId || undefined,
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

  const alternarAtivo = async (row: ProdutoDto) => {
    if (!row.id) return;
    const acao = row.ativo ? "inativar" : "reativar";
    if (
      !window.confirm(
        row.ativo
          ? `Inativar "${row.nome}"? O cadastro é preservado e deixa de aparecer na emissão.`
          : `Reativar "${row.nome}"?`,
      )
    ) {
      return;
    }
    setErro("");
    try {
      const full = await fiscalApi.get<ProdutoDto>(ENDPOINT, row.id);
      await fiscalApi.update(ENDPOINT, row.id, { ...full, ativo: !row.ativo });
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : `Erro ao ${acao}`);
    }
  };

  if (viewMode === "form") {
    const precoCalc =
      form.valorCusto != null && form.valorCusto > 0
        ? calcPrecoVenda(form.valorCusto, form.markup)
        : undefined;
    const titulo = editId ? `Produto ${form.codigo || editId}` : "Novo produto";

    return (
      <div className="fiscal-card">
        <FiscalDetailToolbar
          title={titulo}
          icon="box"
          onVoltar={irPesquisa}
          onNovo={novo}
          onCancelar={irPesquisa}
          onSalvar={salvar}
          saveDisabled={salvando}
        />
        {erro && <p className="erp-alert erp-alert--error">{erro}</p>}

        <div className="erp-master">
          <FiscalField label="Código *">
            <input
              className="fiscal-input fiscal-input--mono"
              maxLength={60}
              value={form.codigo}
              onChange={(e) => setForm((f) => ({ ...f, codigo: e.target.value }))}
              autoFocus={!editId}
            />
          </FiscalField>
          <FiscalField label="Descrição comercial *">
            <input
              className="fiscal-input"
              maxLength={255}
              value={form.nome}
              onChange={(e) => {
                const nome = e.target.value;
                setForm((f) => ({
                  ...f,
                  nome,
                  descricaoPdv:
                    !f.descricaoPdv || f.descricaoPdv === f.nome ? nome.slice(0, 120) : f.descricaoPdv,
                }));
              }}
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
            <span className={`erp-pill ${form.tipo === "S" ? "warn" : "ok"}`}>
              {form.tipo === "S" ? "Serviço" : "Produto"}
            </span>
          </div>
        </div>

        <div className="erp-tabs" role="tablist">
          {(
            [
              ["ident", "Identificação"],
              ["fiscal", "Fiscal"],
              ["preco", "Precificação"],
              ["estoque", "Estoque"],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={tab === id}
              className={`erp-tab ${tab === id ? "active" : ""}`}
              onClick={() => setTab(id)}
            >
              {label}
            </button>
          ))}
        </div>

        {tab === "ident" && (
          <FiscalSection title="Dados cadastrais">
            <FiscalRow>
              <FiscalField label="Tipo">
                <select
                  className="fiscal-input"
                  value={form.tipo || "P"}
                  onChange={(e) => setForm((f) => ({ ...f, tipo: e.target.value }))}
                >
                  <option value="P">P — Produto / mercadoria</option>
                  <option value="S">S — Serviço</option>
                </select>
              </FiscalField>
              <FiscalField label="Unidade comercial *">
                <select
                  className="fiscal-input"
                  value={form.unidade}
                  onChange={(e) => setForm((f) => ({ ...f, unidade: e.target.value }))}
                >
                  {unidadesSelect.map((u) => (
                    <option key={u.sigla} value={u.sigla}>
                      {u.sigla} — {u.descricao}
                    </option>
                  ))}
                </select>
              </FiscalField>
              <FiscalField label="GTIN / EAN">
                <input
                  className="fiscal-input fiscal-input--mono"
                  inputMode="numeric"
                  maxLength={14}
                  value={form.gtin ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, gtin: e.target.value.replace(/\D/g, "") }))}
                  placeholder="8, 12, 13 ou 14 dígitos"
                />
              </FiscalField>
              <FiscalField label="Peso líquido (kg)">
                <QtyInput value={form.peso} onChange={(v) => setForm((f) => ({ ...f, peso: v }))} />
              </FiscalField>
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="Grupo">
                <select
                  className="fiscal-input"
                  value={form.grupoId ?? ""}
                  onChange={(e) => {
                    const grupoId = e.target.value ? Number(e.target.value) : undefined;
                    setForm((f) => ({ ...f, grupoId, subgrupoId: undefined }));
                  }}
                >
                  <option value="">— Selecione o grupo —</option>
                  {grupos.map((g) => (
                    <option key={g.id} value={g.id}>
                      {g.nome}
                    </option>
                  ))}
                </select>
              </FiscalField>
              <FiscalField label="Subgrupo">
                <select
                  className="fiscal-input"
                  value={form.subgrupoId ?? ""}
                  disabled={!form.grupoId}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      subgrupoId: e.target.value ? Number(e.target.value) : undefined,
                    }))
                  }
                >
                  <option value="">
                    {form.grupoId ? "— Selecione o subgrupo —" : "Selecione o grupo primeiro"}
                  </option>
                  {subgrupos.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.nome}
                    </option>
                  ))}
                </select>
              </FiscalField>
            </FiscalRow>
            <FiscalField label="Descrição PDV / cupom">
              <input
                className="fiscal-input"
                maxLength={120}
                value={form.descricaoPdv ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, descricaoPdv: e.target.value }))}
                placeholder="Texto curto para cupom e conferência"
              />
            </FiscalField>
            <FiscalField label="Observações">
              <textarea
                className="fiscal-input fiscal-textarea"
                rows={5}
                maxLength={2000}
                value={form.observacoes ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, observacoes: e.target.value }))}
                placeholder="Anotações internas do item (lote, variedade, convênio, restrição de venda…)"
              />
            </FiscalField>
          </FiscalSection>
        )}

        {tab === "fiscal" && (
          <FiscalSection title="Classificação fiscal">
            <FiscalRow>
              <FiscalField label="NCM" className="erp-field--ncm">
                <div className="ncm-picker">
                  <input
                    className="fiscal-input fiscal-input--mono"
                    value={ncmBusca}
                    onFocus={() => setNcmAberto(true)}
                    onBlur={() => window.setTimeout(() => setNcmAberto(false), 180)}
                    onChange={(e) => {
                      const v = e.target.value;
                      setNcmBusca(v);
                      setNcmAberto(true);
                      setForm((f) => ({ ...f, codigoNcm: v.replace(/\D/g, "").slice(0, 8) }));
                    }}
                    placeholder="Digite 2+ dígitos — tabela TIPI"
                  />
                  {ncmAberto && ncmOpcoes.length > 0 && (
                    <div className="ncm-picker__list">
                      {ncmOpcoes.slice(0, 40).map((n) => (
                        <button
                          key={n.codigo}
                          type="button"
                          className="ncm-picker__item"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => {
                            setNcmBusca(n.codigo);
                            setNcmLabel(n.descricao);
                            setNcmAberto(false);
                            setForm((f) => ({ ...f, codigoNcm: n.codigo }));
                          }}
                        >
                          <span className="ncm-picker__code">{n.codigo}</span>
                          <span className="ncm-picker__desc">{n.descricao}</span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                {ncmLabel && <span className="erp-hint">{ncmLabel}</span>}
              </FiscalField>
              <FiscalField label="CEST">
                <div className="ncm-picker">
                  <input
                    className="fiscal-input fiscal-input--mono"
                    value={cestBusca}
                    onFocus={() => setCestAberto(true)}
                    onBlur={() => window.setTimeout(() => setCestAberto(false), 180)}
                    onChange={(e) => {
                      const v = e.target.value;
                      setCestBusca(v);
                      setCestAberto(true);
                      setCestLabel("");
                      setForm((f) => ({ ...f, cest: v.replace(/\D/g, "").slice(0, 7) }));
                    }}
                    placeholder={form.codigoNcm ? "Lista filtrada pelo NCM — ou busque" : "Busque código ou descrição"}
                  />
                  {cestAberto && cestOpcoes.length > 0 && (
                    <div className="ncm-picker__list">
                      {cestOpcoes.slice(0, 40).map((c) => (
                        <button
                          key={c.codigo}
                          type="button"
                          className="ncm-picker__item"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => {
                            setCestBusca(c.codigo);
                            setCestLabel(c.descricao);
                            setCestAberto(false);
                            setForm((f) => ({ ...f, cest: c.codigo }));
                          }}
                        >
                          <span className="ncm-picker__code">{c.codigo}</span>
                          <span className="ncm-picker__desc">{c.descricao}</span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                {cestLabel && <span className="erp-hint">{cestLabel}</span>}
              </FiscalField>
              <FiscalField label="EX TIPI">
                <input
                  className="fiscal-input fiscal-input--mono"
                  inputMode="numeric"
                  maxLength={3}
                  value={form.exTipi ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, exTipi: e.target.value.replace(/\D/g, "").slice(0, 3) }))}
                />
              </FiscalField>
            </FiscalRow>
            <FiscalRow>
              <FiscalField label="Origem da mercadoria">
                <select
                  className="fiscal-input"
                  value={form.origem || "0"}
                  onChange={(e) => setForm((f) => ({ ...f, origem: e.target.value }))}
                >
                  {ORIGENS_MERCADORIA.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </FiscalField>
              <FiscalField label="Grupo tributário">
                <select
                  className="fiscal-input"
                  value={form.grupoTributarioId ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      grupoTributarioId: e.target.value ? Number(e.target.value) : undefined,
                    }))
                  }
                >
                  <option value="">— Sem grupo (completar depois) —</option>
                  {gruposTrib.map((g) => (
                    <option key={g.id} value={g.id}>
                      {g.descricao}
                    </option>
                  ))}
                </select>
                {!form.grupoTributarioId && (
                  <span className="erp-hint erp-hint--warn">
                    Sem grupo o item entra na NF-e, mas a tributação precisa ser conferida na emissão.
                  </span>
                )}
              </FiscalField>
            </FiscalRow>
          </FiscalSection>
        )}

        {tab === "preco" && (
          <FiscalSection title="Custo, markup e venda">
            <p className="erp-hint" style={{ marginBottom: "0.75rem" }}>
              Informe custo e markup para sugerir o preço de venda. Se alterar o preço à mão, o markup é
              recalculado.
            </p>
            <FiscalRow>
              <FiscalField label="Preço de custo">
                <MoedaInput value={form.valorCusto} onChange={(v) => aplicarCustoMarkup(v, form.markup)} />
              </FiscalField>
              <FiscalField label="Markup">
                <PercentInput
                  value={form.markup}
                  onChange={(v) => {
                    setPrecoManual(false);
                    aplicarCustoMarkup(form.valorCusto, v);
                  }}
                />
              </FiscalField>
              <FiscalField label="Preço de venda">
                <MoedaInput value={form.valorUnitario} onChange={aplicarPrecoVenda} />
              </FiscalField>
            </FiscalRow>
            {precoCalc != null && (
              <div className="erp-formula">
                <span>
                  {fmtMoeda(form.valorCusto)} + {Number(form.markup ?? 0).toLocaleString("pt-BR")}% =
                </span>
                <strong>{fmtMoeda(precoCalc)}</strong>
                {precoManual ? (
                  <span className="erp-pill warn">preço informado manualmente</span>
                ) : (
                  <span className="erp-pill ok">preço sugerido pelo markup</span>
                )}
              </div>
            )}
          </FiscalSection>
        )}

        {tab === "estoque" && (
          <FiscalSection title="Controle de estoque">
            <FiscalRow>
              <FiscalField label="Estoque atual">
                <QtyInput
                  value={form.estoqueAtual}
                  onChange={(v) => setForm((f) => ({ ...f, estoqueAtual: v }))}
                />
              </FiscalField>
              <FiscalField label="Estoque mínimo">
                <QtyInput
                  value={form.estoqueMinimo}
                  onChange={(v) => setForm((f) => ({ ...f, estoqueMinimo: v }))}
                />
              </FiscalField>
              <FiscalField label="Unidade">
                <input className="fiscal-input" value={labelUnidade(form.unidade)} disabled />
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
          <h1 className="erp-list-head__title">Produtos</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Novo produto
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
          <div className="erp-kpi__label">Sem NCM</div>
          <div className="erp-kpi__value">{kpis.semNcm}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Sem grupo tributário</div>
          <div className="erp-kpi__value">{kpis.semGrupo}</div>
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
            placeholder="Código, descrição, grupo, GTIN, NCM…"
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
              <th>Código</th>
              <th>Descrição</th>
              <th>Un</th>
              <th>Grupo</th>
              <th>NCM</th>
              <th className="text-right">Custo</th>
              <th className="text-right">Markup</th>
              <th className="text-right">Venda</th>
              <th>Tributos</th>
              <th>Situação</th>
              <th style={{ width: "7.5rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={11} className="text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={11} className="text-center text-slate-500">
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
                  <td className="whitespace-nowrap font-medium tabular-nums">{row.codigo}</td>
                  <td>
                    <div className="erp-prod-nome">{row.nome}</div>
                    {row.descricaoPdv && row.descricaoPdv !== row.nome && (
                      <div className="erp-prod-pdv">{row.descricaoPdv}</div>
                    )}
                  </td>
                  <td className="whitespace-nowrap" title={labelUnidade(row.unidade)}>
                    {row.unidade}
                  </td>
                  <td className="max-w-[12rem] truncate">
                    {row.grupoId
                      ? `${grupoPorId.get(row.grupoId) ?? "Grupo"}`
                        + (row.subgrupoId ? ` / ${subgrupoPorId.get(row.subgrupoId) ?? ""}` : "")
                      : "—"}
                  </td>
                  <td className="whitespace-nowrap tabular-nums">
                    {row.codigoNcm || <span className="erp-pill warn">sem NCM</span>}
                  </td>
                  <td className="text-right whitespace-nowrap tabular-nums">{fmtMoeda(row.valorCusto)}</td>
                  <td className="text-right whitespace-nowrap tabular-nums">
                    {row.markup != null ? `${Number(row.markup).toLocaleString("pt-BR")}%` : "—"}
                  </td>
                  <td className="text-right whitespace-nowrap tabular-nums font-medium">
                    {fmtMoeda(row.valorUnitario)}
                  </td>
                  <td className="max-w-[11rem] truncate" title={tribPorId.get(row.grupoTributarioId ?? 0)}>
                    {row.grupoTributarioId
                      ? tribPorId.get(row.grupoTributarioId) ?? `#${row.grupoTributarioId}`
                      : "—"}
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
                        className="fiscal-btn-icon"
                        aria-label="Duplicar"
                        title="Duplicar"
                        onClick={() => void duplicar(row)}
                      >
                        <Copy className="h-4 w-4" />
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
