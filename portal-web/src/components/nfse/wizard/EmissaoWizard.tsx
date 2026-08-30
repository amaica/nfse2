"use client";

import { useCallback, useEffect, useState } from "react";
import type { AutoCompleteCompleteEvent } from "primereact/autocomplete";
import { ChevronDown, Loader2, Mail, Printer, Sparkles } from "lucide-react";
import { api, formatarCnpjCpf, type EmissaoContexto, type EmissaoSucesso } from "@/lib/api";
import {
  criarFormularioInicial,
  formParaPayload,
  recalcularValores,
} from "@/lib/form-defaults";
import { PORTAL_EMPRESA_EVENT } from "@/lib/portal-empresa";
import {
  buscarTomadorPorDocumento,
  listarTomadores,
  salvarTomador,
  type TomadorSalvo,
} from "@/lib/tomador-storage";
import { fiscalApi } from "@/lib/fiscal-api";
import { notifyParent } from "@/lib/postMessage";
import { mapEmissaoError } from "@/lib/assinatura";
import type { EmissaoFormState } from "@/types/emissao-form";
import { CertificadoAmbienteBar } from "../CertificadoAmbienteBar";
import { ClassificacaoFiscalServico } from "../ClassificacaoFiscalServico";
import { TributacaoClassificacaoFiscal } from "./TributacaoClassificacaoFiscal";
import { StepIndicator } from "./StepIndicator";
import { Card, GhostButton, Input, Label, PrimaryButton, Textarea, Toggle } from "../ui";
import { AutoCompleteField, type AcOption } from "@/components/ui/AutoCompleteField";
import {
  CST_PIS_COFINS,
  ISS_RETIDO,
  REGIME_ESPECIAL,
  SIMPLES_NACIONAL,
  TRIBUTACAO_ISSQN,
} from "@/lib/nfse-servico-opcoes";

function setPath<K extends keyof EmissaoFormState>(
  form: EmissaoFormState,
  section: K,
  field: keyof EmissaoFormState[K],
  value: string | boolean,
): EmissaoFormState {
  return { ...form, [section]: { ...form[section], [field]: value } };
}

function asOption(value: AcOption | string | null): AcOption | null {
  if (!value) return null;
  if (typeof value === "string") return { label: value, value };
  return value;
}

function opcoesCatalogo(
  lista: Array<{ value: string; label: string }>,
  query: string,
): AcOption[] {
  const q = query.trim().toLowerCase();
  return lista
    .filter((o) => !q || o.label.toLowerCase().includes(q) || o.value.includes(q))
    .map((o) => ({ label: `${o.value} — ${o.label}`, value: o.value, meta: o.label }));
}

