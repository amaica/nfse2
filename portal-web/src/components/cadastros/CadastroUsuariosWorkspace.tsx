"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Building2, Loader2, Pencil, Plus, RefreshCw, Search } from "lucide-react";
import { ApiError, formatarCnpjCpf } from "@/lib/api";
import { getAppToken } from "@/lib/app-session";
import { fiscalApi } from "@/lib/fiscal-api";
import { listarPerfis, type PortalPerfilDto } from "@/lib/permissoes-api";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { FiscalDetailToolbar } from "@/components/fiscal/FiscalDetailToolbar";
import { FiscalField, FiscalRow, FiscalSection } from "@/components/fiscal/FiscalFormUi";

const ENDPOINT = "/api/usuario";
const PAGE_SIZE = 12;

const PAPEIS = [
  { value: "OPERADOR", label: "Operador" },
  { value: "ADMIN", label: "Administrador" },
  { value: "VISUALIZADOR", label: "Visualizador" },
];

type EmpresaOpcao = {
  id: number;
  nome: string;
  cnpj?: string;
};

type EmpresaVinculo = {
  empresaId: number;
  empresaNome?: string;
  cnpj?: string;
  papel?: string;
  portalPerfilId?: number;
};

type UsuarioRow = {
  id: number;
  nome: string;
  email: string;
  cpf?: string;
  perfil?: string;
  papel?: string;
  ativo?: boolean;
  portalPerfilId?: number;
  empresas?: EmpresaVinculo[];
};

const emptyUsuario = (): Partial<UsuarioRow> & { senha?: string } => ({
  nome: "",
  email: "",
  senha: "",
  cpf: "",
  perfil: "OPERADOR",
  ativo: true,
});

function mensagemErro(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.status === 401) return "Sessão expirada — faça login novamente.";
    if (e.status === 403) return e.message || "Sem permissão para gerenciar usuários neste emitente.";
    return e.message || fallback;
  }
  if (e instanceof TypeError) return "Não foi possível conectar à API. Verifique se o portal está em execução.";
  return fallback;
}

function isGestaoPerfil(perfil?: string) {
  return perfil === "ADMIN" || perfil === "OWNER";
}

