"use client";

import { useEffect, useState } from "react";
import { Calculator, RefreshCw } from "lucide-react";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { useEmpresaScope } from "@/hooks/useEmpresaScope";
import { getAppToken } from "@/lib/app-session";
import { apiBaseUrl } from "@/lib/api-base";

type FaixaAplicada = {
  de: number;
  ate: number | null;
  aliquota: number;
  parcelaDeduzir?: number;
  baseParcial: number;
  impostoParcial: number;
};

type LancamentoItem = {
  data: string;
  origem: string;
  numero: string;
  chave: string;
  historico: string;
  contraparte: string;
  valor: number;
  movimento: string;
  fluxo: "ENTRA" | "SAI";
  tipoNota: string;
  tipoLabel: string;
};

type ResumoSimulado = {
  de: string;
  ate: string;
  totalNotas: number;
  entradas: number;
  saidas: number;
  resultado: number;
  baseCalculo: number;
  irEstimado: number;
  faixasAplicadas: FaixaAplicada[];
  tabelaIrVersao: string;
  aviso: string;
  fonte: string;
  itens: LancamentoItem[];
};

function primeiroDiaAno(): string {
  return `${new Date().getFullYear()}-01-01`;
}

function hojeIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function moeda(v: number): string {
  return v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function pct(v: number): string {
  return `${(Number(v) * 100).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;
}

export default function SimuladoIrPage() {
  return <SimuladoIrConteudo />;
}

function SimuladoIrConteudo() {
  const { empresaId, empresaNome } = useEmpresaScope();
  const [de, setDe] = useState(primeiroDiaAno());
  const [ate, setAte] = useState(hojeIso());
  const [incluirNfse, setIncluirNfse] = useState(true);
  const [incluirNfe, setIncluirNfe] = useState(true);
  const [carregando, setCarregando] = useState(false);
  const [resumo, setResumo] = useState<ResumoSimulado | null>(null);
  const [erro, setErro] = useState("");

  useEffect(() => {
    setResumo(null);
    setErro("");
  }, [empresaId]);

  async function carregar() {
    const token = getAppToken();
    if (!token || !empresaId) return;
    setCarregando(true);
    setErro("");
    try {
      const qs = new URLSearchParams({
        de,
        ate,
        nfse: String(incluirNfse),
        nfe: String(incluirNfe),
      });
      const res = await fetch(`${apiBaseUrl()}/api/conta/simulado-ir?${qs}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        throw new Error((await res.json().catch(() => ({})) as { erro?: string }).erro ?? res.statusText);
      }
      setResumo((await res.json()) as ResumoSimulado);
    } catch (e) {
      setResumo(null);
      setErro(e instanceof Error ? e.message : "Falha ao simular");
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="animate-in mx-auto max-w-5xl space-y-6">
      <header>
        <p className="page-header__eyebrow">Simulado IR</p>
        <h1 className="page-header__title">Simulado de Imposto de Renda</h1>
        <p className="page-header__subtitle">
          Caixa a partir das notas: entra $$ (NF-e saída / NFS-e) e sai $$ (NF-e entrada emitida + DF-e) + estimativa IR PF
        </p>
      </header>

      <EmitenteEmissaoBar dica="O simulado usa as notas (emitidas e DF-e) do emitente selecionado. Troque acima para recalcular outro emitente." />

      <section className="fiscal-card p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="agro-icon-box">
            <Calculator className="h-4 w-4" />
          </div>
          <div>
            <h2 className="font-semibold text-agro-body">Apuração do período</h2>
            <p className="text-sm text-agro-muted">
              Emitente: <strong>{empresaNome ?? "—"}</strong>
              {" · "}
              NF-e usa <strong>tpNF</strong> (1 = entra $$, 0 = sai $$). DF-e contra o emitente = sai $$.
            </p>
          </div>
        </div>

        <div className="mb-4 grid gap-4 sm:grid-cols-2">
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-agro-body">De</span>
            <input type="date" className="fiscal-input w-full" value={de} onChange={(e) => setDe(e.target.value)} />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-agro-body">Até</span>
            <input type="date" className="fiscal-input w-full" value={ate} onChange={(e) => setAte(e.target.value)} />
          </label>
        </div>

        <div className="mb-4 flex flex-wrap gap-4 text-sm text-agro-body">
          <label className="flex cursor-pointer items-center gap-2">
            <input type="checkbox" checked={incluirNfse} onChange={(e) => setIncluirNfse(e.target.checked)} />
            Incluir NFS-e
          </label>
          <label className="flex cursor-pointer items-center gap-2">
            <input type="checkbox" checked={incluirNfe} onChange={(e) => setIncluirNfe(e.target.checked)} />
            Incluir NF-e (emitidas + DF-e)
          </label>
        </div>

        <button
          type="button"
          className="fiscal-btn-primary inline-flex items-center gap-2"
          onClick={() => void carregar()}
          disabled={carregando || !empresaId}
        >
          <RefreshCw className={`h-4 w-4 ${carregando ? "animate-spin" : ""}`} />
          {carregando ? "Calculando…" : "Gerar simulado"}
        </button>

        {resumo && (
          <div className="mt-6 space-y-4">
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
              {resumo.aviso}
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
              {[
                { label: "Notas", value: String(resumo.totalNotas) },
                { label: "Entra $$", value: moeda(resumo.entradas) },
                { label: "Sai $$", value: moeda(resumo.saidas) },
                { label: "Resultado", value: moeda(resumo.resultado) },
                { label: "IR estimado", value: moeda(resumo.irEstimado) },
              ].map((c) => (
                <div key={c.label} className="rounded-lg border border-[var(--border)] bg-[var(--primary-50)]/50 p-3">
                  <p className="text-xs uppercase tracking-wide text-agro-muted">{c.label}</p>
                  <p className="mt-1 text-lg font-bold text-agro-body">{c.value}</p>
                </div>
              ))}
            </div>

            <p className="text-xs text-agro-muted">
              Base de cálculo: {moeda(resumo.baseCalculo)} · Tabela: {resumo.tabelaIrVersao} · {resumo.fonte}
            </p>

            {resumo.faixasAplicadas?.length > 0 && (
              <div className="overflow-x-auto rounded-lg border border-[var(--border)]">
                <table className="min-w-full text-left text-sm">
                  <thead className="bg-[var(--primary-50)] text-xs uppercase text-agro-muted">
                    <tr>
                      <th className="px-3 py-2">Faixa de</th>
                      <th className="px-3 py-2">Até</th>
                      <th className="px-3 py-2">Alíquota</th>
                      <th className="px-3 py-2 text-right">Parcela a deduzir</th>
                      <th className="px-3 py-2 text-right">IR parcial</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resumo.faixasAplicadas.map((f, i) => (
                      <tr key={i} className="border-t border-[var(--border)]">
                        <td className="px-3 py-2">{moeda(Number(f.de))}</td>
                        <td className="px-3 py-2">{f.ate == null ? "—" : moeda(Number(f.ate))}</td>
                        <td className="px-3 py-2">{pct(Number(f.aliquota))}</td>
                        <td className="px-3 py-2 text-right">{moeda(Number(f.parcelaDeduzir ?? 0))}</td>
                        <td className="px-3 py-2 text-right font-medium">{moeda(Number(f.impostoParcial))}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="overflow-x-auto rounded-lg border border-[var(--border)]">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-[var(--primary-50)] text-xs uppercase text-agro-muted">
                  <tr>
                    <th className="px-3 py-2">Data</th>
                    <th className="px-3 py-2">Origem</th>
                    <th className="px-3 py-2">Tipo</th>
                    <th className="px-3 py-2">Fluxo</th>
                    <th className="px-3 py-2">Contraparte</th>
                    <th className="px-3 py-2 text-right">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  {resumo.itens.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-3 py-6 text-center text-agro-muted">
                        Nenhuma nota com XML no período.
                      </td>
                    </tr>
                  ) : (
                    resumo.itens.map((item) => (
                      <tr key={item.chave + item.data + item.fluxo} className="border-t border-[var(--border)]">
                        <td className="whitespace-nowrap px-3 py-2">{item.data}</td>
                        <td className="px-3 py-2">{item.origem}</td>
                        <td className="px-3 py-2">{item.tipoLabel}</td>
                        <td className="px-3 py-2">
                          <span className={item.fluxo === "SAI" ? "text-red-700" : "text-emerald-700"}>
                            {item.fluxo === "SAI" ? "Sai $$" : "Entra $$"}
                          </span>
                        </td>
                        <td className="max-w-[12rem] truncate px-3 py-2" title={item.contraparte || item.historico}>
                          {item.contraparte || item.historico}
                        </td>
                        <td
                          className={`px-3 py-2 text-right font-medium ${
                            item.fluxo === "SAI" ? "text-red-700" : "text-emerald-800"
                          }`}
                        >
                          {item.fluxo === "SAI" ? "− " : ""}
                          {moeda(item.valor)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>

      {erro && <p className="text-sm text-red-600">{erro}</p>}
    </div>
  );
}