export function EmissaoWizard({ token }: { token: string }) {
  const [ctx, setCtx] = useState<EmissaoContexto | null>(null);
  const [step, setStep] = useState(1);
  const [form, setForm] = useState<EmissaoFormState>(() => criarFormularioInicial(null));
  const [buscandoTomador, setBuscandoTomador] = useState(false);
  const [cliente, setCliente] = useState<AcOption | null>(null);
  const [clienteSug, setClienteSug] = useState<AcOption[]>([]);
  const [simples, setSimples] = useState(false);
  const [issRetido, setIssRetido] = useState(false);
  const [avancado, setAvancado] = useState(false);
  const [erro, setErro] = useState("");
  const [emitindo, setEmitindo] = useState(false);
  const [sucesso, setSucesso] = useState<EmissaoSucesso | null>(null);
  const [emailDanfe, setEmailDanfe] = useState("");
  const [enviandoEmail, setEnviandoEmail] = useState(false);
  const [emailMsg, setEmailMsg] = useState("");
  const [emailErro, setEmailErro] = useState("");

  const [issqnSug, setIssqnSug] = useState<AcOption[]>([]);
  const [simplesSug, setSimplesSug] = useState<AcOption[]>([]);
  const [issRetSug, setIssRetSug] = useState<AcOption[]>([]);
  const [regimeSug, setRegimeSug] = useState<AcOption[]>([]);
  const [cstSug, setCstSug] = useState<AcOption[]>([]);

  const carregarContexto = useCallback(() => {
    api.emissaoContexto(token).then((c) => {
      setCtx(c);
      setForm(criarFormularioInicial(c));
      setCliente(null);
      setStep(1);
    });
  }, [token]);

  useEffect(() => {
    carregarContexto();
  }, [carregarContexto]);

  useEffect(() => {
    const onTroca = () => {
      setSucesso(null);
      setErro("");
      carregarContexto();
    };
    window.addEventListener(PORTAL_EMPRESA_EVENT, onTroca);
    return () => window.removeEventListener(PORTAL_EMPRESA_EVENT, onTroca);
  }, [carregarContexto]);

  const patch = useCallback(
    <K extends keyof EmissaoFormState>(
      section: K,
      field: keyof EmissaoFormState[K],
      value: string | boolean,
    ) => {
      setForm((f) => {
        const next = setPath(f, section, field, value);
        const recalcSections: (keyof EmissaoFormState)[] = [
          "valores",
          "retencoesFederais",
          "tributacaoFederal",
          "ibsCbs",
          "regime",
        ];
        return recalcSections.includes(section) ? recalcularValores(next) : next;
      });
    },
    [],
  );

  async function aplicarTomador(documento: string, dadosExtras?: Partial<TomadorSalvo>) {
    const doc = documento.replace(/\D/g, "");
    if (doc.length !== 11 && doc.length !== 14) return;
    setBuscandoTomador(true);
    try {
      let dados: Partial<TomadorSalvo> | null = dadosExtras ?? null;
      if (!dados?.razaoSocial) {
        dados = await buscarTomadorPorDocumento(doc);
      }
      if (!dados) return;
      setForm((f) => ({
        ...f,
        tomador: {
          ...f.tomador,
          documento: doc,
          razaoSocial: dados.razaoSocial ?? f.tomador.razaoSocial,
          email: dados.email ?? f.tomador.email,
          telefone: dados.telefone ?? f.tomador.telefone,
        },
        enderecoTomador: {
          ...f.enderecoTomador,
          cep: dados.cep ?? f.enderecoTomador.cep,
          logradouro: dados.logradouro ?? f.enderecoTomador.logradouro,
          numero: dados.numero ?? f.enderecoTomador.numero,
          bairro: dados.bairro ?? f.enderecoTomador.bairro,
          cidade: dados.cidade ?? f.enderecoTomador.cidade,
          uf: dados.uf ?? f.enderecoTomador.uf,
          codigoMunicipioIbge: dados.codigoMunicipioIbge ?? f.enderecoTomador.codigoMunicipioIbge,
        },
      }));
      setCliente({
        label: dados.razaoSocial || formatarCnpjCpf(doc),
        value: doc,
        meta: formatarCnpjCpf(doc),
      });
    } finally {
      setBuscandoTomador(false);
    }
  }

  const buscarCliente = useCallback(async (event: AutoCompleteCompleteEvent) => {
    const q = event.query?.trim() ?? "";
    const recentes = listarTomadores()
      .filter(
        (t) =>
          !q ||
          t.razaoSocial.toLowerCase().includes(q.toLowerCase()) ||
          t.documento.includes(q.replace(/\D/g, "")),
      )
      .slice(0, 8)
      .map((t) => ({
        label: t.razaoSocial,
        value: t.documento,
        meta: formatarCnpjCpf(t.documento),
        raw: t,
      }));

    if (q.length < 2) {
      setClienteSug(recentes);
      return;
    }

    try {
      const pessoas = await fiscalApi.buscaPessoas(q);
      const mapa = new Map<string, AcOption>();
      for (const r of recentes) mapa.set(r.value.replace(/\D/g, ""), r);
      for (const p of pessoas) {
        const doc = (p.cpfCnpj || "").replace(/\D/g, "");
        if (!doc) continue;
        mapa.set(doc, {
          label: p.nome,
          value: doc,
          meta: formatarCnpjCpf(doc),
          raw: { id: p.id, documento: doc, razaoSocial: p.nome },
        });
      }
      setClienteSug([...mapa.values()].slice(0, 20));
    } catch {
      setClienteSug(recentes);
    }
  }, []);

  async function selecionarCliente(opt: AcOption) {
    setCliente(opt);
    const doc = opt.value.replace(/\D/g, "");
    patch("tomador", "documento", doc);
    patch("tomador", "razaoSocial", opt.label);

    const raw = opt.raw as { id?: number; razaoSocial?: string; documento?: string } | TomadorSalvo | undefined;
    if (raw && "id" in raw && raw.id) {
      try {
        const full = await fiscalApi.get<{
          nome?: string;
          email?: string;
          telefone?: string;
          cep?: string;
          logradouro?: string;
          numero?: string;
          bairro?: string;
          municipio?: string;
          uf?: string;
          codigoMunicipioIbge?: string;
        }>("/api/pessoas", raw.id);
        await aplicarTomador(doc, {
          documento: doc,
          razaoSocial: full.nome || opt.label,
          email: full.email,
          telefone: full.telefone,
          cep: full.cep,
          logradouro: full.logradouro,
          numero: full.numero,
          bairro: full.bairro,
          cidade: full.municipio,
          uf: full.uf,
          codigoMunicipioIbge: full.codigoMunicipioIbge,
        });
        return;
      } catch {
        /* cai no fluxo padrão */
      }
    }
    await aplicarTomador(doc, raw as Partial<TomadorSalvo>);
  }

  useEffect(() => {
    patch("regime", "simplesNacional", simples ? "3" : "1");
    patch("regime", "issRetido", issRetido ? "2" : "1");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [simples, issRetido]);

  useEffect(() => {
    if (erro) {
      document.getElementById("emissao-erro")?.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }, [erro]);

  async function emitir() {
    setEmitindo(true);
    setErro("");
    try {
      salvarTomador({
        documento: form.tomador.documento,
        razaoSocial: form.tomador.razaoSocial,
        email: form.tomador.email,
        telefone: form.tomador.telefone,
        ...form.enderecoTomador,
      });
      const payload = formParaPayload(recalcularValores(form));
      const res = await api.emitir(token, payload);
      setSucesso(res);
      setEmailDanfe(form.tomador.email?.trim() ?? "");
      setEmailMsg("");
      setEmailErro("");
      setStep(4);
      notifyParent({ type: "NFSE_EMITIDA", chave: res.chaveAcesso });
    } catch (e) {
      const msg = mapEmissaoError(e instanceof Error ? e.message : "Erro na emissão");
      setErro(msg);
      notifyParent({ type: "ERRO_EMISSAO", mensagem: msg });
    } finally {
      setEmitindo(false);
    }
  }

  async function enviarDanfeEmail() {
    if (!sucesso?.chaveAcesso || !emailDanfe.trim()) return;
    setEnviandoEmail(true);
    setEmailErro("");
    setEmailMsg("");
    try {
      const res = await api.enviarDanfeEmail(token, sucesso.chaveAcesso, emailDanfe.trim());
      if (res.pdfRetryAgendado) {
        setEmailMsg(
          `XML enviado para ${emailDanfe.trim()}. O PDF será reenviado automaticamente quando a SEFIN disponibilizar.`,
        );
      } else if (res.anexoXml) {
        setEmailMsg(`Documento enviado para ${emailDanfe.trim()} (XML em anexo).`);
      } else {
        setEmailMsg(`DANFSe enviado para ${emailDanfe.trim()}`);
      }
    } catch (e) {
      setEmailErro(e instanceof Error ? e.message : "Falha ao enviar e-mail");
    } finally {
      setEnviandoEmail(false);
    }
  }

  const podeAvancar =
    (step === 1 && form.tomador.documento && form.tomador.razaoSocial) ||
    (step === 2 &&
      form.servico.itemListaServico &&
      form.servico.descricaoServico &&
      form.servico.nbs?.replace(/\D/g, "").length === 9) ||
    (step === 3 && form.valores.valorServicos) ||
    step === 4;

  const issqnAtual =
    TRIBUTACAO_ISSQN.find((o) => o.value === form.regime.tributacaoIssqn) ?? null;
  const simplesAtual =
    SIMPLES_NACIONAL.find((o) => o.value === form.regime.simplesNacional) ?? null;
  const issRetAtual = ISS_RETIDO.find((o) => o.value === form.regime.issRetido) ?? null;
  const regimeAtual =
    REGIME_ESPECIAL.find((o) => o.value === form.regime.regimeEspecialTributacao) ?? null;
  const cstAtual =
    CST_PIS_COFINS.find((o) => o.value === form.tributacaoFederal.cstPisCofins) ?? null;

  if (sucesso) {
    return (
      <div className="nfse-emissao-page">
        <Card className="animate-in w-full text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--brand-soft)]">
            <Sparkles className="h-6 w-6 text-[var(--brand)]" />
          </div>
          <h2 className="text-xl font-semibold text-slate-900">NFS-e emitida</h2>
          <p className="mt-2 text-sm text-[var(--muted)]">Chave de acesso</p>
          <p className="mt-1 break-all font-mono text-xs text-slate-700">{sucesso.chaveAcesso}</p>
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            <PrimaryButton onClick={() => api.imprimirPdf(token, sucesso.chaveAcesso)}>
              <Printer className="mr-1.5 h-4 w-4" /> Imprimir
            </PrimaryButton>
            <GhostButton onClick={() => api.downloadPdf(token, sucesso.chaveAcesso)}>Baixar PDF</GhostButton>
            <GhostButton onClick={() => api.downloadXml(token, sucesso.chaveAcesso)}>XML</GhostButton>
          </div>
          <div className="mx-auto mt-6 max-w-md rounded-xl border border-[var(--border)] bg-slate-50 p-4 text-left">
            <p className="mb-2 text-sm font-medium text-slate-800">Enviar DANFSe por e-mail</p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Input
                type="email"
                placeholder="destinatario@empresa.com.br"
                value={emailDanfe}
                onChange={(e) => setEmailDanfe(e.target.value)}
                className="flex-1"
              />
              <PrimaryButton
                type="button"
                disabled={enviandoEmail || !emailDanfe.trim()}
                onClick={() => void enviarDanfeEmail()}
              >
                {enviandoEmail ? (
                  <Loader2 className="mr-1.5 h-4 w-4 animate-spin" />
                ) : (
                  <Mail className="mr-1.5 h-4 w-4" />
                )}
                Enviar
              </PrimaryButton>
            </div>
            {emailMsg && <p className="mt-2 text-sm text-green-700">{emailMsg}</p>}
            {emailErro && <p className="mt-2 text-sm text-red-600">{emailErro}</p>}
          </div>
          <div className="mt-6">
            <GhostButton
              onClick={() => {
                setSucesso(null);
                carregarContexto();
              }}
            >
              Emitir outra
            </GhostButton>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="nfse-emissao-page animate-in">
      <header className="nfse-emissao-hero">
        <h1>Emitir NFS-e</h1>
        <p>
          {ctx?.prestadorNome ?? ctx?.empresaNome ?? "—"}
          {ctx?.prefeitura ? ` · ${ctx.prefeitura}` : ""}
        </p>
      </header>

      <CertificadoAmbienteBar ctx={ctx} />
      <StepIndicator current={step} />

      <section className="nfse-emissao-card">
        {step === 1 && (
          <div className="space-y-5">
            <div>
              <h2>Quem é o cliente?</h2>
              <p className="sub">Digite o nome ou CPF/CNPJ — o sistema completa o cadastro.</p>
            </div>
            <AutoCompleteField
              id="nfse-cliente"
              label="Cliente / tomador"
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
                  patch("tomador", "documento", v);
                  const digits = v.replace(/\D/g, "");
                  if (digits.length === 11 || digits.length === 14) {
                    void aplicarTomador(digits);
                  }
                }
              }}
            />
            {buscandoTomador && (
              <p className="flex items-center gap-2 text-sm text-slate-500">
                <Loader2 className="h-4 w-4 animate-spin" /> Buscando dados do cliente…
              </p>
            )}
            {form.tomador.razaoSocial ? (
              <div className="rounded-xl border border-[var(--primary-100)] bg-[var(--primary-50)] px-3 py-2.5 text-sm">
                <strong className="block text-[var(--primary-800)]">{form.tomador.razaoSocial}</strong>
                <span className="text-slate-600">
                  {formatarCnpjCpf(form.tomador.documento)}
                  {form.enderecoTomador.cidade
                    ? ` · ${form.enderecoTomador.cidade}/${form.enderecoTomador.uf}`
                    : ""}
                </span>
              </div>
            ) : null}
            <div>
              <Label>E-mail (opcional)</Label>
              <Input
                value={form.tomador.email}
                onChange={(e) => patch("tomador", "email", e.target.value)}
                placeholder="para enviar o DANFSe"
              />
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-5">
            <div>
              <h2>Qual serviço?</h2>
              <p className="sub">Escolha o tributo cadastrado ou busque o serviço LC 116.</p>
            </div>
            <ClassificacaoFiscalServico token={token} form={form} patch={patch} ctx={ctx} ativo={step === 2} />
            {form.servico.itemListaServico ? (
              <div>
                <Label>Descrição na nota</Label>
                <Textarea
                  value={form.servico.descricaoServico}
                  onChange={(e) => patch("servico", "descricaoServico", e.target.value)}
                />
              </div>
            ) : null}
          </div>
        )}

        {step === 3 && (
          <div className="space-y-5">
            <div>
              <h2>Valor e regime</h2>
              <p className="sub">Só o essencial. Detalhes fiscais ficam em opções avançadas.</p>
            </div>
            <div>
              <Label>Valor do serviço (R$)</Label>
              <Input
                inputMode="decimal"
                placeholder="0,00"
                className="text-2xl font-semibold tracking-tight"
                value={form.valores.valorServicos}
                onChange={(e) => patch("valores", "valorServicos", e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Toggle label="Optante Simples Nacional?" checked={simples} onChange={setSimples} />
              <Toggle label="ISS retido pelo cliente?" checked={issRetido} onChange={setIssRetido} />
            </div>
            {form.valores.aliquota ? (
              <p className="rounded-lg bg-[var(--brand-soft)] px-4 py-2 text-sm text-slate-700">
                Alíquota estimada: <strong>{form.valores.aliquota}%</strong>
                {form.valores.valorIss ? <> · ISS ~ R$ {form.valores.valorIss}</> : null}
              </p>
            ) : null}

            <button type="button" className="nfse-adv-toggle" onClick={() => setAvancado((v) => !v)}>
              <span>Opções avançadas de tributação</span>
              <ChevronDown className={`h-4 w-4 transition ${avancado ? "rotate-180" : ""}`} />
            </button>

            {avancado ? (
              <div className="nfse-adv-body">
                <AutoCompleteField
                  id="nfse-issqn"
                  label="Tributação ISSQN"
                  value={
                    issqnAtual
                      ? { label: `${issqnAtual.value} — ${issqnAtual.label}`, value: issqnAtual.value }
                      : null
                  }
                  suggestions={issqnSug}
                  completeMethod={(e) => setIssqnSug(opcoesCatalogo(TRIBUTACAO_ISSQN, e.query))}
                  forceSelection
                  onChange={(v) => {
                    const opt = asOption(v);
                    if (opt) patch("regime", "tributacaoIssqn", opt.value);
                  }}
                />
                <AutoCompleteField
                  id="nfse-sn"
                  label="Simples Nacional"
                  value={
                    simplesAtual
                      ? { label: `${simplesAtual.value} — ${simplesAtual.label}`, value: simplesAtual.value }
                      : null
                  }
                  suggestions={simplesSug}
                  completeMethod={(e) => setSimplesSug(opcoesCatalogo(SIMPLES_NACIONAL, e.query))}
                  forceSelection
                  onChange={(v) => {
                    const opt = asOption(v);
                    if (opt) {
                      patch("regime", "simplesNacional", opt.value);
                      setSimples(opt.value !== "1");
                    }
                  }}
                />
                <AutoCompleteField
                  id="nfse-iss-ret"
                  label="ISS retido"
                  value={
                    issRetAtual
                      ? { label: `${issRetAtual.value} — ${issRetAtual.label}`, value: issRetAtual.value }
                      : null
                  }
                  suggestions={issRetSug}
                  completeMethod={(e) => setIssRetSug(opcoesCatalogo(ISS_RETIDO, e.query))}
                  forceSelection
                  onChange={(v) => {
                    const opt = asOption(v);
                    if (opt) {
                      patch("regime", "issRetido", opt.value);
                      setIssRetido(opt.value === "2");
                    }
                  }}
                />
                <AutoCompleteField
                  id="nfse-regime"
                  label="Regime especial"
                  value={
                    regimeAtual
                      ? { label: `${regimeAtual.value} — ${regimeAtual.label}`, value: regimeAtual.value }
                      : null
                  }
                  suggestions={regimeSug}
                  completeMethod={(e) => setRegimeSug(opcoesCatalogo(REGIME_ESPECIAL, e.query))}
                  forceSelection
                  onChange={(v) => {
                    const opt = asOption(v);
                    if (opt) patch("regime", "regimeEspecialTributacao", opt.value);
                  }}
                />
                <AutoCompleteField
                  id="nfse-cst"
                  label="CST PIS/COFINS"
                  value={
                    cstAtual
                      ? { label: `${cstAtual.value} — ${cstAtual.label}`, value: cstAtual.value }
                      : null
                  }
                  suggestions={cstSug}
                  completeMethod={(e) => setCstSug(opcoesCatalogo(CST_PIS_COFINS, e.query))}
                  forceSelection
                  onChange={(v) => {
                    const opt = asOption(v);
                    if (opt) patch("tributacaoFederal", "cstPisCofins", opt.value);
                  }}
                />
                <div>
                  <Label>Alíquota ISS (%)</Label>
                  <Input
                    value={form.valores.aliquota}
                    onChange={(e) => patch("valores", "aliquota", e.target.value)}
                  />
                </div>
                <div>
                  <Label>Observações (infCpl)</Label>
                  <Textarea
                    value={form.informacoesAdicionais.observacoes}
                    onChange={(e) => patch("informacoesAdicionais", "observacoes", e.target.value)}
                  />
                </div>
                <TributacaoClassificacaoFiscal
                  token={token}
                  form={form}
                  patch={patch}
                  ctx={ctx}
                  onRecalc={() => setForm((f) => recalcularValores(f))}
                />
              </div>
            ) : null}
          </div>
        )}

        {step === 4 && (
          <div className="space-y-4 text-sm">
            <div>
              <h2>Revisão</h2>
              <p className="sub">Confira e emita.</p>
            </div>
            <dl className="space-y-3">
              <div className="flex justify-between border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)]">Cliente</dt>
                <dd className="text-right font-medium">{form.tomador.razaoSocial}</dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
                <dt className="shrink-0 text-[var(--muted)]">Serviço</dt>
                <dd className="min-w-0 text-right font-medium">{form.servico.descricaoServico}</dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
                <dt className="shrink-0 text-[var(--muted)]">LC 116</dt>
                <dd className="font-mono text-xs">{form.servico.itemListaServico}</dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
                <dt className="shrink-0 text-[var(--muted)]">NBS</dt>
                <dd className="font-mono text-xs">{form.servico.nbs || "—"}</dd>
              </div>
              <div className="flex justify-between border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)]">Valor</dt>
                <dd className="text-lg font-semibold text-[var(--brand)]">R$ {form.valores.valorServicos}</dd>
              </div>
            </dl>
          </div>
        )}

        {erro ? (
          <div
            id="emissao-erro"
            role="alert"
            className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
          >
            <p className="font-medium">Não foi possível emitir a NFS-e</p>
            <p className="mt-1">{erro}</p>
          </div>
        ) : null}

        <div className="mt-8 flex items-center justify-between gap-3 border-t border-[var(--border)] pt-6">
          <GhostButton disabled={step === 1} onClick={() => setStep((s) => Math.max(1, s - 1))}>
            Voltar
          </GhostButton>
          {step < 4 ? (
            <PrimaryButton
              disabled={!podeAvancar}
              onClick={() => {
                if (step === 3 && ctx) {
                  api
                    .aliquota(token, ctx.codigoMunicipioIbge, form.servico.itemListaServico)
                    .then((r) =>
                      setForm((f) =>
                        recalcularValores({
                          ...f,
                          valores: { ...f.valores, aliquota: String(r.aliquota) },
                        }),
                      ),
                    )
                    .catch(() => {});
                }
                setStep((s) => Math.min(4, s + 1));
              }}
            >
              Continuar
            </PrimaryButton>
          ) : (
            <PrimaryButton
              className="min-w-[10rem]"
              disabled={emitindo || !ctx?.podeEmitir}
              onClick={() => void emitir()}
            >
              {emitindo ? "Emitindo..." : "Emitir NFS-e"}
            </PrimaryButton>
          )}
        </div>
      </section>
    </div>
  );
}