export function CadastroUsuariosWorkspace() {
  const { empresaId, empresaNome, empresaCnpj } = useEmpresaScope();
  const [viewMode, setViewMode] = useState<"list" | "form">("list");
  const [usuarios, setUsuarios] = useState<UsuarioRow[]>([]);
  const [empresas, setEmpresas] = useState<EmpresaOpcao[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [filtro, setFiltro] = useState("");
  const [page, setPage] = useState(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyUsuario());
  const [marcadas, setMarcadas] = useState<Set<number>>(new Set());
  const [erro, setErro] = useState("");
  const [aviso, setAviso] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [gruposPermissao, setGruposPermissao] = useState<PortalPerfilDto[]>([]);
  const [portalPerfilId, setPortalPerfilId] = useState<number | "">("");

  const carregarLista = useCallback(async () => {
    const token = getAppToken();
    if (!token) {
      setErro("Sessão expirada — faça login novamente.");
      setLoadingList(false);
      return;
    }
    setLoadingList(true);
    setErro("");
    setAviso("");

    let listaOk = false;
    try {
      const lista = await fiscalApi.list<UsuarioRow>(ENDPOINT);
      setUsuarios(lista);
      listaOk = true;
    } catch (e) {
      setErro(mensagemErro(e, "Erro ao carregar usuários"));
      setUsuarios([]);
    }

    try {
      const delegaveis = await fiscalApi.request<EmpresaOpcao[]>("/api/conta/empresas-delegaveis");
      setEmpresas(delegaveis);
    } catch (e) {
      if (listaOk) {
        setAviso(mensagemErro(e, "Lista de emitentes indisponível — tente Atualizar."));
      }
      setEmpresas([]);
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    if (!empresaId) return;
    setViewMode("list");
    setEditId(null);
    setForm(emptyUsuario());
    setPage(0);
    void carregarLista();
  }, [empresaId, carregarLista]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    if (!q) return usuarios;
    return usuarios.filter(
      (u) =>
        String(u.id).includes(q) ||
        (u.nome ?? "").toLowerCase().includes(q) ||
        (u.email ?? "").toLowerCase().includes(q),
    );
  }, [usuarios, filtro]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  const irLista = () => {
    setViewMode("list");
    setEditId(null);
    setForm(emptyUsuario());
    setMarcadas(new Set());
    setPortalPerfilId("");
  };

  const carregarGrupos = useCallback(async () => {
    try {
      setGruposPermissao(await listarPerfis());
    } catch {
      setGruposPermissao([]);
    }
  }, []);

  const novo = async () => {
    setEditId(null);
    setForm(emptyUsuario());
    setMarcadas(empresaId != null ? new Set([empresaId]) : new Set());
    setPortalPerfilId("");
    setViewMode("form");
    await carregarGrupos();
  };

  const editar = async (row: UsuarioRow) => {
    setEditId(row.id);
    setForm({
      nome: row.nome,
      email: row.email,
      cpf: row.cpf ?? "",
      perfil: row.perfil ?? row.papel ?? "OPERADOR",
      ativo: row.ativo !== false,
      senha: "",
    });
    const delegaveis = new Set(empresas.map((e) => e.id));
    const vinculadas = (row.empresas ?? [])
      .map((e) => e.empresaId)
      .filter((id) => delegaveis.has(id));
    setMarcadas(new Set(vinculadas.length > 0 ? vinculadas : empresaId != null ? [empresaId] : []));
    const perfilFromEmp =
      row.empresas?.find((e) => e.empresaId === empresaId)?.portalPerfilId ??
      row.portalPerfilId ??
      row.empresas?.find((e) => e.portalPerfilId != null)?.portalPerfilId;
    setPortalPerfilId(perfilFromEmp ?? "");
    setViewMode("form");
    await carregarGrupos();
  };

  const set = (key: string, value: string | boolean) => {
    setForm((f) => ({ ...f, [key]: value }));
  };

  const toggleEmpresa = (id: number) => {
    setMarcadas((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const marcarTodosEmitentes = () => {
    setMarcadas(new Set(empresas.map((e) => e.id)));
  };

  const desmarcarTodosEmitentes = () => {
    setMarcadas(new Set());
  };

  const totalMarcados = marcadas.size;
  const totalEmitentes = empresas.length;

  const salvar = async () => {
    if (!form.nome?.trim()) {
      setErro("Informe o nome.");
      return;
    }
    if (!form.email?.trim()) {
      setErro("Informe o e-mail.");
      return;
    }
    if (!editId && !form.senha?.trim()) {
      setErro("Informe a senha do novo usuário.");
      return;
    }
    if (marcadas.size === 0) {
      setErro("Selecione ao menos um emitente.");
      return;
    }
    if (!isGestaoPerfil(form.perfil) && portalPerfilId === "") {
      setErro("Selecione o grupo de permissão (menus) ou cadastre um em Conta → Permissões.");
      return;
    }

    setSalvando(true);
    setErro("");
    const token = getAppToken();
    if (!token) {
      setErro("Sessão expirada — faça login novamente.");
      setSalvando(false);
      return;
    }

    try {
      const body = {
        nome: form.nome.trim(),
        email: form.email.trim(),
        cpf: form.cpf?.replace(/\D/g, "") || undefined,
        perfil: form.perfil ?? "OPERADOR",
        ativo: form.ativo !== false,
        ...(form.senha?.trim() ? { senha: form.senha } : {}),
      };

      let usuarioId = editId;
      if (editId) {
        await fiscalApi.update(ENDPOINT, editId, body);
      } else {
        const criado = await fiscalApi.create<UsuarioRow>(ENDPOINT, body);
        usuarioId = criado.id;
      }

      if (usuarioId) {
        await fiscalApi.request("/api/conta/usuarios/vincular", {
          method: "POST",
          body: JSON.stringify({
            usuarioId,
            empresaIds: Array.from(marcadas),
            papel: form.perfil ?? "OPERADOR",
            portalPerfilId:
              isGestaoPerfil(form.perfil) || portalPerfilId === ""
                ? null
                : Number(portalPerfilId),
          }),
        });
      }

      await carregarLista();
      irLista();
    } catch (e) {
      setErro(mensagemErro(e, "Erro ao salvar usuário"));
    } finally {
      setSalvando(false);
    }
  };

  if (viewMode === "form") {
    return (
      <div className="fiscal-card">
        <FiscalDetailToolbar
          title={editId ? "Editar usuário" : "Novo usuário"}
          icon="users"
          onVoltar={irLista}
          onNovo={() => void novo()}
          onCancelar={irLista}
          onSalvar={salvar}
          saveDisabled={salvando}
        />
        {erro ? (
          <div className="mb-3 rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">
            {erro}
          </div>
        ) : null}

        <div className="fiscal-form-columns">
          <div>
            <FiscalSection title="Dados do usuário">
              <FiscalField label="Nome">
                <input
                  className="fiscal-input"
                  value={form.nome ?? ""}
                  onChange={(e) => set("nome", e.target.value)}
                />
              </FiscalField>
              <FiscalField label="E-mail (login)">
                <input
                  className="fiscal-input"
                  type="email"
                  value={form.email ?? ""}
                  onChange={(e) => set("email", e.target.value)}
                  readOnly={!!editId}
                />
              </FiscalField>
              <FiscalRow>
                <FiscalField label={editId ? "Nova senha (opcional)" : "Senha"}>
                  <input
                    className="fiscal-input"
                    type="password"
                    value={form.senha ?? ""}
                    onChange={(e) => set("senha", e.target.value)}
                    placeholder={editId ? "Deixe em branco para manter" : ""}
                  />
                </FiscalField>
                <FiscalField label="CPF">
                  <input
                    className="fiscal-input"
                    value={form.cpf ?? ""}
                    onChange={(e) => set("cpf", e.target.value)}
                  />
                </FiscalField>
              </FiscalRow>
              <FiscalRow>
                <FiscalField label="Papel do sistema">
                  <select
                    className="fiscal-input"
                    value={form.perfil ?? "OPERADOR"}
                    onChange={(e) => {
                      const v = e.target.value;
                      set("perfil", v);
                      if (isGestaoPerfil(v)) setPortalPerfilId("");
                    }}
                  >
                    {PAPEIS.map((p) => (
                      <option key={p.value} value={p.value}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-xs text-agro-muted">
                    Operador/Visualizador usam o grupo de menus. Admin vê todos os menus.
                  </p>
                </FiscalField>
              </FiscalRow>
              <FiscalField label="Grupo de permissão (menus)">
                <select
                  className="fiscal-input"
                  disabled={isGestaoPerfil(form.perfil)}
                  value={portalPerfilId === "" ? "" : String(portalPerfilId)}
                  onChange={(e) =>
                    setPortalPerfilId(e.target.value ? Number(e.target.value) : "")
                  }
                >
                  <option value="">
                    {isGestaoPerfil(form.perfil)
                      ? "— não se aplica a Administrador —"
                      : gruposPermissao.length === 0
                        ? "Nenhum grupo cadastrado (vá em Conta → Permissões)"
                        : "Selecione…"}
                  </option>
                  {!isGestaoPerfil(form.perfil) &&
                    gruposPermissao.map((g) => (
                      <option key={g.id} value={g.id}>
                        {g.nome}
                        {g.totalMenus != null ? ` (${g.totalMenus} menus)` : ""}
                      </option>
                    ))}
                </select>
                {isGestaoPerfil(form.perfil) ? (
                  <p className="mt-1 text-xs text-amber-700">
                    Mude o papel para <strong>Operador</strong> para poder escolher o grupo
                    (ex.: operador_werlang).
                  </p>
                ) : gruposPermissao.length === 0 ? (
                  <p className="mt-1 text-xs text-amber-700">
                    Cadastre o grupo em Conta → Permissões e reabra este usuário.
                  </p>
                ) : (
                  <p className="mt-1 text-xs text-agro-muted">
                    Grupos cadastrados em Conta → Permissões.
                  </p>
                )}
              </FiscalField>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.ativo !== false}
                  onChange={(e) => set("ativo", e.target.checked)}
                />
                Ativo
              </label>
            </FiscalSection>
          </div>

          <div>
            <FiscalSection title="Emitentes com acesso">
              <p className="mb-3 text-sm text-agro-muted">
                Marque quais emitentes este usuário pode acessar. Ao salvar, os vínculos são
                atualizados — emitentes desmarcados perdem o acesso.
              </p>
              {empresas.length === 0 ? (
                <p className="text-sm text-amber-700">
                  Nenhum emitente disponível para delegar. Troque de emitente ou verifique suas
                  permissões (OWNER ou ADMIN).
                </p>
              ) : (
              <>
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm text-agro-muted">
                    {totalMarcados} de {totalEmitentes} selecionado{totalEmitentes !== 1 ? "s" : ""}
                  </span>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      className="rounded-lg border border-[var(--border)] px-3 py-1.5 text-sm font-medium text-agro-body hover:bg-[var(--primary-50)] disabled:opacity-40"
                      onClick={marcarTodosEmitentes}
                      disabled={totalMarcados === totalEmitentes}
                    >
                      Marcar todos
                    </button>
                    <button
                      type="button"
                      className="rounded-lg border border-[var(--border)] px-3 py-1.5 text-sm font-medium text-agro-body hover:bg-[var(--primary-50)] disabled:opacity-40"
                      onClick={desmarcarTodosEmitentes}
                      disabled={totalMarcados === 0}
                    >
                      Desmarcar todos
                    </button>
                  </div>
                </div>
                <div className="grid max-h-80 gap-2 overflow-y-auto sm:grid-cols-2">
                  {empresas.map((emp) => (
                    <label
                      key={emp.id}
                      className="flex cursor-pointer items-start gap-2 rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-[var(--primary-50)]"
                    >
                      <input
                        type="checkbox"
                        checked={marcadas.has(emp.id)}
                        onChange={() => toggleEmpresa(emp.id)}
                        className="mt-1"
                      />
                      <span>
                        <span className="block font-medium text-agro-body">{emp.nome}</span>
                        <span className="text-xs text-agro-muted">{formatarCnpjCpf(emp.cnpj ?? "")}</span>
                      </span>
                    </label>
                  ))}
                </div>
              </>
              )}
            </FiscalSection>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fiscal-card">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="m-0 text-2xl font-semibold text-slate-800">Cadastro de usuários</h1>
          {empresaNome && (
            <p className="mt-1 text-sm text-slate-500">
              Emitente ativo: <span className="font-medium text-slate-700">{empresaNome}</span>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={() => void novo()}>
            <Plus className="h-4 w-4" /> Novo
          </button>
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            onClick={() => void carregarLista()}
          >
            <RefreshCw className="h-4 w-4" /> Atualizar
          </button>
        </div>
      </div>

      {erro && <p className="mb-3 text-sm text-red-600">{erro}</p>}
      {aviso && !erro && <p className="mb-3 text-sm text-amber-700">{aviso}</p>}

      <p className="mb-4 text-sm text-slate-600">
        Lista todos os usuários dos emitentes em que você é OWNER ou ADMIN. Edite para alterar papel,
        grupo de permissão e quais emitentes cada um acessa.
      </p>

      <div className="fiscal-table-caption">
        <span>Filtro</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Nome ou e-mail..."
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
              <th>E-mail</th>
              <th>Perfil</th>
              <th>Emitentes</th>
              <th style={{ width: "6rem" }} />
            </tr>
          </thead>
          <tbody>
            {loadingList ? (
              <tr>
                <td colSpan={6} className="text-center text-slate-500">
                  <Loader2 className="mr-2 inline h-4 w-4 animate-spin" />
                  Carregando…
                </td>
              </tr>
            ) : pagina.length === 0 ? (
              <tr>
                <td colSpan={6} className="text-center text-slate-500">
                  Nenhum usuário
                </td>
              </tr>
            ) : (
              pagina.map((row) => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.nome}</td>
                  <td>{row.email}</td>
                  <td>{row.perfil ?? row.papel ?? "—"}</td>
                  <td>
                    <span className="inline-flex items-center gap-1 text-sm text-slate-600">
                      <Building2 className="h-3.5 w-3.5" />
                      {row.empresas?.length ?? 0}
                    </span>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="fiscal-btn-icon"
                      aria-label="Editar"
                      onClick={() => void editar(row)}
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
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
