"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import type { AutoCompleteCompleteEvent } from "primereact/autocomplete";
import { api, ApiError } from "@/lib/api";
import { apiBaseUrl } from "@/lib/api-base";
import { getAppToken } from "@/lib/app-session";
import { EmitenteEmissaoBar } from "@/components/fiscal/EmitenteEmissaoBar";
import { AutoCompleteField, type AcOption } from "@/components/ui/AutoCompleteField";

type EmitenteOpt = { id: number; nome: string };

type NotaLista = {
  chave: string;
  serie?: string;
  numero?: number;
  empresaId?: number;
  emitenteNome?: string;
  statusProtocolo?: string;
  createdAt?: string;
};

type NotaEvento = NotaLista & {
  protocolo?: string;
  proximaCarta?: number;
  status?: string;
  ambiente?: string;
};

type SerieOpt = { serie: string; ultimoNumero: number };

function asOption(value: AcOption | string | null): AcOption | null {
  if (!value || typeof value === "string") return null;
  return value;
}

function statusNota(codigo?: string): string {
  if (codigo === "100") return "Autorizada";
  if (codigo === "101") return "Cancelada";
  return codigo ? `Status ${codigo}` : "";
}

function formatarData(iso?: string): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleDateString("pt-BR");
}

export function NfeRegistrarEvento() {
  const token = getAppToken();
  const [emitentes, setEmitentes] = useState<EmitenteOpt[]>([]);
  const [emitente, setEmitente] = useState("");
  const [series, setSeries] = useState<SerieOpt[]>([]);

  const [cancelBusca, setCancelBusca] = useState<AcOption | string | null>(null);
  const [cancelSug, setCancelSug] = useState<AcOption[]>([]);
  const [cancelNota, setCancelNota] = useState<NotaEvento | null>(null);
  const [justificativa, setJustificativa] = useState("");

  const [cceBusca, setCceBusca] = useState<AcOption | string | null>(null);
  const [cceSug, setCceSug] = useState<AcOption[]>([]);
  const [cceNota, setCceNota] = useState<NotaEvento | null>(null);
  const [sequencial, setSequencial] = useState("1");
  const [correcao, setCorrecao] = useState("");

  const [serieBusca, setSerieBusca] = useState<AcOption | string | null>(null);
  const [serieSug, setSerieSug] = useState<AcOption[]>([]);
  const [numIni, setNumIni] = useState("");
  const [numFim, setNumFim] = useState("");
  const [ano, setAno] = useState(String(new Date().getFullYear()));
  const [justificativaInut, setJustificativaInut] = useState("");
  const [ultimoNumero, setUltimoNumero] = useState<number | null>(null);

  const [resultado, setResultado] = useState("");
  const avisoRef = useRef<HTMLParagraphElement>(null);
  const [erro, setErro] = useState("");

  useEffect(() => {
    if (!token) return;
    let cancelado = false;
    void (async () => {
      try {
        const res = await fetch(`${apiBaseUrl()}/api/nfe/eventos?page=0&size=1`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) return;
        const body = (await res.json()) as { emitentes?: EmitenteOpt[] };
        if (!cancelado && body.emitentes) setEmitentes(body.emitentes);
      } catch {
        /* o select fica vazio */
      }
    })();
    return () => {
      cancelado = true;
    };
  }, [token]);

  useEffect(() => {
    if (!token || !emitente) {
      setSeries([]);
      return;
    }
    let cancelado = false;
    void (async () => {
      try {
        const res = await fetch(`${apiBaseUrl()}/api/nfe/eventos/series?emitente=${encodeURIComponent(emitente)}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) return;
        const body = (await res.json()) as SerieOpt[];
        if (!cancelado) setSeries(Array.isArray(body) ? body : []);
      } catch {
        if (!cancelado) setSeries([]);
      }
    })();
    return () => {
      cancelado = true;
    };
  }, [token, emitente]);

  const buscarNotas = useCallback(
    async (q: string) => {
      if (!token) return [];
      const params = new URLSearchParams({ page: "0", size: "12", ambiente: "producao", status: "100" });
      if (q.trim()) params.set("q", q.trim());
      if (emitente) params.set("emitente", emitente);
      const res = await fetch(`${apiBaseUrl()}/api/nfe/notas?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return [];
      const body = (await res.json()) as { itens?: NotaLista[] };
      return (body.itens ?? []).map((n) => ({
        label: `Nº ${n.numero ?? "—"} · série ${n.serie ?? "—"}`,
        value: n.chave,
        meta: [n.emitenteNome, formatarData(n.createdAt), statusNota(n.statusProtocolo)].filter(Boolean).join(" · "),
        raw: n,
      }));
    },
    [token, emitente],
  );

  const carregarDetalhe = async (chave: string): Promise<NotaEvento | null> => {
    if (!token) return null;
    const res = await fetch(`${apiBaseUrl()}/api/nfe/eventos/notas/${encodeURIComponent(chave)}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new ApiError((body as { erro?: string }).erro ?? "Nota não encontrada", res.status);
    }
    return body as NotaEvento;
  };

  const aplicarEmitenteDaNota = (nota: NotaEvento) => {
    if (nota.empresaId) setEmitente(String(nota.empresaId));
  };

  const escolherCancelamento = async (opt: AcOption | null) => {
    setCancelBusca(opt);
    setErro("");
    if (!opt) {
      setCancelNota(null);
      return;
    }
    try {
      const detalhe = await carregarDetalhe(opt.value);
      setCancelNota(detalhe);
      if (detalhe) aplicarEmitenteDaNota(detalhe);
    } catch (e) {
      setCancelNota(null);
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar a nota");
    }
  };

  const escolherCarta = async (opt: AcOption | null) => {
    setCceBusca(opt);
    setErro("");
    if (!opt) {
      setCceNota(null);
      return;
    }
    try {
      const detalhe = await carregarDetalhe(opt.value);
      setCceNota(detalhe);
      if (detalhe?.proximaCarta) setSequencial(String(detalhe.proximaCarta));
      if (detalhe) aplicarEmitenteDaNota(detalhe);
    } catch (e) {
      setCceNota(null);
      setErro(e instanceof ApiError ? e.message : "Erro ao carregar a nota");
    }
  };

  const seriesParaAc = (q: string): AcOption[] => {
    const termo = q.trim().toLowerCase();
    return series
      .filter((s) => !termo || s.serie.toLowerCase().includes(termo))
      .map((s) => ({
        label: `Série ${s.serie}`,
        value: s.serie,
        meta: `Último número emitido: ${s.ultimoNumero}`,
        raw: s,
      }));
  };

  const escolherSerie = (opt: AcOption | null, texto?: string) => {
    setSerieBusca(opt ?? texto ?? null);
    const serie = opt?.value || texto || "";
    const conhecida = series.find((s) => s.serie === serie);
    if (conhecida) {
      const proximo = conhecida.ultimoNumero + 1;
      setUltimoNumero(conhecida.ultimoNumero);
      setNumIni(String(proximo));
      setNumFim(String(proximo));
      return;
    }
    setUltimoNumero(null);
  };

  useEffect(() => {
    if (!erro && !resultado) return;
    avisoRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [erro, resultado]);

  const emitenteId = emitente ? Number(emitente) : undefined;
  const serieInut = typeof serieBusca === "string" ? serieBusca : serieBusca?.value ?? "";

  const run = async (fn: () => Promise<unknown>, empresaDaNota?: number) => {
    if (!token) return;
    if (!empresaDaNota && !emitenteId) {
      setErro("Selecione o emitente. O evento usa o certificado dele.");
      setResultado("");
      return;
    }
    setErro("");
    setResultado("");
    try {
      const res = (await fn()) as { ok?: boolean; mensagem?: string; motivo?: string };
      const texto = res?.mensagem || res?.motivo || "Evento enviado.";
      if (res?.ok) setResultado(texto);
      else setErro(texto);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro");
    }
  };

  return (
    <div className="space-y-4">
      <EmitenteEmissaoBar dica="Busque a nota pelo número ou pela série. A chave e o protocolo são preenchidos sozinhos." />
      <div className="fiscal-card space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h1 className="text-2xl font-semibold text-slate-800">Registrar evento da NF-e</h1>
          <Link href="/nfe/eventos-fiscais" className="text-sm text-slate-600 underline">
            Ver eventos registrados
          </Link>
        </div>

        {(erro || resultado) && (
          <p
            ref={avisoRef}
            role="alert"
            className={
              erro
                ? "rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                : "rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
            }
          >
            {erro || resultado}
          </p>
        )}

        <label className="block max-w-xl text-sm">
          <span className="mb-1 block text-slate-500">Emitente</span>
          <select className="fiscal-input w-full" value={emitente} onChange={(e) => setEmitente(e.target.value)}>
            <option value="">Todos — ou escolha ao selecionar a nota</option>
            {emitentes.map((em) => (
              <option key={em.id} value={String(em.id)}>
                {em.nome}
              </option>
            ))}
          </select>
        </label>

        <section className="space-y-3 rounded-xl border border-slate-200 p-4">
          <h2 className="font-medium">Cancelamento</h2>
          <AutoCompleteField
            id="nfe-cancel-nota"
            label="Nota"
            placeholder="Nota de produção — número, série ou chave…"
            value={cancelBusca}
            suggestions={cancelSug}
            completeMethod={(e: AutoCompleteCompleteEvent) => {
              void buscarNotas(e.query).then(setCancelSug);
            }}
            onChange={(v) => {
              const opt = asOption(v);
              if (opt) void escolherCancelamento(opt);
              else {
                setCancelBusca(v);
                setCancelNota(null);
              }
            }}
          />
          {cancelNota && (
            <p className="text-sm text-slate-600">
              {cancelNota.emitenteNome ? `${cancelNota.emitenteNome} · ` : ""}
              nº {cancelNota.numero} · série {cancelNota.serie}
              {statusNota(cancelNota.status) ? ` · ${statusNota(cancelNota.status)}` : ""}
              <br />
              <span className="font-mono text-xs">Chave {cancelNota.chave}</span>
              <br />
              Protocolo {cancelNota.protocolo || "não encontrado no XML"}
            </p>
          )}
          <textarea
            className="fiscal-input w-full"
            placeholder="Justificativa (mín. 15 caracteres)"
            value={justificativa}
            onChange={(e) => setJustificativa(e.target.value)}
          />
          <button
            type="button"
            className="fiscal-btn-primary"
            onClick={() =>
              run(
                () =>
                  api.nfeCancelar(token!, {
                    chave: cancelNota?.chave ?? "",
                    protocolo: cancelNota?.protocolo ?? "",
                    motivo: justificativa,
                    emitente: cancelNota?.empresaId ?? emitenteId,
                  }),
                cancelNota?.empresaId,
              )
            }
          >
            Cancelar NF-e
          </button>
        </section>

        <section className="space-y-3 rounded-xl border border-slate-200 p-4">
          <h2 className="font-medium">Carta de correção (CC-e)</h2>
          <AutoCompleteField
            id="nfe-cce-nota"
            label="Nota"
            placeholder="Nota de produção — número, série ou chave…"
            value={cceBusca}
            suggestions={cceSug}
            completeMethod={(e: AutoCompleteCompleteEvent) => {
              void buscarNotas(e.query).then(setCceSug);
            }}
            onChange={(v) => {
              const opt = asOption(v);
              if (opt) void escolherCarta(opt);
              else {
                setCceBusca(v);
                setCceNota(null);
              }
            }}
          />
          {cceNota && (
            <p className="text-sm text-slate-600">
              {cceNota.emitenteNome ? `${cceNota.emitenteNome} · ` : ""}
              nº {cceNota.numero} · série {cceNota.serie}
              <br />
              <span className="font-mono text-xs">Chave {cceNota.chave}</span>
            </p>
          )}
          <label className="block max-w-[8rem] text-sm">
            <span className="mb-1 block text-slate-500">Sequência</span>
            <input className="fiscal-input w-full" value={sequencial} onChange={(e) => setSequencial(e.target.value)} />
          </label>
          <textarea
            className="fiscal-input w-full"
            placeholder="Texto da correção"
            value={correcao}
            onChange={(e) => setCorrecao(e.target.value)}
          />
          <button
            type="button"
            className="fiscal-btn-primary"
            onClick={() =>
              run(
                () =>
                  api.nfeCartaCorrecao(token!, {
                    chave: cceNota?.chave ?? "",
                    texto: correcao,
                    sequencial: Number(sequencial) || 1,
                    emitente: cceNota?.empresaId ?? emitenteId,
                  }),
                cceNota?.empresaId,
              )
            }
          >
            Enviar CC-e
          </button>
        </section>

        <section className="space-y-3 rounded-xl border border-slate-200 p-4">
          <h2 className="font-medium">Inutilização de numeração</h2>
          <p className="text-sm text-slate-500">
            Escolha o emitente e a série já usada em produção. O próximo número sai da última nota de produção.
          </p>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <AutoCompleteField
              id="nfe-inut-serie"
              label="Série"
              placeholder={emitente ? "Série…" : "Selecione o emitente antes"}
              disabled={!emitente}
              value={serieBusca}
              suggestions={serieSug}
              forceSelection={false}
              completeMethod={(e: AutoCompleteCompleteEvent) => setSerieSug(seriesParaAc(e.query))}
              onChange={(v) => {
                const opt = asOption(v);
                if (opt) escolherSerie(opt);
                else if (typeof v === "string") escolherSerie(null, v);
                else escolherSerie(null, "");
              }}
            />
            <label className="block text-sm">
              <span className="mb-1.5 block text-sm font-medium text-slate-700">Ano</span>
              <input className="fiscal-input w-full" value={ano} onChange={(e) => setAno(e.target.value)} />
            </label>
            <label className="block text-sm">
              <span className="mb-1.5 block text-sm font-medium text-slate-700">Número inicial</span>
              <input className="fiscal-input w-full" value={numIni} onChange={(e) => setNumIni(e.target.value)} />
            </label>
            <label className="block text-sm">
              <span className="mb-1.5 block text-sm font-medium text-slate-700">Número final</span>
              <input className="fiscal-input w-full" value={numFim} onChange={(e) => setNumFim(e.target.value)} />
            </label>
          </div>
          {ultimoNumero != null && (
            <p className="text-sm text-slate-600">
              Último número emitido na série {serieInut}: {ultimoNumero}. A faixa acima começa no próximo.
            </p>
          )}
          <textarea
            className="fiscal-input w-full"
            placeholder="Justificativa (mín. 15 caracteres)"
            value={justificativaInut}
            onChange={(e) => setJustificativaInut(e.target.value)}
          />
          <button
            type="button"
            className="fiscal-btn-primary"
            onClick={() =>
              run(() =>
                api.nfeInutilizar(token!, {
                  ano: Number(ano) || undefined,
                  serie: serieInut,
                  numeroInicial: numIni,
                  numeroFinal: numFim,
                  justificativa: justificativaInut,
                  emitente: emitenteId,
                }),
              )
            }
          >
            Inutilizar faixa
          </button>
        </section>
      </div>
    </div>
  );
}
