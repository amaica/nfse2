"use client";

import { useEffect, useMemo, useState } from "react";
import { Plus, RefreshCw, Search } from "lucide-react";
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
        <span>Filtro</span>
        <div className="fiscal-table-search relative">
          <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Nome, CPF/CNPJ, município…"
            value={filtro}
            onChange={(e) => {
              setFiltro(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {carregando && empresas.length === 0 ? (
          <p className="text-sm text-slate-500">Carregando…</p>
        ) : pagina.length === 0 ? (
          <p className="text-sm text-slate-500">Nenhum emitente neste filtro.</p>
        ) : (
          pagina.map((row) => (
            <article key={row.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <button type="button" className="w-full text-left" onClick={() => onEditar(row.id)}>
                <div className="text-lg font-semibold text-slate-800">{row.nome}</div>
                {row.nomeFantasia && row.nomeFantasia !== row.nome && (
                  <div className="text-sm text-slate-500">{row.nomeFantasia}</div>
                )}
                <div className="mt-1 font-mono text-sm text-slate-600">{formatarCnpjCpf(row.cnpj)}</div>
                <div className="mt-2 text-sm text-slate-500">
                  {[row.municipio, row.uf].filter(Boolean).join(" / ") || "Endereço ainda não informado"}
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">
                    {labelCrt(undefined, row.optanteSimples)}
                  </span>
                  <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">
                    {labelAmbiente(row.ambiente)}
                  </span>
                  <span className={`erp-pill ${row.certificadoCadastrado ? "ok" : "off"}`}>
                    {row.certificadoCadastrado ? "Certificado ok" : "Falta certificado"}
                  </span>
                </div>
              </button>
              <button
                type="button"
                className="mt-3 text-xs text-red-600"
                onClick={() => {
                  if (window.confirm(`Excluir o emitente "${row.nome}"? Esta ação não pode ser desfeita.`)) {
                    onExcluir(row.id, row.nome);
                  }
                }}
              >
                Excluir
              </button>
            </article>
          ))
        )}
      </div>

      <div className="fiscal-pagination">
        <span>
          {filtrados.length === 0
            ? "0 registros"
            : `${page * PAGE_SIZE + 1}–${Math.min((page + 1) * PAGE_SIZE, filtrados.length)} de ${filtrados.length}`}
        </span>
        <div className="flex gap-2">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Anterior
          </button>
          <button type="button" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            Próxima
          </button>
        </div>
      </div>
    </div>
  );
}
