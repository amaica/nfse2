"use client";

import { useState } from "react";
import { formatarCnpjCpf, type EmpresaResumo } from "@/lib/api";

type Props = {
  empresas: EmpresaResumo[];
  carregando: boolean;
  onNovo: () => void;
  onEditar: (id: number) => void;
  onExcluir: (id: number, nome: string) => void;
  onAtualizar: () => void;
};

const POR_PAGINA = 20;

export function EmpresaListagem({ empresas, carregando, onNovo, onEditar, onExcluir, onAtualizar }: Props) {
  const [pagina, setPagina] = useState(1);

  const totalPaginas = Math.max(1, Math.ceil(empresas.length / POR_PAGINA));
  const paginaAtual = Math.min(pagina, totalPaginas);
  const inicio = (paginaAtual - 1) * POR_PAGINA;
  const itens = empresas.slice(inicio, inicio + POR_PAGINA);

  return (
    <div className="rounded border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 px-4 py-3">
        <h1 className="text-base font-medium text-slate-800">Listagem empresa</h1>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 bg-slate-700 px-3 py-2 text-white">
        <div className="flex items-center gap-2 text-sm">
          <button
            type="button"
            onClick={onNovo}
            className="flex h-8 w-8 items-center justify-center rounded bg-blue-600 text-lg font-bold hover:bg-blue-500"
            title="Nova empresa"
          >
            +
          </button>
          <span className="text-slate-300">
            ({paginaAtual} of {totalPaginas})
          </span>
          <button
            type="button"
            disabled={paginaAtual <= 1}
            onClick={() => setPagina(1)}
            className="px-1 disabled:opacity-40"
          >
            «
          </button>
          <button
            type="button"
            disabled={paginaAtual <= 1}
            onClick={() => setPagina((p) => Math.max(1, p - 1))}
            className="px-1 disabled:opacity-40"
          >
            ‹
          </button>
          {Array.from({ length: totalPaginas }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              type="button"
              onClick={() => setPagina(n)}
              className={`min-w-[1.5rem] rounded px-1 ${n === paginaAtual ? "bg-slate-500" : ""}`}
            >
              {n}
            </button>
          ))}
          <button
            type="button"
            disabled={paginaAtual >= totalPaginas}
            onClick={() => setPagina((p) => Math.min(totalPaginas, p + 1))}
            className="px-1 disabled:opacity-40"
          >
            ›
          </button>
          <button
            type="button"
            disabled={paginaAtual >= totalPaginas}
            onClick={() => setPagina(totalPaginas)}
            className="px-1 disabled:opacity-40"
          >
            »
          </button>
          <select
            className="ml-2 rounded border-0 bg-slate-600 px-2 py-1 text-sm text-white"
            value={POR_PAGINA}
            disabled
          >
            <option value={20}>20</option>
          </select>
        </div>
        <button
          type="button"
          onClick={onAtualizar}
          disabled={carregando}
          className="text-xs text-slate-300 hover:text-white disabled:opacity-50"
        >
          Atualizar
        </button>
      </div>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate-200 bg-slate-50 text-center text-slate-600">
            <th className="px-4 py-2 font-medium">Nome</th>
            <th className="px-4 py-2 font-medium">Cnpj</th>
            <th className="w-28 px-4 py-2 font-medium">Ações</th>
          </tr>
        </thead>
        <tbody>
          {itens.length === 0 ? (
            <tr>
              <td colSpan={3} className="px-4 py-8 text-center text-slate-500">
                Nenhuma empresa cadastrada.
              </td>
            </tr>
          ) : (
            itens.map((e) => (
              <tr key={e.id} className="border-b border-slate-100 text-center hover:bg-slate-50">
                <td className="px-4 py-3 text-slate-800">{e.nome}</td>
                <td className="px-4 py-3 text-slate-700">{formatarCnpjCpf(e.cnpj)}</td>
                <td className="px-4 py-3">
                  <div className="flex justify-center gap-2">
                    <button
                      type="button"
                      onClick={() => onEditar(e.id)}
                      className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-white hover:bg-blue-500"
                      title="Editar"
                    >
                      ✎
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (window.confirm(`Excluir a empresa "${e.nome}"? Esta ação não pode ser desfeita.`)) {
                          onExcluir(e.id, e.nome);
                        }
                      }}
                      className="flex h-8 w-8 items-center justify-center rounded-full bg-red-600 text-white hover:bg-red-500"
                      title="Excluir"
                    >
                      ✕
                    </button>
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
