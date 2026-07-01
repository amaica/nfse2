"use client";

import { useState } from "react";
import { type EmpresaDetalhe } from "@/lib/api";
import { codigoUfIbge } from "./uf-ibge";

export type EnderecoLinha = {
  id?: number;
  municipio: string;
  bairro: string;
  cep: string;
  inscricaoEstadual: string;
  codigoMunicipioIbge: string;
  uf: string;
  logradouro: string;
  numero: string;
  apelido: string;
  serieNfe: string;
  ultimoNumeroNfe: string;
  principal: boolean;
  ativo: boolean;
};

export type EmpresaFormData = {
  nome: string;
  cpfCnpj: string;
  crt: string;
  ambiente: string;
  tipoEmitente: string;
  email: string;
  modelo: string;
  senhaIntegracao: string;
  prefeitura: string;
  codigoMunicipioIbge: string;
  serieRps: string;
  ultimoNumeroNfse: string;
  enderecos: EnderecoLinha[];
};

export function serieNfePadrao(cpfCnpj: string): string {
  const doc = cpfCnpj.replace(/\D/g, "");
  return doc.length === 11 ? "921" : "1";
}

export function enderecoVazio(partial?: Partial<EnderecoLinha>): EnderecoLinha {
  return {
    municipio: "",
    bairro: "",
    cep: "",
    inscricaoEstadual: "",
    codigoMunicipioIbge: "",
    uf: "RS",
    logradouro: "",
    numero: "",
    apelido: "Matriz",
    serieNfe: partial?.serieNfe ?? "1",
    ultimoNumeroNfe: "0",
    principal: true,
    ativo: true,
    ...partial,
  };
}

export function detalheParaForm(det: EmpresaDetalhe): EmpresaFormData {
  const ends: EnderecoLinha[] =
    det.enderecos?.map((e) => ({
      id: e.id,
      municipio: e.municipio ?? "",
      bairro: e.bairro ?? "",
      cep: e.cep ?? "",
      inscricaoEstadual: e.inscricaoEstadual ?? "",
      codigoMunicipioIbge: e.codigoMunicipioIbge ?? "",
      uf: e.uf ?? "RS",
      logradouro: e.logradouro ?? "",
      numero: e.numero ?? "",
      apelido: e.apelido ?? "Matriz",
      serieNfe: e.serieNfe ?? "1",
      ultimoNumeroNfe: String(e.ultimoNumeroNfe ?? 0),
      principal: e.principal ?? false,
      ativo: e.ativo ?? true,
    })) ?? [];

  if (ends.length === 0) {
    ends.push(
      enderecoVazio({
        cep: det.endereco?.cep ?? "",
        bairro: det.endereco?.bairro ?? "",
        logradouro: det.endereco?.logradouro ?? "",
        numero: det.endereco?.numero ?? "",
        municipio: det.municipio ?? "",
        uf: det.uf ?? "RS",
        inscricaoEstadual: det.inscricaoEstadual ?? "",
        codigoMunicipioIbge: det.codigoMunicipioIbge ?? "",
        principal: true,
      }),
    );
  }

  return {
    nome: det.nome ?? "",
    cpfCnpj: det.cnpj ?? "",
    crt: det.optanteSimples ? "1" : "3",
    ambiente: det.ambiente ?? "homologacao",
    tipoEmitente: "normal",
    email: det.email ?? det.emailIntegracao ?? "",
    modelo: "NFE",
    senhaIntegracao: "",
    prefeitura: det.prefeitura ?? det.municipio ?? "",
    codigoMunicipioIbge: det.codigoMunicipioIbge ?? ends[0]?.codigoMunicipioIbge ?? "",
    serieRps: det.serieRps ?? "1",
    ultimoNumeroNfse: String(det.ultimoNumeroNfse ?? 0),
    enderecos: ends,
  };
}

export function formInicialVazio(): EmpresaFormData {
  return {
    nome: "",
    cpfCnpj: "",
    crt: "3",
    ambiente: "homologacao",
    tipoEmitente: "normal",
    email: "",
    modelo: "NFE",
    senhaIntegracao: "demo123",
    prefeitura: "",
    codigoMunicipioIbge: "",
    serieRps: "1",
    ultimoNumeroNfse: "0",
    enderecos: [enderecoVazio()],
  };
}

