"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import type { AutoCompleteCompleteEvent } from "primereact/autocomplete";
import { CheckCircle, ChevronDown, Loader2 } from "lucide-react";
import { api, ApiError, formatarCnpjCpf, type EmissaoContexto, type EmissaoSucesso } from "@/lib/api";
import { fiscalApi } from "@/lib/fiscal-api";
import { mapEmissaoError } from "@/lib/assinatura";
import { criarFormularioInicial, formParaPayload, recalcularValores } from "@/lib/form-defaults";
import { imprimirDanfseNfse } from "@/lib/imprimir-danfse-nfse";
import { formatBrNumber } from "@/lib/number-mask";
import { ISS_RETIDO, SIMPLES_NACIONAL, TRIBUTACAO_ISSQN } from "@/lib/nfse-servico-opcoes";
import { AutoCompleteField, type AcOption } from "@/components/ui/AutoCompleteField";
import { CertificadoAmbienteBar } from "@/components/nfse/CertificadoAmbienteBar";
import { InputField } from "./agrow/InputField";
import { Label } from "./agrow/Label";
import { MaskedNumberInput } from "./agrow/MaskedNumberInput";
import { Select } from "./agrow/Select";
import { TextArea } from "./agrow/TextArea";

type FormState = {
  prestadorCnpj: string;
  prestadorNome: string;
  prestadorIm: string;
  tomadorDocumento: string;
  tomadorNome: string;
  tomadorEmail: string;
  tomadorTelefone: string;
  servicoTributId: string;
  itemListaServico: string;
  codigoTributacaoMunicipio: string;
  cnae: string;
  nbs: string;
  municipioIbge: string;
  descricaoServico: string;
  valorServicos: number | null;
  aliquotaIss: number | null;
  tributacaoIssqn: string;
  issRetido: string;
  simplesNacional: string;
  serieRps: string;
  observacoes: string;
};

const EMPTY: FormState = {
  prestadorCnpj: "",
  prestadorNome: "",
  prestadorIm: "",
  tomadorDocumento: "",
  tomadorNome: "",
  tomadorEmail: "",
  tomadorTelefone: "",
  servicoTributId: "",
  itemListaServico: "",
  codigoTributacaoMunicipio: "",
  cnae: "",
  nbs: "",
  municipioIbge: "",
  descricaoServico: "",
  valorServicos: null,
  aliquotaIss: 2,
  tributacaoIssqn: "1",
  issRetido: "1",
  simplesNacional: "1",
  serieRps: "1",
  observacoes: "",
};

