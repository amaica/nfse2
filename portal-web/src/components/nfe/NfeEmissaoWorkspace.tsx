"use client";

import { useEffect, useState } from "react";
import { api, ApiError, type NfeContexto } from "@/lib/api";
import { fiscalApi } from "@/lib/fiscal-api";
import { getAppToken } from "@/lib/app-session";

export function NfeEmissaoWorkspace() {
  const token = getAppToken();
  const [ctx, setCtx] = useState<NfeContexto | null>(null);
  const [operacoes, setOperacoes] = useState<Array<{ id: number; descricao: string }>>([]);
  const [produtos, setProdutos] = useState<Array<{ id: number; codigo: string; nome: string }>>([]);
  const [enderecoId, setEnderecoId] = useState<number | "">("");
  const [operacaoFiscalId, setOperacaoFiscalId] = useState<number | "">("");
  const [destNome, setDestNome] = useState("");
  const [destDoc, setDestDoc] = useState("");
  const [produtoId, setProdutoId] = useState<number | "">("");
  const [qtd, setQtd] = useState("1");
  const [valor, setValor] = useState("100.00");
  const [loading, setLoading] = useState(false);
  const [resultado, setResultado] = useState<Record<string, unknown> | null>(null);
  const [erro, setErro] = useState("");

  useEffect(() => {
    if (!token) return;
    api.nfeContexto(token).then(setCtx).catch(() => {});
    fiscalApi.operacoesSimples().then(setOperacoes).catch(() => {});
    fiscalApi.produtosSimples().then(setProdutos).catch(() => {});
  }, [token]);

  const emitir = async () => {
    if (!token) return;
    setLoading(true);
    setErro("");
    setResultado(null);
    try {
      const body = {
        enderecoId: enderecoId === "" ? undefined : Number(enderecoId),
        operacaoFiscalId: operacaoFiscalId === "" ? undefined : Number(operacaoFiscalId),
        sincrono: true,
        naturezaOperacao: "VENDA DE MERCADORIA",
        destinatario: destNome || destDoc
          ? { nome: destNome || "DESTINATARIO", documento: destDoc || undefined, email: undefined }
          : undefined,
        itens: [
          {
            produtoId: produtoId === "" ? undefined : Number(produtoId),
            codigo: "001",
            descricao: "PRODUTO NF-E",
            ncm: "61091000",
            cfop: "5102",
            unidade: "UN",
            quantidade: Number(qtd),
            valorUnitario: Number(valor),
            ibsCbs: { habilitar: true },
          },
        ],
      };
      const res = await api.nfeEnviarLote(token, body);
      setResultado(res as unknown as Record<string, unknown>);
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Falha na emissão");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fiscal-card space-y-4">
      <div>
        <h1 className="text-2xl font-semibold text-slate-800">Emissão NF-e</h1>
        <p className="mt-1 text-sm text-amber-800 rounded-lg bg-amber-50 px-3 py-2 mt-2">
          Reforma Tributária: IBS e CBS incluídos automaticamente (alíquota teste 1%). Obrigatório
          em todos os DF-e a partir de <strong>03/08/2026</strong>.
        </p>
      </div>

      {ctx && !ctx.podeEmitir && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{ctx.aviso ?? "Emissão bloqueada"}</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <label className="block text-sm">
          <span className="mb-1 block font-medium">Endereço / IE</span>
          <select
            className="fiscal-input w-full"
            value={enderecoId}
            onChange={(e) => setEnderecoId(e.target.value ? Number(e.target.value) : "")}
          >
            <option value="">Principal</option>
            {ctx?.enderecos?.map((e) => (
              <option key={e.id} value={e.id}>
                {e.apelido} — IE {e.inscricaoEstadual ?? "—"} (série {e.serieNfe})
              </option>
            ))}
          </select>
        </label>

        <label className="block text-sm">
          <span className="mb-1 block font-medium">Operação fiscal (IBS/CBS)</span>
          <select
            className="fiscal-input w-full"
            value={operacaoFiscalId}
            onChange={(e) => setOperacaoFiscalId(e.target.value ? Number(e.target.value) : "")}
          >
            <option value="">Padrão portal (1% teste)</option>
            {operacoes.map((o) => (
              <option key={o.id} value={o.id}>
                {o.descricao}
              </option>
            ))}
          </select>
        </label>

        <label className="block text-sm">
          <span className="mb-1 block font-medium">Produto cadastrado</span>
          <select
            className="fiscal-input w-full"
            value={produtoId}
            onChange={(e) => setProdutoId(e.target.value ? Number(e.target.value) : "")}
          >
            <option value="">Manual</option>
            {produtos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.codigo} — {p.nome}
              </option>
            ))}
          </select>
        </label>

        <label className="block text-sm">
          <span className="mb-1 block font-medium">Destinatário</span>
          <input
            className="fiscal-input w-full"
            placeholder="Nome"
            value={destNome}
            onChange={(e) => setDestNome(e.target.value)}
          />
        </label>

        <label className="block text-sm">
          <span className="mb-1 block font-medium">CPF/CNPJ destinatário</span>
          <input
            className="fiscal-input w-full"
            value={destDoc}
            onChange={(e) => setDestDoc(e.target.value)}
          />
        </label>

        <label className="block text-sm">
          <span className="mb-1 block font-medium">Qtd / Valor unit.</span>
          <div className="flex gap-2">
            <input className="fiscal-input w-20" value={qtd} onChange={(e) => setQtd(e.target.value)} />
            <input className="fiscal-input flex-1" value={valor} onChange={(e) => setValor(e.target.value)} />
          </div>
        </label>
      </div>

      {erro && <p className="text-sm text-red-600">{erro}</p>}

      <button
        type="button"
        className="fiscal-btn-primary"
        disabled={loading || (ctx != null && !ctx.podeEmitir)}
        onClick={emitir}
      >
        {loading ? "Transmitindo…" : "Emitir NF-e"}
      </button>

      {resultado && (
        <div className="rounded-lg border border-green-200 bg-green-50 p-4 text-sm">
          <p>
            <strong>Status:</strong> {String(resultado.statusProtocolo ?? resultado.status)}
          </p>
          <p>
            <strong>Chave:</strong> {String(resultado.chaveAcesso ?? "—")}
          </p>
          <p>
            <strong>Protocolo:</strong> {String(resultado.protocolo ?? "—")}
          </p>
          <p className="text-slate-600">{String(resultado.motivoProtocolo ?? resultado.motivo ?? "")}</p>
        </div>
      )}
    </div>
  );
}
