"use client";

type Props = {
  title: string;
  endpoint?: string;
  columns?: { key: string; label: string }[];
};

export function TributacaoCrudPage({
  title,
  endpoint,
  columns = [
    { key: "id", label: "Id" },
    { key: "descricao", label: "Descrição" },
  ],
}: Props) {
  return (
    <div className="fiscal-card">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-800">{title}</h1>
        <div className="flex gap-2">
          <button type="button" className="fiscal-btn-primary">
            Novo
          </button>
          <button
            type="button"
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            Atualizar
          </button>
        </div>
      </div>
      {endpoint && (
        <p className="mb-4 text-sm text-amber-700 rounded-lg bg-amber-50 px-3 py-2">
          API <code className="text-xs">{endpoint}</code> — conecte o backend fiscal (FISCALBACKEND) ou
          implemente no portal-api.
        </p>
      )}
      <div className="overflow-x-auto">
        <table className="fiscal-table">
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key}>{c.label}</th>
              ))}
              <th style={{ width: "8rem" }} />
            </tr>
          </thead>
          <tbody>
            <tr>
              <td colSpan={columns.length + 1} className="py-8 text-center text-slate-500">
                Nenhum registro — cadastro disponível após integração da API.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
