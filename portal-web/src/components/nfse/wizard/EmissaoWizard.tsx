"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type EmissaoContexto, type EmissaoSucesso } from "@/lib/api";
import {
  criarFormularioInicial,
  formParaPayload,
  recalcularValores,
} from "@/lib/form-defaults";
import {
  buscarTomadorPorDocumento,
  listarTomadores,
  salvarTomador,
  type TomadorSalvo,
} from "@/lib/tomador-storage";
import { notifyParent } from "@/lib/postMessage";
import type { EmissaoFormState } from "@/types/emissao-form";
import { CertificadoAmbienteBar } from "../CertificadoAmbienteBar";
import { ClassificacaoFiscalServico } from "../ClassificacaoFiscalServico";
import { TributacaoClassificacaoFiscal } from "./TributacaoClassificacaoFiscal";
import { StepIndicator } from "./StepIndicator";
import { Card, GhostButton, Input, Label, PrimaryButton, Textarea, Toggle } from "../ui";
import { Loader2, Mail, Printer, Sparkles } from "lucide-react";

function setPath<K extends keyof EmissaoFormState>(
  form: EmissaoFormState,
  section: K,
  field: keyof EmissaoFormState[K],
  value: string | boolean,
): EmissaoFormState {
  return { ...form, [section]: { ...form[section], [field]: value } };
}

