"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { getAppToken } from "@/lib/app-session";

export function NfeEventosFiscais() {
  const token = getAppToken();
  const [chave, setChave] = useState("");
  const [justificativa, setJustificativa] = useState("");
  const [correcao, setCorrecao] = useState("");
  const [serie, setSerie] = useState("921");
  const [numIni, setNumIni] = useState("1");
  const [numFim, setNumFim] = useState("1");
  const [resultado, setResultado] = useState<string>("");
  const [erro, setErro] = useState("");

  const run = async (fn: () => Promise<unknown>) => {
    if (!token) return;
    setErro("");
    setResultado("");
    try {
      const res = await fn();
      setResultado(JSON.stringify(res, null, 2));
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Erro");
    }
  };

  return (
    <div className="fiscal-card space-y-6">
      <h1 className="text-2xl font-semibold text-slate-800">NF-e — Eventos fiscais</h1>

      <section className="space-y-2 rounded-xl border border-slate-200 p-4">
        <h2 className="font-medium">Cancelamento</h2>
        <input
          className="fiscal-input w-full"
          placeholder="Chave 44 dígitos"
          value={chave}
          onChange={(e) => setChave(e.target.value)}
        />
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
            run(() => api.nfeCancelar(token!, { chave, protocolo: "", motivo: justificativa }))
          }
        >
          Cancelar NF-e
        </button>
      </section>

      <section className="space-y-2 rounded-xl border border-slate-200 p-4">
        <h2 className="font-medium">Carta de correção (CC-e)</h2>
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
            run(() =>
              api.nfeCartaCorrecao(token!, {
                chave,
                texto: correcao,
                sequencial: 1,
              }),
            )
          }
        >
          Enviar CC-e
        </button>
      </section>

      <section className="space-y-2 rounded-xl border border-slate-200 p-4">
        <h2 className="font-medium">Inutilização de numeração</h2>
        <div className="flex flex-wrap gap-2">
          <input className="fiscal-input w-24" value={serie} onChange={(e) => setSerie(e.target.value)} />
          <input className="fiscal-input w-24" value={numIni} onChange={(e) => setNumIni(e.target.value)} />
          <input className="fiscal-input w-24" value={numFim} onChange={(e) => setNumFim(e.target.value)} />
        </div>
        <button
          type="button"
          className="fiscal-btn-primary"
          onClick={() =>
            run(() =>
              api.nfeInutilizar(token!, {
                serie,
                numeroInicial: numIni,
                numeroFinal: numFim,
                justificativa: justificativa || "Inutilizacao de numeracao conforme solicitado.",
              }),
            )
          }
        >
          Inutilizar faixa
        </button>
      </section>

      {erro && <p className="text-sm text-red-600">{erro}</p>}
      {resultado && (
        <pre className="overflow-x-auto rounded-lg bg-slate-900 p-4 text-xs text-green-300">{resultado}</pre>
      )}
    </div>
  );
}
