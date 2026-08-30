"use client";

import { useEffect, useMemo, useState } from "react";
import { Pencil, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { formatarCnpjCpf, type EmpresaResumo } from "@/lib/api";
import { labelAmbiente, labelCrt } from "@/lib/empresa-opcoes";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";

type Props = {
  empresas: EmpresaResumo[];
  carregando: boolean;
  onNovo: () => void;
  onEditar: (id: number) => void;
  onExcluir: (id: number, nome: string) => void;
  onAtualizar: () => void;
};

const PAGE_SIZE = 20;

export function EmpresaListagem({ empresas, carregando, onNovo, onEditar, onExcluir, onAtualizar }: Props) {
  const { empresaNome, empresaCnpj } = useEmpresaScope();
  const [filtro, setFiltro] = useState("");
  const [page, setPage] = useState(0);

  const kpis = useMemo(() => {
    const total = empresas.length;
    const producao = empresas.filter((e) => (e.ambiente ?? "").toLowerCase() === "producao").length;
    const cert = empresas.filter((e) => e.certificadoCadastrado).length;
    const simples = empresas.filter((e) => e.optanteSimples).length;
    return { total, producao, cert, simples };
  }, [empresas]);

  const filtrados = useMemo(() => {
    const q = filtro.trim().toLowerCase();
    if (!q) return empresas;
    const qDigits = q.replace(/\D/g, "");
    return empresas.filter((e) => {
      const crt = labelCrt(undefined, e.optanteSimples).toLowerCase();
      const amb = labelAmbiente(e.ambiente).toLowerCase();
      return (
        (e.nome ?? "").toLowerCase().includes(q) ||
        (e.nomeFantasia ?? "").toLowerCase().includes(q) ||
        (e.cnpj ?? "").includes(qDigits || q) ||
        (e.municipio ?? "").toLowerCase().includes(q) ||
        (e.uf ?? "").toLowerCase().includes(q) ||
        crt.includes(q) ||
        amb.includes(q)
      );
    });
  }, [empresas, filtro]);

  const totalPages = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const pagina = filtrados.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  useEffect(() => {
    if (page >= totalPages) setPage(Math.max(0, totalPages - 1));
  }, [page, totalPages]);

  return (
    <div className="fiscal-card">
      <div className="erp-list-head">
        <div>
          <h1 className="erp-list-head__title">Emitentes</h1>
          {empresaNome && (
            <p className="erp-list-head__sub">
              Emitente atual: <strong>{empresaNome}</strong>
              {empresaCnpj ? ` · ${formatarCnpjCpf(empresaCnpj)}` : ""}
            </p>
          )}
          <p className="erp-list-head__sub">Cadastro da empresa, certificado A1, endereços e numeração da NF-e / NFS-e.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary" onClick={onNovo}>
            <Plus className="h-4 w-4" /> Novo emitente
          </button>
          <button type="button" className="fiscal-btn-secondary" onClick={onAtualizar} disabled={carregando}>
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
          <div className="erp-kpi__label">Produção</div>
          <div className="erp-kpi__value">{kpis.producao}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Simples Nacional</div>
          <div className="erp-kpi__value">{kpis.simples}</div>
        </div>
        <div className="erp-kpi">
          <div className="erp-kpi__label">Com certificado</div>
          <div className="erp-kpi__value">{kpis.cert}</div>
        </div>
      </div>

      <div className="fiscal-table-caption">
        <span>Filtro na grade</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Nome, CPF/CNPJ, município, regime…"
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
              <th>Razão social</th>
              <th>CPF / CNPJ</th>
              <th>Município</th>
              <th>Regime</th>
              <th>Ambiente</th>
              <th>Certificado</th>
              <th style={{ width: "6.5rem" }} />
            </tr>
          </thead>
          <tbody>
            {carregando && empresas.length === 0 ? (
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
                <tr key={row.id} onDoubleClick={() => onEditar(row.id)}>
                  <td>
                    <div className="erp-prod-nome">{row.nome}</div>
                    {row.nomeFantasia && row.nomeFantasia !== row.nome && (
                      <div className="erp-prod-pdv">{row.nomeFantasia}</div>
                    )}
                  </td>
                  <td className="whitespace-nowrap tabular-nums">{formatarCnpjCpf(row.cnpj)}</td>
                  <td>
                    {[row.municipio, row.uf].filter(Boolean).join(" / ") || "—"}
                  </td>
                  <td>{labelCrt(undefined, row.optanteSimples)}</td>
                  <td>{labelAmbiente(row.ambiente)}</td>
                  <td>
                    <span className={`erp-pill ${row.certificadoCadastrado ? "ok" : "off"}`}>
                      {row.certificadoCadastrado ? "Cadastrado" : "Pendente"}
                    </span>
                  </td>
                  <td>
                    <div className="fiscal-table-actions">
                      <button
                        type="button"
                        className="fiscal-btn-icon"
                        title="Editar"
                        aria-label="Editar"
                        onClick={() => onEditar(row.id)}
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        className="fiscal-btn-icon danger"
                        title="Excluir"
                        aria-label="Excluir"
                        onClick={() => {
                          if (window.confirm(`Excluir o emitente "${row.nome}"? Esta ação não pode ser desfeita.`)) {
                            onExcluir(row.id, row.nome);
                          }
                        }}
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
