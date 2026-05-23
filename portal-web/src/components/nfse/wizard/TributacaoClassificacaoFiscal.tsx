"use client";

import { ChevronDown } from "lucide-react";
import { useState } from "react";
import type { EmissaoFormState } from "@/types/emissao-form";
import { Accordion, Field, FormGrid, inputCls } from "@/components/nfse/Accordion";
import { ClassificacaoFiscalServico } from "@/components/nfse/ClassificacaoFiscalServico";
import { CST_PIS_COFINS, RESPONSAVEL_RETENCAO_ISS } from "@/lib/catalogos-fiscais";
import { cn } from "@/lib/utils";
import { fieldClass } from "../ui";

type Patch = <K extends keyof EmissaoFormState>(
  section: K,
  field: keyof EmissaoFormState[K],
  value: string | boolean,
) => void;

import type { InputHTMLAttributes, SelectHTMLAttributes } from "react";

function CompactInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn(fieldClass, "py-2 text-sm", props.className)} />;
}

function CompactSelect(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn(fieldClass, "py-2 text-sm", props.className)} />;
}

export function TributacaoClassificacaoFiscal({
  token,
  form,
  patch,
  onRecalc,
}: {
  token: string;
  form: EmissaoFormState;
  patch: Patch;
  onRecalc: () => void;
}) {
  const [open, setOpen] = useState(false);

  const patchR = <K extends keyof EmissaoFormState>(
    section: K,
    field: keyof EmissaoFormState[K],
    value: string | boolean,
  ) => {
    patch(section, field, value);
    onRecalc();
  };

  return (
    <div className="mt-6 border-t border-[var(--border)] pt-5">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between rounded-lg px-1 py-2 text-left text-sm font-medium text-slate-700 transition hover:bg-slate-50"
      >
        Tributação e Classificação Fiscal
        <ChevronDown className={cn("h-4 w-4 text-slate-400 transition", open && "rotate-180")} />
      </button>

      {open && (
        <div className="mt-3 space-y-2 animate-in">
          <Accordion title="Classificação fiscal do serviço" defaultOpen>
            <ClassificacaoFiscalServico token={token} form={form} patch={(s, f, v) => patchR(s, f, v)} />
          </Accordion>

          <Accordion title="Valores NFSe" defaultOpen={false}>
            <FormGrid cols={4}>
              <Field label="Valor serviços (R$)">
                <CompactInput value={form.valores.valorServicos} readOnly className="bg-slate-50" />
              </Field>
              <Field label="Dedução base cálculo">
                <CompactInput
                  value={form.valores.deducoes}
                  onChange={(e) => patchR("valores", "deducoes", e.target.value)}
                />
              </Field>
              <Field label="Desc. incondicionado">
                <CompactInput
                  value={form.valores.descontoIncondicionado}
                  onChange={(e) => patchR("valores", "descontoIncondicionado", e.target.value)}
                />
              </Field>
              <Field label="Desc. condicionado">
                <CompactInput
                  value={form.valores.descontoCondicionado}
                  onChange={(e) => patchR("valores", "descontoCondicionado", e.target.value)}
                />
              </Field>
              <Field label="Base cálculo">
                <CompactInput value={form.valores.baseCalculo} readOnly className="bg-slate-50" />
              </Field>
              <Field label="Alíquota ISS (%)">
                <CompactInput
                  value={form.valores.aliquota}
                  onChange={(e) => patchR("valores", "aliquota", e.target.value)}
                />
              </Field>
              <Field label="Valor ISS">
                <CompactInput value={form.valores.valorIss} readOnly className="bg-slate-50" />
              </Field>
              <Field label="ISS retido">
                <CompactSelect
                  value={form.regime.issRetido}
                  onChange={(e) => {
                    patchR("regime", "issRetido", e.target.value);
                    patchR("valores", "responsavelRetencaoIss", e.target.value === "2" ? "2" : "1");
                  }}
                >
                  <option value="1">Não retido</option>
                  <option value="2">Retido</option>
                </CompactSelect>
              </Field>
              <Field label="Responsável retenção">
                <CompactSelect
                  value={form.valores.responsavelRetencaoIss}
                  onChange={(e) => patchR("valores", "responsavelRetencaoIss", e.target.value)}
                >
                  {RESPONSAVEL_RETENCAO_ISS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </CompactSelect>
              </Field>
            </FormGrid>
          </Accordion>

          <Accordion title="Tributação federal" defaultOpen={false}>
            <div className="mb-3">
              <label className="flex items-center gap-2 text-xs text-slate-600">
                <input
                  type="checkbox"
                  checked={form.tributacaoFederal.habilitarRetencoes}
                  onChange={(e) => patchR("tributacaoFederal", "habilitarRetencoes", e.target.checked)}
                />
                Informar retenções federais detalhadas
              </label>
            </div>
            <FormGrid cols={4}>
              <Field label="CST PIS/COFINS">
                <CompactSelect
                  value={form.tributacaoFederal.cstPisCofins}
                  onChange={(e) => patchR("tributacaoFederal", "cstPisCofins", e.target.value)}
                >
                  {CST_PIS_COFINS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </CompactSelect>
              </Field>
              <Field label="Tipo retenção PIS/COFINS/CSLL">
                <CompactInput
                  value={form.tributacaoFederal.tipoRetencaoPisCofins}
                  onChange={(e) => patchR("tributacaoFederal", "tipoRetencaoPisCofins", e.target.value)}
                />
              </Field>
              <Field label="Base PIS/COFINS">
                <CompactInput
                  value={form.tributacaoFederal.baseCalculoPisCofins}
                  onChange={(e) => patchR("tributacaoFederal", "baseCalculoPisCofins", e.target.value)}
                />
              </Field>
              <Field label="Alíq. PIS (%)">
                <CompactInput
                  value={form.tributacaoFederal.aliquotaPis}
                  onChange={(e) => patchR("tributacaoFederal", "aliquotaPis", e.target.value)}
                />
              </Field>
              <Field label="Alíq. COFINS (%)">
                <CompactInput
                  value={form.tributacaoFederal.aliquotaCofins}
                  onChange={(e) => patchR("tributacaoFederal", "aliquotaCofins", e.target.value)}
                />
              </Field>
              <Field label="Valor PIS">
                <CompactInput value={form.tributacaoFederal.valorPis} readOnly className="bg-slate-50" />
              </Field>
              <Field label="Valor COFINS">
                <CompactInput value={form.tributacaoFederal.valorCofins} readOnly className="bg-slate-50" />
              </Field>
            </FormGrid>
            {form.tributacaoFederal.habilitarRetencoes && (
              <FormGrid cols={4}>
                <Field label="Retenção IRRF">
                  <CompactInput
                    value={form.tributacaoFederal.retencaoIrrf}
                    onChange={(e) => patchR("tributacaoFederal", "retencaoIrrf", e.target.value)}
                  />
                </Field>
                <Field label="Retenção CSLL">
                  <CompactInput
                    value={form.tributacaoFederal.retencaoCsll}
                    onChange={(e) => patchR("tributacaoFederal", "retencaoCsll", e.target.value)}
                  />
                </Field>
                <Field label="Retenção ISS">
                  <CompactInput
                    value={form.tributacaoFederal.retencaoIss}
                    onChange={(e) => patchR("tributacaoFederal", "retencaoIss", e.target.value)}
                  />
                </Field>
                <Field label="Retenção INSS">
                  <CompactInput
                    value={form.tributacaoFederal.retencaoInss}
                    onChange={(e) => patchR("tributacaoFederal", "retencaoInss", e.target.value)}
                  />
                </Field>
              </FormGrid>
            )}
          </Accordion>

          <Accordion title="IBS / CBS (reforma tributária)" defaultOpen={false}>
            <div className="mb-3">
              <label className="flex items-center gap-2 text-xs text-slate-600">
                <input
                  type="checkbox"
                  checked={form.ibsCbs.habilitar}
                  onChange={(e) => patchR("ibsCbs", "habilitar", e.target.checked)}
                />
                Preencher grupo IBS/CBS nesta NFS-e
              </label>
            </div>
            {form.ibsCbs.habilitar && (
              <FormGrid cols={4}>
                <Field label="CST IBS/CBS">
                  <CompactInput value={form.ibsCbs.cst} onChange={(e) => patchR("ibsCbs", "cst", e.target.value)} />
                </Field>
                <Field label="Class. tributária">
                  <CompactInput
                    value={form.ibsCbs.classificacaoTributaria}
                    onChange={(e) => patchR("ibsCbs", "classificacaoTributaria", e.target.value)}
                  />
                </Field>
                <Field label="Class. operação">
                  <CompactInput
                    value={form.ibsCbs.classificacaoOperacao}
                    onChange={(e) => patchR("ibsCbs", "classificacaoOperacao", e.target.value)}
                  />
                </Field>
                <Field label="Base cálculo">
                  <CompactInput
                    value={form.ibsCbs.baseCalculo}
                    onChange={(e) => patchR("ibsCbs", "baseCalculo", e.target.value)}
                  />
                </Field>
                <Field label="Alíq. IBS (%)">
                  <CompactInput
                    value={form.ibsCbs.aliquotaIbs}
                    onChange={(e) => patchR("ibsCbs", "aliquotaIbs", e.target.value)}
                  />
                </Field>
                <Field label="Alíq. CBS (%)">
                  <CompactInput
                    value={form.ibsCbs.aliquotaCbs}
                    onChange={(e) => patchR("ibsCbs", "aliquotaCbs", e.target.value)}
                  />
                </Field>
                <Field label="Redução IBS (%)">
                  <CompactInput
                    value={form.ibsCbs.reducaoIbs}
                    onChange={(e) => patchR("ibsCbs", "reducaoIbs", e.target.value)}
                  />
                </Field>
                <Field label="Redução CBS (%)">
                  <CompactInput
                    value={form.ibsCbs.reducaoCbs}
                    onChange={(e) => patchR("ibsCbs", "reducaoCbs", e.target.value)}
                  />
                </Field>
                <Field label="Valor IBS">
                  <CompactInput value={form.ibsCbs.valorIbs} readOnly className="bg-slate-50" />
                </Field>
                <Field label="Valor CBS">
                  <CompactInput value={form.ibsCbs.valorCbs} readOnly className="bg-slate-50" />
                </Field>
                <Field label="Total IBS/CBS">
                  <CompactInput value={form.ibsCbs.valorTotal} readOnly className="bg-slate-50" />
                </Field>
              </FormGrid>
            )}
          </Accordion>

          <Accordion title="Identificação RPS (automático)" defaultOpen={false}>
            <FormGrid cols={3}>
              <Field label="Número RPS (auto se vazio)">
                <input className={inputCls} value={form.identificacao.numeroRps} onChange={(e) => patch("identificacao", "numeroRps", e.target.value)} />
              </Field>
              <Field label="Série">
                <input className={inputCls} value={form.identificacao.serieRps} onChange={(e) => patch("identificacao", "serieRps", e.target.value)} />
              </Field>
              <Field label="Competência">
                <input type="date" className={inputCls} value={form.identificacao.competencia} onChange={(e) => patch("identificacao", "competencia", e.target.value)} />
              </Field>
            </FormGrid>
          </Accordion>
        </div>
      )}
    </div>
  );
}