type Props = {
  titulo: string;
  form: EmpresaFormData;
  empresaId: number | null;
  carregando: boolean;
  certNome?: string;
  logoNome?: string;
  mensagem?: { tipo: "ok" | "erro"; texto: string } | null;
  onChange: (form: EmpresaFormData) => void;
  onSalvar: (e: React.FormEvent) => void;
  onVoltar: () => void;
  onCertificado: (arquivo: File | null, senha: string) => void;
  onLogo: (arquivo: File | null) => void;
};

const inputCls =
  "mt-0.5 w-full rounded border border-slate-300 bg-white px-2 py-1.5 text-sm text-slate-800";
const labelCls = "text-xs font-medium text-slate-600";

export function EmpresaFormulario({
  titulo,
  form,
  empresaId,
  carregando,
  certNome,
  logoNome,
  mensagem,
  onChange,
  onSalvar,
  onVoltar,
  onCertificado,
  onLogo,
}: Props) {
  const [certArquivo, setCertArquivo] = useState<File | null>(null);
  const [certSenha, setCertSenha] = useState("");
  const [abaCert, setAbaCert] = useState<"logo" | "cert">("cert");
  const [paginaEnd, setPaginaEnd] = useState(1);
  const porPagina = 5;
  const totalEnd = Math.max(1, Math.ceil(form.enderecos.length / porPagina));
  const endsPagina = form.enderecos.slice((paginaEnd - 1) * porPagina, paginaEnd * porPagina);

  function atualizarEndereco(idxGlobal: number, patch: Partial<EnderecoLinha>) {
    const next = form.enderecos.map((e, i) => (i === idxGlobal ? { ...e, ...patch } : e));
    onChange({ ...form, enderecos: next });
  }

  function adicionarEndereco() {
    onChange({
      ...form,
      enderecos: [...form.enderecos, enderecoVazio({ apelido: `Filial ${form.enderecos.length + 1}`, principal: false })],
    });
  }

  function removerEndereco(idx: number) {
    if (form.enderecos.length <= 1) return;
    onChange({ ...form, enderecos: form.enderecos.filter((_, i) => i !== idx) });
  }

  return (
    <form onSubmit={onSalvar} className="rounded border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h1 className="text-base font-medium text-slate-800">{titulo}</h1>
        <button type="button" onClick={onVoltar} className="text-sm text-slate-500 hover:text-slate-800">
          ← Voltar à listagem
        </button>
      </div>

      <div className="grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
        <label className={labelCls}>
          Nome *
          <input
            required
            className={inputCls}
            value={form.nome}
            onChange={(e) => onChange({ ...form, nome: e.target.value })}
          />
        </label>
        <label className={labelCls}>
          CpfCnpj
          <input
            required
            className={inputCls}
            value={form.cpfCnpj}
            onChange={(e) => {
              const cpfCnpj = e.target.value;
              const doc = cpfCnpj.replace(/\D/g, "");
              const seriePf = doc.length === 11 ? "921" : null;
              onChange({
                ...form,
                cpfCnpj,
                enderecos: seriePf
                  ? form.enderecos.map((end) =>
                      end.serieNfe === "1" || !end.serieNfe ? { ...end, serieNfe: seriePf } : end
                    )
                  : form.enderecos,
              });
            }}
            placeholder="CPF ou CNPJ"
            readOnly={empresaId != null}
          />
        </label>
        <label className={labelCls}>
          Crt
          <select
            className={inputCls}
            value={form.crt}
            onChange={(e) => onChange({ ...form, crt: e.target.value })}
          >
            <option value="1">1 - Simples Nacional</option>
            <option value="2">2 - Simples excesso sublimite</option>
            <option value="3">3 - Regime Normal</option>
          </select>
        </label>
        <label className={labelCls}>
          Ambiente
          <select
            className={inputCls}
            value={form.ambiente}
            onChange={(e) => onChange({ ...form, ambiente: e.target.value })}
          >
            <option value="homologacao">Homologação</option>
            <option value="producao">Produção</option>
          </select>
        </label>
        <label className={labelCls}>
          Tipo Emitente
          <select
            className={inputCls}
            value={form.tipoEmitente}
            onChange={(e) => onChange({ ...form, tipoEmitente: e.target.value })}
          >
            <option value="normal">Normal</option>
          </select>
        </label>
        <label className={labelCls}>
          Email
          <input
            type="email"
            className={inputCls}
            value={form.email}
            onChange={(e) => onChange({ ...form, email: e.target.value })}
          />
        </label>
        <label className={labelCls}>
          Modelo
          <select
            className={inputCls}
            value={form.modelo}
            onChange={(e) => onChange({ ...form, modelo: e.target.value })}
          >
            <option value="NFE">Nota Fiscal Eletrônica - NFe</option>
            <option value="NFCE">Nota Fiscal de Consumidor - NFCe</option>
          </select>
        </label>
        {empresaId == null && (
          <label className={labelCls}>
            Senha integração (login portal)
            <input
              type="password"
              required
              className={inputCls}
              value={form.senhaIntegracao}
              onChange={(e) => onChange({ ...form, senhaIntegracao: e.target.value })}
            />
          </label>
        )}
      </div>

      <div className="border-t border-slate-200 px-4 pt-3">
        <div className="mb-2 flex gap-4 border-b border-slate-200 text-sm">
          <button
            type="button"
            onClick={() => setAbaCert("logo")}
            className={`border-b-2 px-2 py-1 ${abaCert === "logo" ? "border-blue-600 font-medium text-blue-700" : "border-transparent text-slate-400"}`}
          >
            Logo
          </button>
          <button
            type="button"
            onClick={() => setAbaCert("cert")}
            className={`border-b-2 px-2 py-1 ${abaCert === "cert" ? "border-blue-600 font-medium text-blue-700" : "border-transparent text-slate-400"}`}
          >
            Certificação
          </button>
        </div>
        {abaCert === "logo" ? (
          <div className="grid gap-3 pb-4 sm:grid-cols-2">
            <label className={labelCls}>
              Logo DANFE (PNG/JPG)
              {logoNome && (
                <span className="ml-2 text-slate-500">— {logoNome}</span>
              )}
              <input
                type="file"
                accept="image/png,image/jpeg,image/gif"
                className="mt-1 block w-full text-sm"
                onChange={(e) => {
                  const f = e.target.files?.[0] ?? null;
                  onLogo(f);
                }}
              />
            </label>
            <p className="text-xs text-slate-500 sm:col-span-2">
              A imagem aparece no canto superior do DANFE (biblioteca java-danfe).
            </p>
          </div>
        ) : (
        <div className="grid gap-3 pb-4 sm:grid-cols-2">
          <label className={labelCls}>
            Certificado (A1)
            {certNome && !certArquivo && (
              <span className="ml-2 text-slate-500">— {certNome}</span>
            )}
            <input
              type="file"
              accept=".pfx,.p12"
              className="mt-1 block w-full text-sm"
              onChange={(e) => {
                const f = e.target.files?.[0] ?? null;
                setCertArquivo(f);
                onCertificado(f, certSenha);
              }}
            />
          </label>
          <label className={labelCls}>
            Setar Senha
            <input
              type="password"
              className={inputCls}
              value={certSenha}
              onChange={(e) => {
                setCertSenha(e.target.value);
                onCertificado(certArquivo, e.target.value);
              }}
              placeholder="Senha do PFX"
            />
          </label>
        </div>
        )}
      </div>

      <div className="border-t border-slate-200">
        <div className="flex flex-wrap items-center gap-2 bg-slate-700 px-3 py-2 text-white">
          <button
            type="button"
            onClick={adicionarEndereco}
            className="flex h-8 w-8 items-center justify-center rounded bg-blue-600 text-lg font-bold hover:bg-blue-500"
          >
            +
          </button>
          <span className="text-sm text-slate-300">
            ({paginaEnd} of {totalEnd})
          </span>
          <button type="button" disabled={paginaEnd <= 1} onClick={() => setPaginaEnd(1)} className="px-1 disabled:opacity-40">«</button>
          <button type="button" disabled={paginaEnd <= 1} onClick={() => setPaginaEnd((p) => p - 1)} className="px-1 disabled:opacity-40">‹</button>
          <span className="rounded bg-slate-500 px-2 py-0.5 text-sm">{paginaEnd}</span>
          <button type="button" disabled={paginaEnd >= totalEnd} onClick={() => setPaginaEnd((p) => p + 1)} className="px-1 disabled:opacity-40">›</button>
          <button type="button" disabled={paginaEnd >= totalEnd} onClick={() => setPaginaEnd(totalEnd)} className="px-1 disabled:opacity-40">»</button>
          <select className="ml-2 rounded bg-slate-600 px-2 py-1 text-sm" value={5} disabled>
            <option value={5}>5</option>
          </select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-xs">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-center text-slate-600">
                <th className="px-2 py-2">Cidade</th>
                <th className="px-2 py-2">Bairro</th>
                <th className="px-2 py-2">Cep</th>
                <th className="px-2 py-2">Ie</th>
                <th className="px-2 py-2">Cod Ibge Municipio</th>
                <th className="px-2 py-2">UF</th>
                <th className="px-2 py-2">Cod Uf</th>
                <th className="px-2 py-2">Logradouro</th>
                <th className="w-20 px-2 py-2">Ações</th>
              </tr>
            </thead>
            <tbody>
              {endsPagina.map((end, i) => {
                const idx = (paginaEnd - 1) * porPagina + i;
                return (
                  <tr key={idx} className="border-b border-slate-100">
                    <td className="p-1">
                      <input className={inputCls} value={end.municipio} onChange={(e) => atualizarEndereco(idx, { municipio: e.target.value })} />
                    </td>
                    <td className="p-1">
                      <input className={inputCls} value={end.bairro} onChange={(e) => atualizarEndereco(idx, { bairro: e.target.value })} />
                    </td>
                    <td className="p-1">
                      <input className={inputCls} value={end.cep} onChange={(e) => atualizarEndereco(idx, { cep: e.target.value })} />
                    </td>
                    <td className="p-1">
                      <input className={inputCls} value={end.inscricaoEstadual} onChange={(e) => atualizarEndereco(idx, { inscricaoEstadual: e.target.value })} />
                    </td>
                    <td className="p-1">
                      <input className={inputCls} value={end.codigoMunicipioIbge} onChange={(e) => atualizarEndereco(idx, { codigoMunicipioIbge: e.target.value })} />
                    </td>
                    <td className="p-1">
                      <input
                        className={inputCls}
                        value={end.uf}
                        onChange={(e) => atualizarEndereco(idx, { uf: e.target.value.toUpperCase() })}
                        placeholder="UF"
                        maxLength={2}
                        title="UF"
                      />
                    </td>
                    <td className="p-1">
                      <input
                        className={`${inputCls} bg-slate-50`}
                        value={codigoUfIbge(end.uf)}
                        readOnly
                      />
                    </td>
                    <td className="p-1">
                      <input className={inputCls} value={end.logradouro} onChange={(e) => atualizarEndereco(idx, { logradouro: e.target.value })} />
                    </td>
                    <td className="p-1 text-center">
                      <button
                        type="button"
                        onClick={() => removerEndereco(idx)}
                        className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-blue-600 text-white hover:bg-blue-500"
                        title="Remover"
                      >
                        🗑
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3 border-t border-slate-200 px-4 py-4">
        <button
          type="submit"
          disabled={carregando}
          className="rounded bg-blue-600 px-6 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:opacity-50"
        >
          {carregando ? "Salvando…" : "Salvar"}
        </button>
        {mensagem && (
          <p className={`text-sm ${mensagem.tipo === "ok" ? "text-emerald-700" : "text-red-600"}`}>
            {mensagem.texto}
          </p>
        )}
      </div>
    </form>
  );
}
