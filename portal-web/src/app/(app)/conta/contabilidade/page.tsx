"use client";

import { useCallback, useEffect, useState } from "react";
import { BookOpen, Download, FileText, Mail, RefreshCw, Save } from "lucide-react";
import { GestaoGuard } from "@/components/auth/GestaoGuard";
import { getAppToken } from "@/lib/app-session";
import { api } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";

type ConfigContabilidade = {
  emailContabilidade: string;
  envioAutomatico: boolean;
  enviarNfse: boolean;
  enviarNfe: boolean;
  enviarNfeEntrada: boolean;
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
};

type ResumoLivroCaixa = {
  de: string;
  ate: string;
  totalNotas: number;
  receitas: number;
  despesas: number;
  resultado: number;
  fonte: string;
  lcdprDisponivel: boolean;
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

export default function ContabilidadePage() {
  return (
    <GestaoGuard>
      <ContabilidadeConteudo />
    </GestaoGuard>
  );
}

function ContabilidadeConteudo() {
  const [config, setConfig] = useState<ConfigContabilidade>({
    emailContabilidade: "",
    envioAutomatico: false,
    enviarNfse: true,
    enviarNfe: true,
    enviarNfeEntrada: false,
  });
  const [de, setDe] = useState(primeiroDiaAno());
  const [ate, setAte] = useState(hojeIso());
  const [incluirNfse, setIncluirNfse] = useState(true);
  const [incluirNfe, setIncluirNfe] = useState(true);
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [carregandoLivro, setCarregandoLivro] = useState(false);
  const [baixando, setBaixando] = useState<string | null>(null);
  const [baixandoXmls, setBaixandoXmls] = useState(false);
  const [resumo, setResumo] = useState<ResumoLivroCaixa | null>(null);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");

  const carregarConfig = useCallback(async () => {
    const token = getAppToken();
    if (!token) return;
    setLoading(true);
    setErro("");
    try {
      const res = await fetch(`${apiBaseUrl()}/api/conta/contabilidade/config`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error((await res.json().catch(() => ({})) as { erro?: string }).erro ?? res.statusText);
      setConfig(await res.json());
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao carregar");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregarConfig();
  }, [carregarConfig]);

  async function salvar() {
    const token = getAppToken();
    if (!token) return;
    setSalvando(true);
    setErro("");
    setOk("");
    try {
      const res = await fetch(`${apiBaseUrl()}/api/conta/contabilidade/config`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify(config),
      });
      if (!res.ok) throw new Error((await res.json().catch(() => ({})) as { erro?: string }).erro ?? res.statusText);
      setConfig(await res.json());
      setOk("Configuração salva.");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao salvar");
    } finally {
      setSalvando(false);
    }
  }

  async function carregarLivroCaixa() {
    const token = getAppToken();
    if (!token) return;
    setCarregandoLivro(true);
    setErro("");
    setOk("");
    try {
      const params = new URLSearchParams({ de, ate, nfse: String(incluirNfse), nfe: String(incluirNfe) });
      const res = await fetch(`${apiBaseUrl()}/api/conta/contabilidade/livro-caixa?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error((await res.json().catch(() => ({})) as { erro?: string }).erro ?? res.statusText);
      setResumo((await res.json()) as ResumoLivroCaixa);
      setOk("Livro Caixa atualizado a partir dos XMLs das notas.");
    } catch (e) {
      setResumo(null);
      setErro(e instanceof Error ? e.message : "Falha ao gerar livro caixa");
    } finally {
      setCarregandoLivro(false);
    }
  }

  async function baixarArquivo(tipo: "zip" | "csv" | "lcdpr") {
    const token = getAppToken();
    if (!token) return;
    setBaixando(tipo);
    setErro("");
    try {
      const params = new URLSearchParams({ de, ate, nfse: String(incluirNfse), nfe: String(incluirNfe) });
      const path =
        tipo === "zip"
          ? `/api/conta/contabilidade/export.zip?${params}`
          : tipo === "csv"
            ? `/api/conta/contabilidade/livro-caixa.csv?${params}`
            : `/api/conta/contabilidade/lcdpr.txt?${params}`;
      const res = await fetch(`${apiBaseUrl()}${path}`, { headers: { Authorization: `Bearer ${token}` } });
      if (!res.ok) throw new Error((await res.json().catch(() => ({})) as { erro?: string }).erro ?? res.statusText);
      const blob = await res.blob();
      const disp = res.headers.get("Content-Disposition") ?? "";
      const match = disp.match(/filename="([^"]+)"/);
      const filename = match?.[1] ?? `syncnota-${tipo}-${de}_${ate}`;
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha no download");
    } finally {
      setBaixando(null);
    }
  }

  async function baixarXmlsEntrada() {
    const token = getAppToken();
    if (!token) return;
    setBaixandoXmls(true);
    setErro("");
    setOk("");
    try {
      const res = await api.nfeBaixarXmlsDestinatario(token);
      setOk(
        `${res.novas} XML(s) de entrada gravados para despesas do livro caixa` +
          (res.ultimoNsu ? ` (NSU ${res.ultimoNsu}).` : "."),
      );
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao baixar XMLs de entrada");
    } finally {
      setBaixandoXmls(false);
    }
  }

  if (loading) {
    return <div className="app-loading">Carregando…</div>;
  }

  return (
    <div className="animate-in mx-auto max-w-4xl space-y-6">
      <header>
        <p className="page-header__eyebrow">Conta</p>
        <h1 className="page-header__title">Livro Caixa + LCDPR</h1>
        <p className="page-header__subtitle">
          Apuração a partir dos XMLs das NFS-e/NF-e emitidas (receitas) e das NF-e de entrada baixadas da SEFAZ (despesas)
        </p>
      </header>

      <section className="fiscal-card p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="agro-icon-box">
            <BookOpen className="h-4 w-4" />
          </div>
          <div>
            <h2 className="font-semibold text-agro-body">Livro Caixa e LCDPR</h2>
            <p className="text-sm text-agro-muted">
              Lê os XMLs das NFS-e e NF-e do período (emitidas = receita, entradas SEFAZ = despesa) e gera apuração, CSV e arquivo LCDPR leiaute 1.3 (RFB).
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
            Incluir NF-e
          </label>
        </div>

        <div className="flex flex-wrap gap-2">
          <button type="button" className="fiscal-btn-primary inline-flex items-center gap-2" onClick={() => void carregarLivroCaixa()} disabled={carregandoLivro}>
            <RefreshCw className={`h-4 w-4 ${carregandoLivro ? "animate-spin" : ""}`} />
            {carregandoLivro ? "Lendo XMLs…" : "Atualizar apuração"}
          </button>
          <button
            type="button"
            className="btn-ghost inline-flex items-center gap-2 border border-[var(--border)]"
            onClick={() => void baixarXmlsEntrada()}
            disabled={baixandoXmls}
          >
            <Download className={`h-4 w-4 ${baixandoXmls ? "animate-spin" : ""}`} />
            {baixandoXmls ? "Baixando XMLs…" : "Baixar XMLs de entrada"}
          </button>
          <button type="button" className="btn-ghost inline-flex items-center gap-2 border border-[var(--border)]" onClick={() => void baixarArquivo("csv")} disabled={!!baixando}>
            <FileText className="h-4 w-4" />
            Livro Caixa CSV
          </button>
          <button type="button" className="btn-ghost inline-flex items-center gap-2 border border-[var(--border)]" onClick={() => void baixarArquivo("lcdpr")} disabled={!!baixando}>
            <Download className="h-4 w-4" />
            LCDPR (.txt)
          </button>
          <button type="button" className="btn-ghost inline-flex items-center gap-2 border border-[var(--border)]" onClick={() => void baixarArquivo("zip")} disabled={!!baixando}>
            <Download className="h-4 w-4" />
            ZIP XMLs
          </button>
        </div>

        {resumo && (
          <div className="mt-6 space-y-4">
            <div className="grid gap-3 sm:grid-cols-4">
              {[
                { label: "Notas", value: String(resumo.totalNotas) },
                { label: "Receitas", value: moeda(resumo.receitas) },
                { label: "Despesas", value: moeda(resumo.despesas) },
                { label: "Resultado", value: moeda(resumo.resultado) },
              ].map((c) => (
                <div key={c.label} className="rounded-lg border border-[var(--border)] bg-[var(--primary-50)]/50 p-3">
                  <p className="text-xs uppercase tracking-wide text-agro-muted">{c.label}</p>
                  <p className="mt-1 text-lg font-bold text-agro-body">{c.value}</p>
                </div>
              ))}
            </div>
            <p className="text-xs text-agro-muted">{resumo.fonte}</p>
            {!resumo.lcdprDisponivel && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
                LCDPR exige emitente com CPF (11 dígitos). Emitente CNPJ: use Livro Caixa CSV e valide com o contador.
              </p>
            )}
            <div className="overflow-x-auto rounded-lg border border-[var(--border)]">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-[var(--primary-50)] text-xs uppercase text-agro-muted">
                  <tr>
                    <th className="px-3 py-2">Data</th>
                    <th className="px-3 py-2">Tipo</th>
                    <th className="px-3 py-2">Movimento</th>
                    <th className="px-3 py-2">Histórico</th>
                    <th className="px-3 py-2 text-right">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  {resumo.itens.map((item) => (
                    <tr key={item.chave + item.data} className="border-t border-[var(--border)]">
                      <td className="px-3 py-2 whitespace-nowrap">{item.data}</td>
                      <td className="px-3 py-2">{item.origem}</td>
                      <td className="px-3 py-2">
                        <span className={item.movimento === "DESPESA" ? "text-red-700" : "text-emerald-700"}>
                          {item.movimento === "DESPESA" ? "Despesa" : "Receita"}
                        </span>
                      </td>
                      <td className="px-3 py-2 max-w-xs truncate" title={item.historico}>{item.historico}</td>
                      <td className={`px-3 py-2 text-right font-medium ${item.movimento === "DESPESA" ? "text-red-700" : "text-emerald-800"}`}>
                        {item.movimento === "DESPESA" ? "− " : ""}
                        {moeda(item.valor)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>

      <section className="fiscal-card p-6">
        <div className="mb-4 flex items-center gap-3">
          <div className="agro-icon-box">
            <Mail className="h-4 w-4" />
          </div>
          <div>
            <h2 className="font-semibold text-agro-body">Envio automático de XML</h2>
            <p className="text-sm text-agro-muted">A cada emissão, o XML autorizado vai para o e-mail do contador.</p>
          </div>
        </div>
        <div className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-agro-body">E-mail da contabilidade</span>
            <input type="email" className="fiscal-input w-full" placeholder="contador@escritorio.com.br" value={config.emailContabilidade} onChange={(e) => setConfig((c) => ({ ...c, emailContabilidade: e.target.value }))} />
          </label>
          <label className="flex cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={config.envioAutomatico} onChange={(e) => setConfig((c) => ({ ...c, envioAutomatico: e.target.checked }))} />
            Ativar envio automático
          </label>
          <div className="flex flex-wrap gap-4 text-sm text-agro-body">
            <label className="flex cursor-pointer items-center gap-2">
              <input type="checkbox" checked={config.enviarNfse} onChange={(e) => setConfig((c) => ({ ...c, enviarNfse: e.target.checked }))} />
              NFS-e emitidas
            </label>
            <label className="flex cursor-pointer items-center gap-2">
              <input type="checkbox" checked={config.enviarNfe} onChange={(e) => setConfig((c) => ({ ...c, enviarNfe: e.target.checked }))} />
              NF-e emitidas
            </label>
            <label className="flex cursor-pointer items-center gap-2">
              <input
                type="checkbox"
                checked={config.enviarNfeEntrada}
                onChange={(e) => setConfig((c) => ({ ...c, enviarNfeEntrada: e.target.checked }))}
              />
              NF-e recebidas (DF-e) — ZIP periódico
            </label>
          </div>
          <p className="text-xs text-agro-muted">
            DF-e: com «Baixar notas DF-e» no emitente, o cron baixa da SEFAZ e, se marcado acima, envia ZIP das novas ao e-mail.
          </p>
          <button type="button" className="fiscal-btn-primary inline-flex items-center gap-2" onClick={() => void salvar()} disabled={salvando}>
            <Save className="h-4 w-4" />
            {salvando ? "Salvando…" : "Salvar"}
          </button>
        </div>
      </section>

      {erro && <p className="text-sm text-red-600">{erro}</p>}
      {ok && <p className="text-sm text-emerald-700">{ok}</p>}
    </div>
  );
}
