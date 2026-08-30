"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Loader2, MapPin, Pencil, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { apenasNumeros, cnpjValido, cpfValido } from "@/lib/cpf-cnpj";
import { isValidLat, isValidLng, normalizarCoord } from "@/lib/coordenadas-geo";
import { consultarCep, consultarCnpjPessoa } from "@/lib/consulta-externa";
import { fiscalApi, type PessoaDto } from "@/lib/fiscal-api";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";
import { AddressMap } from "@/components/map/AddressMap";

const PAGE_SIZE = 10;
const ENDPOINT = "/api/pessoas";
const MAP_CENTER_BR: [number, number] = [-14.235, -51.925];
const MAP_ZOOM = 14;

const emptyPessoa = (): PessoaDto => ({
  nome: "",
  nomeFantasia: "",
  tipo: "J",
  cpfCnpj: "",
  email: "",
  fone: "",
  celular: "",
  inscricaoEstadual: "",
  logradouro: "",
  numero: "",
  complemento: "",
  bairro: "",
  municipio: "",
  uf: "",
  cep: "",
  codigoMunicipioIbge: "",
  latitude: "",
  longitude: "",
  observacoes: "",
  ativo: true,
});

export function CadastroPessoasWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [pessoas, setPessoas] = useState<PessoaDto[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<PessoaDto>(emptyPessoa());
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [loadingCnpj, setLoadingCnpj] = useState(false);
  const [loadingCep, setLoadingCep] = useState(false);
  const [mapView, setMapView] = useState<"mapa" | "satelite">("satelite");

  const carregarLista = useCallback(async () => {
    setLoadingList(true);
    setErro("");
    try {
      const data = await fiscalApi.listPessoas();
      setPessoas(data);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar pessoas");
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) {
      setLoadingList(false);
      return;
    }
    setViewMode("list");
    setEditId(null);
    setForm(emptyPessoa());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const filtradas = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    if (!q) return pessoas;
    return pessoas.filter(
      (p) =>
        String(p.id ?? "").includes(q) ||
        (p.nome ?? "").toLowerCase().includes(q) ||
        (p.cpfCnpj ?? "").includes(q) ||
        (p.email ?? "").toLowerCase().includes(q),
    );
  }, [pessoas, filtro]);

  const totalPages = Math.max(1, Math.ceil(filtradas.length / PAGE_SIZE));
  const pagina = filtradas.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irPesquisa = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyPessoa());
  };

  const novo = () => {
    setEditId(null);
    setForm(emptyPessoa());
    setViewMode("form");
  };

  const editar = async (row: PessoaDto) => {
    if (!row.id) return;
    setErro("");
    try {
      const full = await fiscalApi.get<PessoaDto>(ENDPOINT, row.id);
      setEditId(row.id);
      setForm({ ...emptyPessoa(), ...full });
      setViewMode("form");
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao abrir cadastro");
    }
  };

  const salvar = async () => {
    if (!form.nome?.trim()) {
      setErro("Informe o nome / razão social.");
      return;
    }
    setSalvando(true);
    setErro("");
    try {
      if (editId) {
        await fiscalApi.update(ENDPOINT, editId, form);
      } else {
        await fiscalApi.create(ENDPOINT, form);
      }
      await carregarLista();
      irPesquisa();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao salvar");
    } finally {
      setSalvando(false);
    }
  };

  const excluir = async (row: PessoaDto) => {
    if (!row.id) return;
    if (!window.confirm(`Excluir "${row.nome}"?`)) return;
    setErro("");
    try {
      await fiscalApi.remove(ENDPOINT, row.id);
      await carregarLista();
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro ao excluir");
    }
  };

  const set = (key: keyof PessoaDto, value: string | boolean) => {
    setForm((f) => ({ ...f, [key]: value }));
  };

  const updateMany = useCallback((updates: Partial<PessoaDto>) => {
    setForm((prev) => ({ ...prev, ...updates }));
    setErro("");
  }, []);

  const preencherCep = useCallback(
    async (cepDigits: string) => {
      if (cepDigits.length !== 8) return;
      setLoadingCep(true);
      try {
        const c = await consultarCep(cepDigits);
        updateMany({
          logradouro: c.logradouro || undefined,
          bairro: c.bairro || undefined,
          municipio: c.localidade || undefined,
          uf: c.uf || undefined,
          codigoMunicipioIbge: c.ibge || undefined,
          ...(c.latitude != null && c.latitude !== "" && { latitude: c.latitude }),
          ...(c.longitude != null && c.longitude !== "" && { longitude: c.longitude }),
        });
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Falha ao consultar CEP");
      } finally {
        setLoadingCep(false);
      }
    },
    [updateMany],
  );

  const handleCpfCnpjBlur = useCallback(
    async (currentValue?: string) => {
      const raw = (currentValue ?? form.cpfCnpj ?? "").trim();
      if (!raw) return;
      const num = apenasNumeros(raw);
      if (num.length === 11) {
        if (!cpfValido(num)) {
          setErro("CPF inválido. Verifique os dígitos.");
          return;
        }
        setErro("");
        updateMany({ tipo: "F", cpfCnpj: num });
        return;
      }
      if (num.length === 0) return;
      const doc = num.length <= 14 ? num.padStart(14, "0") : num.slice(0, 14);
      if (doc.length !== 14) {
        setErro("Documento incompleto — informe 11 dígitos (CPF) ou 14 (CNPJ).");
        return;
      }
      if (!cnpjValido(doc)) {
        setErro("CNPJ inválido. Verifique os dígitos.");
        return;
      }
      setLoadingCnpj(true);
      setErro("");
      try {
        const d = await consultarCnpjPessoa(doc);
        const end = d.endereco ?? {};
        updateMany({
          tipo: "J",
          cpfCnpj: doc,
          nome: d.razaoSocial?.trim() || form.nome,
          email: d.email?.trim() || form.email,
          cep: end.cep?.replace(/\D/g, "") || form.cep,
          logradouro: end.logradouro?.trim() || form.logradouro,
          numero: end.numero?.trim() || form.numero,
          bairro: end.bairro?.trim() || form.bairro,
          municipio: end.municipio?.trim() || form.municipio,
          uf: end.uf?.trim().toUpperCase() || form.uf,
          codigoMunicipioIbge:
            end.codigoMunicipioIbge?.replace(/\D/g, "") ||
            d.codigoMunicipioIbge?.replace(/\D/g, "") ||
            form.codigoMunicipioIbge,
        });
        const cepDigits = (end.cep ?? "").replace(/\D/g, "");
        if (cepDigits.length === 8) {
          await preencherCep(cepDigits);
        }
      } catch (e) {
        setErro(e instanceof ApiError ? e.message : "Falha ao consultar CNPJ");
      } finally {
        setLoadingCnpj(false);
      }
    },
    [form, preencherCep, updateMany],
  );

  const handleCepBlur = useCallback(async () => {
    const digits = (form.cep ?? "").replace(/\D/g, "");
    if (digits.length !== 8) {
      if (digits.length > 0) setErro("CEP deve conter exatamente 8 dígitos.");
      return;
    }
    await preencherCep(digits);
  }, [form.cep, preencherCep]);

  const handleLatBlur = useCallback(() => {
    const v = normalizarCoord(form.latitude ?? "");
    if (!v) return;
    if (!isValidLat(v)) {
      setErro("Latitude deve estar entre -90 e 90.");
      return;
    }
    set("latitude", v);
    setErro("");
  }, [form.latitude]);

  const handleLngBlur = useCallback(() => {
    const v = normalizarCoord(form.longitude ?? "");
    if (!v) return;
    if (!isValidLng(v)) {
      setErro("Longitude deve estar entre -180 e 180.");
      return;
    }
    set("longitude", v);
    setErro("");
  }, [form.longitude]);

  const mapCenter: [number, number] =
    form.latitude != null &&
    form.longitude != null &&
    isValidLat(form.latitude) &&
    isValidLng(form.longitude)
      ? [Number(normalizarCoord(form.latitude)), Number(normalizarCoord(form.longitude))]
      : MAP_CENTER_BR;

  const markerPosition: [number, number] = mapCenter;

  const handleMarkerPositionChange = useCallback(
    (lat: number, lng: number) => {
      updateMany({ latitude: lat.toFixed(6), longitude: lng.toFixed(6) });
    },
    [updateMany],
  );

  if (viewMode === "form") {
    return (
      <div className="fiscal-card">
        <FiscalDetailToolbar
          title="Cadastro de pessoa"
          icon="users"
          onVoltar={irPesquisa}
          onNovo={novo}
          onCancelar={irPesquisa}
          onSalvar={salvar}
          saveDisabled={salvando}
        />
        {erro && <p className="mb-3 text-sm text-red-600">{erro}</p>}
        {(loadingCnpj || loadingCep) && (
          <p className="mb-3 flex items-center gap-2 text-sm text-[var(--primary-700)]">
            <Loader2 className="h-4 w-4 animate-spin" />
            {loadingCnpj ? "Buscando dados do CNPJ na Receita Federal…" : "Buscando endereço pelo CEP…"}
          </p>
        )}

        <div className="fiscal-form-columns">
          <div>
            <FiscalSection title="Geral">
              <FiscalRow>
                <FiscalField label="Cpf/Cnpj">
                  <input
                    className="fiscal-input"
                    value={form.cpfCnpj ?? ""}
                    onChange={(e) => set("cpfCnpj", e.target.value)}
                    onBlur={(e) => void handleCpfCnpjBlur(e.target.value)}
                    placeholder="CPF ou CNPJ — saia do campo para buscar"
                  />
                </FiscalField>
                <FiscalField label="Tipo">
                  <select
                    className="fiscal-input"
                    value={form.tipo}
                    onChange={(e) => set("tipo", e.target.value)}
                  >
                    <option value="F">F — Pessoa física</option>
                    <option value="J">J — Pessoa jurídica</option>
                  </select>
                </FiscalField>
              </FiscalRow>
              <FiscalField label="Nome / Razão social">
                <input
                  className="fiscal-input"
                  value={form.nome}
                  onChange={(e) => set("nome", e.target.value)}
                />
              </FiscalField>
              <FiscalField label="Nome fantasia">
                <input
                  className="fiscal-input"
                  value={form.nomeFantasia ?? ""}
                  onChange={(e) => set("nomeFantasia", e.target.value)}
                />
              </FiscalField>
              <FiscalRow>
                <FiscalField label="E-mail">
                  <input
                    className="fiscal-input"
                    type="email"
                    value={form.email ?? ""}
                    onChange={(e) => set("email", e.target.value)}
                  />
                </FiscalField>
                <FiscalField label="Inscrição estadual">
                  <input
                    className="fiscal-input"
                    value={form.inscricaoEstadual ?? ""}
                    onChange={(e) => set("inscricaoEstadual", e.target.value)}
                  />
                </FiscalField>
              </FiscalRow>
              <FiscalRow>
                <FiscalField label="Telefone">
                  <input
                    className="fiscal-input"
                    value={form.fone ?? ""}
                    onChange={(e) => set("fone", e.target.value)}
                  />
                </FiscalField>
                <FiscalField label="Celular">
                  <input
                    className="fiscal-input"
                    value={form.celular ?? ""}
                    onChange={(e) => set("celular", e.target.value)}
                  />
                </FiscalField>
              </FiscalRow>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.ativo}
                  onChange={(e) => set("ativo", e.target.checked)}
                />
                Ativo
              </label>
            </FiscalSection>
          </div>

          <div>
            <FiscalSection title="Endereço">
              <FiscalRow>
                <FiscalField label="CEP">
                  <input
                    className="fiscal-input"
                    value={form.cep ?? ""}
                    onChange={(e) => set("cep", e.target.value)}
                    onBlur={() => void handleCepBlur()}
                    placeholder="00000-000"
                  />
                </FiscalField>
                <FiscalField label="Cód. IBGE município">
                  <input
                    className="fiscal-input"
                    value={form.codigoMunicipioIbge ?? ""}
                    onChange={(e) => set("codigoMunicipioIbge", e.target.value)}
                  />
                </FiscalField>
              </FiscalRow>
              <FiscalField label="Logradouro">
                <input
                  className="fiscal-input"
                  value={form.logradouro ?? ""}
                  onChange={(e) => set("logradouro", e.target.value)}
                />
              </FiscalField>
              <FiscalRow>
                <FiscalField label="Número">
                  <input
                    className="fiscal-input"
                    value={form.numero ?? ""}
                    onChange={(e) => set("numero", e.target.value)}
                  />
                </FiscalField>
                <FiscalField label="Bairro">
                  <input
                    className="fiscal-input"
                    value={form.bairro ?? ""}
                    onChange={(e) => set("bairro", e.target.value)}
                  />
                </FiscalField>
              </FiscalRow>
              <FiscalField label="Referência / complemento">
                <input
                  className="fiscal-input"
                  value={form.complemento ?? ""}
                  onChange={(e) => set("complemento", e.target.value)}
                />
              </FiscalField>
              <FiscalRow>
                <FiscalField label="Município">
                  <input
                    className="fiscal-input"
                    value={form.municipio ?? ""}
                    onChange={(e) => set("municipio", e.target.value)}
                  />
                </FiscalField>
                <FiscalField label="UF">
                  <input
                    className="fiscal-input"
                    maxLength={2}
                    value={form.uf ?? ""}
                    onChange={(e) => set("uf", e.target.value.toUpperCase())}
                  />
                </FiscalField>
              </FiscalRow>
              <FiscalRow>
                <FiscalField label="Latitude">
                  <input
                    className="fiscal-input"
                    placeholder="-90 a 90"
                    value={form.latitude ?? ""}
                    onChange={(e) => set("latitude", e.target.value)}
                    onBlur={handleLatBlur}
                  />
                </FiscalField>
                <FiscalField label="Longitude">
                  <input
                    className="fiscal-input"
                    placeholder="-180 a 180"
                    value={form.longitude ?? ""}
                    onChange={(e) => set("longitude", e.target.value)}
                    onBlur={handleLngBlur}
                  />
                </FiscalField>
              </FiscalRow>
            </FiscalSection>

            <FiscalSection title="Mapa">
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <MapPin className="h-4 w-4 text-slate-500" />
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setMapView("mapa")}
                    className={`rounded-lg px-3 py-1.5 text-sm font-medium ${
                      mapView === "mapa"
                        ? "bg-[var(--primary-600)] text-white"
                        : "border border-slate-300 bg-white text-slate-700"
                    }`}
                  >
                    Mapa
                  </button>
                  <button
                    type="button"
                    onClick={() => setMapView("satelite")}
                    className={`rounded-lg px-3 py-1.5 text-sm font-medium ${
                      mapView === "satelite"
                        ? "bg-[var(--primary-600)] text-white"
                        : "border border-slate-300 bg-white text-slate-700"
                    }`}
                  >
                    Satélite
                  </button>
                </div>
              </div>
              <p className="mb-2 text-xs text-slate-500">
                Arraste o marcador para definir a localização; as coordenadas são salvas automaticamente.
              </p>
              <AddressMap
                viewMode={mapView}
                center={mapCenter}
                zoom={MAP_ZOOM}
                markerPosition={markerPosition}
                draggable
                onMarkerPositionChange={handleMarkerPositionChange}
                height="20rem"
              />
            </FiscalSection>

            <FiscalSection title="Observações">
              <textarea
                className="fiscal-input min-h-[5rem] w-full resize-y"
                value={form.observacoes ?? ""}
                onChange={(e) => set("observacoes", e.target.value)}
                placeholder="Anotações sobre o cadastro"
              />
            </FiscalSection>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="m-0 text-2xl font-semibold text-slate-800">Cadastro de pessoas</h1>
        {empresaNome && (
          <p className="mt-1 text-sm text-slate-500">
            Emitente: <span className="font-medium text-slate-700">{empresaNome}</span>
            {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            {!loadingList && pessoas.length > 0 && (
              <span className="ml-2 font-medium text-[var(--primary-700)]">
                · {pessoas.length.toLocaleString("pt-BR")} cadastros
              </span>
            )}
          </p>
        )}
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={novo}>
            <Plus className="h-4 w-4" /> Novo
          </button>
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            onClick={carregarLista}
          >
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
        </div>
      </div>

      {erro && <p className="mb-3 text-sm text-red-600">{erro}</p>}

      {!empresaId && (
        <p className="mb-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          Nenhum emitente selecionado. Faça login novamente ou use o seletor de empresa na barra superior.
        </p>
      )}

      <div className="fiscal-table-caption">
        <span>Filtro na grade</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Filtrar registros..."
            value={filtro}
            onChange={(e) => {
              setFiltro(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="fiscal-table striped">
          <thead>
            <tr>
              <th style={{ width: "5rem" }}>Id</th>
              <th>Nome</th>
              <th>Cpf/Cnpj</th>
              <th>Email</th>
              <th>UF</th>
              <th style={{ width: "6rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={6} className="text-center text-slate-500">
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center text-slate-500">
                  {erro
                    ? "Não foi possível carregar — verifique login e emitente ativo"
                    : empresaId
                      ? "Nenhum registro neste emitente"
                      : "Selecione um emitente para ver os cadastros"}
                </td>
              </tr>
            ) : (
              pagina.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.nome || "—"}</td>
                  <td>{row.cpfCnpj || "—"}</td>
                  <td>{row.email || "—"}</td>
                  <td>{row.uf || "—"}</td>
                  <td>
                    <div className="fiscal-table-actions">
                      <button
                        type="button"
                        className="fiscal-btn-icon"
                        aria-label="Editar"
                        onClick={() => editar(row)}
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        className="fiscal-btn-icon danger"
                        aria-label="Excluir"
                        onClick={() => excluir(row)}
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
          {filtradas.length === 0
            ? "0 registros"
            : `${page * PAGE_SIZE + 1}–${Math.min((page + 1) * PAGE_SIZE, filtradas.length)} de ${filtradas.length}`}
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