function moneyBr(v: number | null | undefined) {
  if (v == null || !Number.isFinite(v)) return "R$ 0,00";
  return v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function asOption(v: AcOption | string | null): AcOption | null {
  if (v == null) return null;
  if (typeof v === "string") return { label: v, value: v.replace(/\D/g, "") };
  return v;
}

export function NfseEmissaoAgrow({ token }: { token: string }) {
  const [ctx, setCtx] = useState<EmissaoContexto | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [cliente, setCliente] = useState<AcOption | string | null>(null);
  const [clienteSug, setClienteSug] = useState<AcOption[]>([]);
  const [buscandoTomador, setBuscandoTomador] = useState(false);
  const [avancado, setAvancado] = useState(false);
  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState<EmissaoSucesso | null>(null);

  const aplicarTomador = useCallback(
    async (
      doc: string,
      dados?: { razaoSocial?: string; email?: string; telefone?: string },
    ) => {
      setBuscandoTomador(true);
      try {
        setForm((f) => ({
          ...f,
          tomadorDocumento: doc,
          tomadorNome: dados?.razaoSocial ?? f.tomadorNome,
          tomadorEmail: dados?.email ?? f.tomadorEmail,
          tomadorTelefone: dados?.telefone ?? f.tomadorTelefone,
        }));
        setCliente({
          label: dados?.razaoSocial || formatarCnpjCpf(doc),
          value: doc,
          meta: formatarCnpjCpf(doc),
        });
      } finally {
        setBuscandoTomador(false);
      }
    },
    [],
  );

  const buscarCliente = useCallback(async (event: AutoCompleteCompleteEvent) => {
    const q = event.query?.trim() ?? "";
    if (q.length < 2) {
      setClienteSug([]);
      return;
    }
    try {
      const pessoas = await fiscalApi.buscaPessoas(q);
      setClienteSug(
        pessoas.slice(0, 50).map((p) => {
          const doc = (p.cpfCnpj || "").replace(/\D/g, "");
          return {
            label: p.nome || formatarCnpjCpf(doc),
            value: doc || String(p.id),
            meta: formatarCnpjCpf(doc) || undefined,
            raw: { id: p.id, documento: doc, razaoSocial: p.nome },
          };
        }),
      );
    } catch {
      setClienteSug([]);
    }
  }, []);

  const selecionarCliente = useCallback(
    async (opt: AcOption) => {
      setCliente(opt);
      const doc = String(opt.value ?? "").replace(/\D/g, "");
      const raw = opt.raw as { id?: number; razaoSocial?: string } | undefined;
      if (raw?.id) {
        try {
          const full = await fiscalApi.get<{
            nome?: string;
            email?: string;
            celular?: string;
            contato?: string;
            cpfCnpj?: string;
          }>("/api/pessoas", raw.id);
          await aplicarTomador((full.cpfCnpj || doc).replace(/\D/g, ""), {
            razaoSocial: full.nome || opt.label,
            email: full.email,
            telefone: full.celular || full.contato,
          });
          return;
        } catch {
          /* fallback */
        }
      }
      await aplicarTomador(doc, { razaoSocial: raw?.razaoSocial || opt.label });
    },
    [aplicarTomador],
  );

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro("");
    try {
      const c = await api.emissaoContexto(token);
      setCtx(c);
      const principal =
        c.operacoesNfse?.find((s) => s.principal) ?? c.operacoesNfse?.[0] ?? null;
      setForm({
        ...EMPTY,
        prestadorCnpj: c.prestadorDocumento ?? "",
        prestadorNome: c.prestadorNome ?? c.empresaNome ?? "",
        tomadorDocumento: "",
        tomadorNome: "",
        servicoTributId: principal ? String(principal.id) : "",
        itemListaServico: principal?.itemListaServico ?? c.codigoServicoPadrao ?? "",
        nbs: principal?.nbs ?? "",
        descricaoServico: principal?.descricao ?? c.descricaoServicoPadrao ?? "",
        aliquotaIss:
          principal?.aliquotaIss != null
            ? Number(principal.aliquotaIss)
            : c.aliquotaPadraoPercentual != null
              ? Number(c.aliquotaPadraoPercentual)
              : 2,
        municipioIbge: c.codigoMunicipioIbge ?? "",
      });
      setCliente(null);
      if (principal) {
        try {
          const full = await api.obterTributNfseServico(token, principal.id);
          setForm((f) => ({
            ...f,
            codigoTributacaoMunicipio: full.codigoTributacaoMunicipio ?? f.codigoTributacaoMunicipio,
            cnae: full.cnae ?? f.cnae,
            nbs: full.nbs ?? f.nbs,
            descricaoServico: full.descricaoServico || full.descricao || f.descricaoServico,
            municipioIbge: full.municipioPrestacaoIbge ?? f.municipioIbge,
            aliquotaIss: full.aliquotaIss != null ? Number(full.aliquotaIss) : f.aliquotaIss,
            tributacaoIssqn: full.tributacaoIssqn ?? f.tributacaoIssqn,
            issRetido: full.issRetido ?? f.issRetido,
            simplesNacional: full.simplesNacional ?? f.simplesNacional,
          }));
        } catch {
          /* usa resumo do contexto */
        }
      }
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : "Falha ao carregar contexto de emissão");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const servicoOptions = useMemo(
    () =>
      (ctx?.operacoesNfse ?? []).map((s) => ({
        value: String(s.id),
        label: `${s.itemListaServico ?? "—"} · ${s.descricao ?? s.id}`,
      })),
    [ctx],
  );

  const aplicarServico = async (idStr: string) => {
    const resumo = ctx?.operacoesNfse?.find((x) => String(x.id) === idStr);
    setForm((f) => ({
      ...f,
      servicoTributId: idStr,
      itemListaServico: resumo?.itemListaServico ?? f.itemListaServico,
      nbs: resumo?.nbs ?? f.nbs,
      descricaoServico: resumo?.descricao ?? f.descricaoServico,
      aliquotaIss: resumo?.aliquotaIss != null ? Number(resumo.aliquotaIss) : f.aliquotaIss,
    }));
    if (!idStr) return;
    try {
      const s = await api.obterTributNfseServico(token, Number(idStr));
      setForm((f) => ({
        ...f,
        itemListaServico: s.itemListaServico ?? f.itemListaServico,
        codigoTributacaoMunicipio: s.codigoTributacaoMunicipio ?? f.codigoTributacaoMunicipio,
        cnae: s.cnae ?? f.cnae,
        nbs: s.nbs ?? f.nbs,
        municipioIbge: s.municipioPrestacaoIbge ?? f.municipioIbge,
        descricaoServico: s.descricaoServico || s.descricao || f.descricaoServico,
        aliquotaIss: s.aliquotaIss != null ? Number(s.aliquotaIss) : f.aliquotaIss,
        tributacaoIssqn: s.tributacaoIssqn ?? f.tributacaoIssqn,
        issRetido: s.issRetido ?? f.issRetido,
        simplesNacional: s.simplesNacional ?? f.simplesNacional,
      }));
    } catch {
      /* mantém resumo */
    }
  };

  const valorIss = useMemo(() => {
    const v = form.valorServicos ?? 0;
    const a = form.aliquotaIss ?? 0;
    return (v * a) / 100;
  }, [form.aliquotaIss, form.valorServicos]);

  const podeEmitir =
    !!form.tomadorDocumento &&
    !!form.tomadorNome &&
    !!form.descricaoServico.trim() &&
    (form.valorServicos ?? 0) > 0 &&
    ctx?.podeEmitir !== false;

  const montarPayload = () => {
    const base = criarFormularioInicial(ctx);
    const merged = recalcularValores({
      ...base,
      identificacao: { ...base.identificacao, serieRps: form.serieRps || "1" },
      regime: {
        ...base.regime,
        tributacaoIssqn: form.tributacaoIssqn,
        issRetido: form.issRetido,
        simplesNacional: form.simplesNacional,
      },
      prestador: {
        ...base.prestador,
        cnpj: form.prestadorCnpj,
        razaoSocial: form.prestadorNome,
        inscricaoMunicipal: form.prestadorIm,
      },
      tomador: {
        documento: form.tomadorDocumento,
        razaoSocial: form.tomadorNome,
        email: form.tomadorEmail,
        telefone: form.tomadorTelefone,
        inscricaoEstadual: "",
        inscricaoMunicipal: "",
      },
      classificacao: { atividadePrincipal: form.codigoTributacaoMunicipio },
      servico: {
        itemListaServico: form.itemListaServico,
        codigoTributacaoMunicipio: form.codigoTributacaoMunicipio,
        cnae: form.cnae,
        nbs: form.nbs,
        descricaoServico: form.descricaoServico,
        municipioIncidencia: form.municipioIbge || ctx?.codigoMunicipioIbge || "",
        municipioPrestacao: form.municipioIbge || ctx?.codigoMunicipioIbge || "",
        localPrestacao: form.municipioIbge || ctx?.codigoMunicipioIbge || "",
      },
      valores: {
        ...base.valores,
        valorServicos: form.valorServicos != null ? String(form.valorServicos) : "",
        aliquota: form.aliquotaIss != null ? String(form.aliquotaIss) : "",
      },
      informacoesAdicionais: {
        observacoes: form.observacoes,
        informacoesComplementares: "",
      },
    });
    return formParaPayload(merged);
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.tomadorDocumento) {
      setErro("Selecione o tomador (CPF/CNPJ).");
      return;
    }
    if (!form.descricaoServico.trim()) {
      setErro("Informe a descrição do serviço.");
      return;
    }
    if ((form.valorServicos ?? 0) <= 0) {
      setErro("Informe o valor dos serviços.");
      return;
    }
    setSalvando(true);
    setErro("");
    setSucesso(null);
    try {
      const nota = await api.emitir(token, montarPayload());
      setSucesso(nota);
    } catch (err) {
      setErro(mapEmissaoError(err instanceof ApiError ? err.message : "Falha ao emitir NFS-e"));
    } finally {
      setSalvando(false);
    }
  };

  if (loading) {
    return (
      <p className="flex items-center justify-center gap-2 p-8 text-sm text-slate-500">
        <Loader2 className="h-4 w-4 animate-spin" /> Carregando emissão…
      </p>
    );
  }

  if (sucesso) {
    const chave = sucesso.chaveAcesso;
    const imprimirPdf = async () => {
      setErro("");
      try {
        await api.imprimirPdf(token, chave);
      } catch {
        try {
          imprimirDanfseNfse({
            chave,
            status: "EMITIDA",
            serieRps: form.serieRps,
            prestadorCnpj: form.prestadorCnpj,
            prestadorNome: form.prestadorNome,
            prestadorIm: form.prestadorIm,
            tomadorDocumento: form.tomadorDocumento,
            tomadorNome: form.tomadorNome,
            tomadorEmail: form.tomadorEmail,
            tomadorTelefone: form.tomadorTelefone,
            descricaoServico: form.descricaoServico,
            itemListaServico: form.itemListaServico,
            codigoTributacaoMunicipio: form.codigoTributacaoMunicipio,
            municipioIbge: form.municipioIbge,
            valorServicos: form.valorServicos,
            valorIss,
            aliquotaIss: form.aliquotaIss,
            observacoes: form.observacoes,
            criadoEm: sucesso.processadoEm,
          });
        } catch (e) {
          setErro(e instanceof Error ? e.message : "Não foi possível gerar o PDF.");
        }
      }
    };

    return (
      <div className="mx-auto max-w-xl rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center">
        <CheckCircle className="mx-auto mb-3 h-10 w-10 text-emerald-600" />
        <h2 className="text-xl font-semibold text-emerald-900">NFS-e registrada</h2>
        <p className="mt-2 text-sm text-emerald-800">
          Valor {moneyBr(form.valorServicos)} · emitida com sucesso
        </p>
        <p className="mt-1 break-all font-mono text-[11px] text-emerald-700/80">{chave}</p>
        {erro ? <p className="mt-3 text-sm text-red-600">{erro}</p> : null}
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button
            type="button"
            onClick={() => void imprimirPdf()}
            className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
          >
            Gerar PDF
          </button>
          <Link href="/nfse/emitidas" className="rounded-lg border border-emerald-300 px-4 py-2 text-sm">
            Ver emitidas
          </Link>
          <button
            type="button"
            onClick={() => {
              setSucesso(null);
              setErro("");
              void carregar();
            }}
            className="rounded-lg border border-emerald-300 px-4 py-2 text-sm"
          >
            Nova emissão
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="nfse-emissao-agrow w-full space-y-4">
      <CertificadoAmbienteBar ctx={ctx} />

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Emissão de NFS-e</h1>
          <p className="mt-1 text-sm text-slate-500">
            Prestador = empresa do emitente · selecione o tomador e confirme o valor.
            {ctx?.prefeitura ? ` · ${ctx.prefeitura}` : ""}
          </p>
        </div>
        <Link
          href="/nfse/emitidas"
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm hover:bg-slate-50"
        >
          Ver emitidas
        </Link>
      </div>

      {ctx && !ctx.podeEmitir && ctx.aviso ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-800">
          {ctx.aviso}
        </div>
      ) : null}

      {erro ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">{erro}</div>
      ) : null}

      <form onSubmit={(e) => void onSubmit(e)} className="space-y-4">
        <section className="rounded-2xl border border-slate-200 bg-gradient-to-br from-slate-50 to-white p-5">
          <div className="mb-3 flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Prestador (emitente)</p>
              <h2 className="mt-0.5 text-lg font-semibold text-slate-900">{form.prestadorNome || "Empresa não configurada"}</h2>
              <p className="mt-0.5 text-sm tabular-nums text-slate-500">
                {formatarCnpjCpf(form.prestadorCnpj) || "—"}
                {form.prestadorIm ? ` · IM ${form.prestadorIm}` : ""}
              </p>
            </div>
            <div className="w-28">
              <Label>Série RPS</Label>
              <InputField
                value={form.serieRps}
                onChange={(e) => setForm((f) => ({ ...f, serieRps: e.target.value }))}
              />
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <div className="mb-4">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">Tomador</h2>
            <p className="mt-1 text-sm text-slate-500">Digite nome ou CPF/CNPJ — lista os clientes cadastrados.</p>
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="md:col-span-2">
              <AutoCompleteField
                id="nfse-tomador"
                label="CPF/CNPJ Tomador"
                placeholder="Nome, CPF ou CNPJ…"
                value={cliente}
                suggestions={clienteSug}
                completeMethod={buscarCliente}
                dropdown
                onChange={(v) => {
                  const opt = asOption(v);
                  if (opt && typeof v !== "string") {
                    void selecionarCliente(opt);
                    return;
                  }
                  setCliente(opt);
                  if (typeof v === "string") {
                    const digits = v.replace(/\D/g, "");
                    setForm((f) => ({
                      ...f,
                      tomadorDocumento: digits,
                      tomadorNome: digits.length === 11 || digits.length === 14 ? f.tomadorNome : v,
                    }));
                    if (digits.length === 11 || digits.length === 14) void aplicarTomador(digits);
                  }
                }}
              />
              {buscandoTomador ? (
                <p className="mt-1 flex items-center gap-2 text-sm text-slate-500">
                  <Loader2 className="h-4 w-4 animate-spin" /> Buscando dados do cliente…
                </p>
              ) : null}
            </div>
            {form.tomadorNome ? (
              <div className="md:col-span-2 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-emerald-100 bg-emerald-50/80 px-4 py-3">
                <div>
                  <strong className="block text-emerald-900">{form.tomadorNome}</strong>
                  <span className="text-sm tabular-nums text-slate-600">{formatarCnpjCpf(form.tomadorDocumento)}</span>
                </div>
                <CheckCircle className="h-5 w-5 text-emerald-600" />
              </div>
            ) : null}
            <div>
              <Label>E-mail</Label>
              <InputField
                value={form.tomadorEmail}
                onChange={(e) => setForm((f) => ({ ...f, tomadorEmail: e.target.value }))}
                placeholder="para enviar o DANFSe"
              />
            </div>
            <div>
              <Label>Telefone</Label>
              <InputField
                value={form.tomadorTelefone}
                onChange={(e) => setForm((f) => ({ ...f, tomadorTelefone: e.target.value }))}
              />
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-400">Serviço e valor</h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="md:col-span-2">
              <Label>Serviço tributário cadastrado</Label>
              <Select
                options={servicoOptions}
                placeholder="Selecione um serviço…"
                value={form.servicoTributId}
                onChange={(v) => void aplicarServico(v)}
              />
              {servicoOptions.length === 0 ? (
                <p className="mt-1 text-xs text-amber-600">
                  Cadastre serviços em{" "}
                  <Link href="/tributacao/nfse-servico" className="underline">
                    Tributação NFS-e
                  </Link>
                  .
                </p>
              ) : null}
            </div>
            <div>
              <Label>Item LC 116</Label>
              <InputField
                value={form.itemListaServico}
                onChange={(e) => setForm((f) => ({ ...f, itemListaServico: e.target.value }))}
              />
            </div>
            <div>
              <Label>Cód. trib. município</Label>
              <InputField
                value={form.codigoTributacaoMunicipio}
                onChange={(e) => setForm((f) => ({ ...f, codigoTributacaoMunicipio: e.target.value }))}
              />
            </div>
            <div className="md:col-span-2">
              <Label>Descrição do serviço</Label>
              <TextArea
                rows={3}
                value={form.descricaoServico}
                onChange={(v) => setForm((f) => ({ ...f, descricaoServico: v }))}
              />
            </div>
            <div className="md:col-span-2 rounded-2xl border border-emerald-200 bg-emerald-50/60 p-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div className="sm:col-span-2">
                  <Label>Valor dos serviços</Label>
                  <MaskedNumberInput
                    id="nfse-valor"
                    value={form.valorServicos}
                    onChange={(v) => setForm((f) => ({ ...f, valorServicos: v }))}
                    decimalPlaces={2}
                    prefix="R$"
                    placeholder="0,00"
                    className="[&_input]:h-14 [&_input]:text-2xl [&_input]:tracking-tight"
                  />
                </div>
                <div>
                  <Label>Alíquota ISS</Label>
                  <MaskedNumberInput
                    id="nfse-aliq"
                    value={form.aliquotaIss}
                    onChange={(v) => setForm((f) => ({ ...f, aliquotaIss: v }))}
                    decimalPlaces={2}
                    suffix="%"
                    placeholder="0,00"
                  />
                  <p className="mt-2 text-xs text-slate-500">
                    ISS estimado: <strong className="tabular-nums text-emerald-800">{moneyBr(valorIss)}</strong>
                  </p>
                </div>
              </div>
            </div>
            <div>
              <Label>Município IBGE</Label>
              <InputField
                value={form.municipioIbge}
                onChange={(e) => setForm((f) => ({ ...f, municipioIbge: e.target.value }))}
              />
            </div>
            <div>
              <Label>Observações</Label>
              <InputField
                value={form.observacoes}
                onChange={(e) => setForm((f) => ({ ...f, observacoes: e.target.value }))}
              />
            </div>
          </div>

          <button
            type="button"
            onClick={() => setAvancado((v) => !v)}
            className="mt-4 flex w-full items-center justify-between rounded-xl border border-slate-200 px-4 py-2.5 text-left text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            <span>Opções avançadas de tributação</span>
            <ChevronDown className={`h-4 w-4 transition ${avancado ? "rotate-180" : ""}`} />
          </button>

          {avancado ? (
            <div className="mt-3 grid grid-cols-1 gap-3 rounded-xl border border-dashed border-slate-200 p-4 md:grid-cols-3">
              <div>
                <Label>Tributação ISSQN</Label>
                <Select
                  options={TRIBUTACAO_ISSQN.map((o) => ({ value: o.value, label: `${o.value} — ${o.label}` }))}
                  value={form.tributacaoIssqn}
                  onChange={(v) => setForm((f) => ({ ...f, tributacaoIssqn: v }))}
                />
              </div>
              <div>
                <Label>ISS retido</Label>
                <Select
                  options={ISS_RETIDO.map((o) => ({ value: o.value, label: `${o.value} — ${o.label}` }))}
                  value={form.issRetido}
                  onChange={(v) => setForm((f) => ({ ...f, issRetido: v }))}
                />
              </div>
              <div>
                <Label>Simples Nacional</Label>
                <Select
                  options={SIMPLES_NACIONAL.map((o) => ({ value: o.value, label: `${o.value} — ${o.label}` }))}
                  value={form.simplesNacional}
                  onChange={(v) => setForm((f) => ({ ...f, simplesNacional: v }))}
                />
              </div>
            </div>
          ) : null}
        </section>

        <div className="sticky bottom-3 z-10 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white/95 px-4 py-3 shadow-lg backdrop-blur">
          <div className="text-sm text-slate-500">
            <span className="block text-[11px] uppercase tracking-wide">Total serviços</span>
            <span className="text-xl font-bold tabular-nums text-slate-900">{moneyBr(form.valorServicos)}</span>
            <span className="ml-2 text-xs text-slate-400">ISS ~ {formatBrNumber(valorIss, 2)}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="submit"
              disabled={salvando || !podeEmitir}
              className="inline-flex items-center gap-2 rounded-lg bg-[#16c15e] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[#13aa52] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {salvando ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" /> Emitindo…
                </>
              ) : (
                "Confirmar emissão"
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