export function EmissaoWizard({ token }: { token: string }) {
  const [ctx, setCtx] = useState<EmissaoContexto | null>(null);
  const [step, setStep] = useState(1);
  const [form, setForm] = useState<EmissaoFormState>(() => criarFormularioInicial(null));
  const [buscandoTomador, setBuscandoTomador] = useState(false);
  const [sugestoesTomador, setSugestoesTomador] = useState<TomadorSalvo[]>([]);
  const [simples, setSimples] = useState(false);
  const [issRetido, setIssRetido] = useState(false);
  const [erro, setErro] = useState("");
  const [emitindo, setEmitindo] = useState(false);
  const [sucesso, setSucesso] = useState<EmissaoSucesso | null>(null);
  const [emailDanfe, setEmailDanfe] = useState("");
  const [enviandoEmail, setEnviandoEmail] = useState(false);
  const [emailMsg, setEmailMsg] = useState("");
  const [emailErro, setEmailErro] = useState("");

  const carregarContexto = useCallback(() => {
    api.emissaoContexto(token).then((c) => {
      setCtx(c);
      setForm(criarFormularioInicial(c));
    });
  }, [token]);

  useEffect(() => {
    carregarContexto();
    setSugestoesTomador(listarTomadores());
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

  async function resolverTomador(doc?: string) {
    const documento = (doc ?? form.tomador.documento).replace(/\D/g, "");
    if (documento.length !== 11 && documento.length !== 14) return;
    setBuscandoTomador(true);
    const dados = await buscarTomadorPorDocumento(documento);
    setBuscandoTomador(false);
    if (!dados) return;
    setForm((f) => ({
      ...f,
      tomador: {
        ...f.tomador,
        documento,
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
  }

  function aplicarTributacao() {
    patch("regime", "simplesNacional", simples ? "3" : "1");
    patch("regime", "issRetido", issRetido ? "2" : "1");
  }

  useEffect(() => {
    aplicarTributacao();
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
      const msg = e instanceof Error ? e.message : "Erro na emissão";
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
          `XML enviado para ${emailDanfe.trim()}. O PDF sera reenviado automaticamente quando a SEFIN disponibilizar.`,
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

  if (sucesso) {
    return (
      <Card className="animate-in w-full text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--brand-soft)]">
          <Sparkles className="h-6 w-6 text-[var(--brand)]" />
        </div>
        <h2 className="text-xl font-semibold text-slate-900">NFS-e emitida</h2>
        <p className="mt-2 text-sm text-[var(--muted)]">Chave de acesso</p>
        <p className="mt-1 break-all font-mono text-xs text-slate-700">{sucesso.chaveAcesso}</p>
        <p className="mt-4 text-xs text-[var(--muted)]">
          O PDF é o DANFSe oficial (SEFIN). Use Imprimir para abrir na impressora ou Baixar para salvar o arquivo.
        </p>
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
      </Card>
    );
  }

  return (
    <div className="w-full animate-in">
      <CertificadoAmbienteBar ctx={ctx} />
      <StepIndicator current={step} />

      <Card className="w-full">
        {step === 1 && (
          <div className="space-y-5">
            <Label hint="Buscamos nome e endereço automaticamente">CPF ou CNPJ do cliente</Label>
            <div className="relative">
              <Input
                placeholder="00.000.000/0000-00"
                value={form.tomador.documento}
                onChange={(e) => patch("tomador", "documento", e.target.value)}
                onBlur={() => resolverTomador()}
              />
              {buscandoTomador && (
                <Loader2 className="absolute right-3 top-3.5 h-5 w-5 animate-spin text-[var(--muted)]" />
              )}
            </div>
            {sugestoesTomador.length > 0 && (
              <div className="space-y-1">
                <p className="text-xs font-medium text-[var(--muted)]">Recentes</p>
                {sugestoesTomador.slice(0, 4).map((t) => (
                  <button
                    key={t.documento}
                    type="button"
                    className="flex w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-slate-50"
                    onClick={() => {
                      patch("tomador", "documento", t.documento);
                      resolverTomador(t.documento);
                    }}
                  >
                    <span className="font-medium text-slate-800">{t.razaoSocial}</span>
                    <span className="ml-2 text-[var(--muted)]">{t.documento}</span>
                  </button>
                ))}
              </div>
            )}
            <div>
              <Label>Nome / razão social</Label>
              <Input
                value={form.tomador.razaoSocial}
                onChange={(e) => patch("tomador", "razaoSocial", e.target.value)}
              />
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-5">
            <Label>Qual serviço foi prestado?</Label>
            <ClassificacaoFiscalServico token={token} form={form} patch={patch} ctx={ctx} ativo={step === 2} />
            {form.servico.itemListaServico && (
              <div>
                <Label>Descrição na nota</Label>
                <Textarea
                  value={form.servico.descricaoServico}
                  onChange={(e) => patch("servico", "descricaoServico", e.target.value)}
                />
              </div>
            )}
          </div>
        )}

        {step === 3 && (
          <div className="space-y-5">
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
            {form.valores.aliquota && (
              <p className="rounded-lg bg-[var(--brand-soft)] px-4 py-2 text-sm text-slate-700">
                Alíquota estimada: <strong>{form.valores.aliquota}%</strong>
                {form.valores.valorIss && (
                  <> · ISS ~ R$ {form.valores.valorIss}</>
                )}
              </p>
            )}
          </div>
        )}

        {step === 4 && (
          <div className="space-y-4 text-sm">
            <h3 className="font-semibold text-slate-900">Revisão</h3>
            <dl className="space-y-3">
              <div className="flex justify-between border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)]">Cliente</dt>
                <dd className="font-medium text-right">{form.tomador.razaoSocial}</dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)] shrink-0">Serviço</dt>
                <dd className="min-w-0 text-right font-medium">{form.servico.descricaoServico}</dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)] shrink-0">LC 116</dt>
                <dd className="font-mono text-xs">{form.servico.itemListaServico}</dd>
              </div>
              <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)] shrink-0">NBS</dt>
                <dd className="min-w-0 text-right text-xs">
                  {form.servico.nbs ? (
                    <>
                      <span className="font-mono">{form.servico.nbs}</span>
                    </>
                  ) : (
                    "—"
                  )}
                </dd>
              </div>
              <div className="flex justify-between border-b border-slate-100 pb-2">
                <dt className="text-[var(--muted)]">Valor</dt>
                <dd className="text-lg font-semibold text-[var(--brand)]">
                  R$ {form.valores.valorServicos}
                </dd>
              </div>
            </dl>
          </div>
        )}

        {erro && (
          <div
            id="emissao-erro"
            role="alert"
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
          >
            <p className="font-medium">Não foi possível emitir a NFS-e</p>
            <p className="mt-1">{erro}</p>
          </div>
        )}

        <TributacaoClassificacaoFiscal
          token={token}
          form={form}
          patch={patch}
          ctx={ctx}
          onRecalc={() => setForm((f) => recalcularValores(f))}
        />

        <div className="mt-8 flex items-center justify-between gap-3 border-t border-[var(--border)] pt-6">
          <GhostButton
            disabled={step === 1}
            onClick={() => setStep((s) => Math.max(1, s - 1))}
          >
            Voltar
          </GhostButton>
          {step < 4 ? (
            <PrimaryButton
              disabled={!podeAvancar}
              onClick={() => {
                if (step === 3) {
                  if (ctx) {
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
              onClick={emitir}
            >
              {emitindo ? "Emitindo..." : "Emitir NFS-e"}
            </PrimaryButton>
          )}
        </div>
      </Card>
    </div>
  );
}
