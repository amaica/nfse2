"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { AutoCompleteCompleteEvent } from "primereact/autocomplete";
import type { EmissaoContexto } from "@/lib/api";
import { api } from "@/lib/api";
import type { EmissaoFormState } from "@/types/emissao-form";
import type { ServicoLc116 } from "@/lib/servicos-lc116";
import { AutoCompleteField, type AcOption } from "@/components/ui/AutoCompleteField";

type Patch = <K extends keyof EmissaoFormState>(
  section: K,
  field: keyof EmissaoFormState[K],
  value: string | boolean,
) => void;

function asOption(value: AcOption | string | null): AcOption | null {
  if (!value) return null;
  if (typeof value === "string") return { label: value, value };
  return value;
}

export function ClassificacaoFiscalServico({
  token,
  form,
  patch,
  ctx,
  ativo = true,
}: {
  token: string;
  form: EmissaoFormState;
  patch: Patch;
  ctx?: EmissaoContexto | null;
  ativo?: boolean;
}) {
  const [cadastro, setCadastro] = useState<AcOption | null>(null);
  const [cadastroSug, setCadastroSug] = useState<AcOption[]>([]);
  const [servico, setServico] = useState<AcOption | null>(
    form.servico.itemListaServico
      ? {
          label: form.servico.descricaoServico || form.servico.itemListaServico,
          value: form.servico.itemListaServico,
          meta: `LC 116 · ${form.servico.itemListaServico}`,
        }
      : null,
  );
  const [servicoSug, setServicoSug] = useState<AcOption[]>([]);
  const [nbs, setNbs] = useState<AcOption | null>(
    form.servico.nbs ? { label: form.servico.nbs, value: form.servico.nbs } : null,
  );
  const [nbsSug, setNbsSug] = useState<AcOption[]>([]);
  const [cnae, setCnae] = useState<AcOption | null>(
    form.servico.cnae ? { label: form.servico.cnae, value: form.servico.cnae } : null,
  );
  const [cnaeSug, setCnaeSug] = useState<AcOption[]>([]);
  const [verTodos, setVerTodos] = useState(false);

  const cnaesEmpresa = useMemo(
    () => (ctx?.empresaCnaes ?? []).map((c) => c.codigo),
    [ctx?.empresaCnaes],
  );

  const cnaesFiltro = verTodos || cnaesEmpresa.length === 0 ? undefined : cnaesEmpresa;

  useEffect(() => {
    if (!ativo) return;
    const base = ctx?.operacoesNfse ?? [];
    if (base.length && !cadastro) {
      const principal = base.find((o) => o.principal) ?? base[0];
      if (principal) {
        void aplicarCadastro(String(principal.id), {
          label: `${principal.descricao}${principal.principal ? " (principal)" : ""}`,
          value: String(principal.id),
          meta: `LC 116 · ${principal.itemListaServico}`,
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ativo, ctx?.operacoesNfse]);

  const buscarCadastros = useCallback(
    async (event: AutoCompleteCompleteEvent) => {
      const q = event.query?.trim() ?? "";
      try {
        const lista = await api.listarTributNfseServicos(token, q || undefined);
        const fonte = Array.isArray(lista) && lista.length ? lista : (ctx?.operacoesNfse ?? []);
        setCadastroSug(
          fonte.map((o) => ({
            label: `${o.descricao}${o.principal ? " (principal)" : ""}`,
            value: String(o.id),
            meta: `LC 116 · ${o.itemListaServico}`,
          })),
        );
      } catch {
        setCadastroSug(
          (ctx?.operacoesNfse ?? []).map((o) => ({
            label: `${o.descricao}${o.principal ? " (principal)" : ""}`,
            value: String(o.id),
            meta: `LC 116 · ${o.itemListaServico}`,
          })),
        );
      }
    },
    [token, ctx?.operacoesNfse],
  );

  const buscarServicos = useCallback(
    async (event: AutoCompleteCompleteEvent) => {
      const q = event.query?.trim() ?? "";
      try {
        const r = await api.buscarServicos(token, q, 40, "todos", cnaesFiltro);
        let itens = r.itens ?? [];
        if (!itens.length && !q && (ctx?.servicosSugeridos?.length ?? 0) > 0) {
          itens = ctx!.servicosSugeridos!;
        }
        setServicoSug(
          itens.map((s) => ({
            label: s.descricao,
            value: s.codigo,
            meta: `LC 116 · ${s.codigo}`,
            raw: s,
          })),
        );
      } catch {
        setServicoSug([]);
      }
    },
    [token, cnaesFiltro, ctx],
  );

  const buscarNbs = useCallback(
    async (event: AutoCompleteCompleteEvent) => {
      const q = event.query?.trim() ?? "";
      const lc = form.servico.itemListaServico;
      if (!lc) {
        setNbsSug([]);
        return;
      }
      try {
        const r = await api.buscarNbs(token, q, 30, lc);
        setNbsSug(
          (r.itens ?? []).map((n) => ({
            label: n.descricao,
            value: n.codigoNacional,
            meta: n.codigoNacional,
          })),
        );
      } catch {
        setNbsSug([]);
      }
    },
    [token, form.servico.itemListaServico],
  );

  const buscarCnae = useCallback(
    async (event: AutoCompleteCompleteEvent) => {
      const q = event.query?.trim() ?? "";
      try {
        const r = await api.buscarCnae(token, q, 40);
        setCnaeSug(
          (r.itens ?? []).map((c) => ({
            label: c.descricao,
            value: c.codigo,
            meta: c.codigo,
          })),
        );
      } catch {
        setCnaeSug([]);
      }
    },
    [token],
  );

  async function aplicarCadastro(id: string, option?: AcOption) {
    const cad = await api.obterTributNfseServico(token, Number(id));
    setCadastro(
      option ?? {
        label: cad.descricao,
        value: String(cad.id),
        meta: cad.itemListaServico ? `LC 116 · ${cad.itemListaServico}` : undefined,
      },
    );
    if (cad.itemListaServico) {
      patch("servico", "itemListaServico", cad.itemListaServico);
      setServico({
        label: cad.descricaoServico || cad.descricao || cad.itemListaServico,
        value: cad.itemListaServico,
        meta: `LC 116 · ${cad.itemListaServico}`,
      });
    }
    if (cad.descricaoServico) patch("servico", "descricaoServico", cad.descricaoServico);
    if (cad.codigoTributacaoMunicipio) {
      patch("servico", "codigoTributacaoMunicipio", cad.codigoTributacaoMunicipio);
    }
    if (cad.nbs) {
      patch("servico", "nbs", cad.nbs);
      setNbs({ label: cad.nbs, value: cad.nbs, meta: cad.nbs });
    }
    if (cad.cnae) {
      patch("servico", "cnae", cad.cnae);
      setCnae({ label: cad.cnae, value: cad.cnae, meta: cad.cnae });
    }
    if (cad.municipioPrestacaoIbge) {
      patch("servico", "municipioPrestacao", cad.municipioPrestacaoIbge);
      patch("servico", "localPrestacao", cad.municipioPrestacaoIbge);
    }
    if (cad.aliquotaIss != null) patch("valores", "aliquota", String(cad.aliquotaIss));
    if (cad.tributacaoIssqn) patch("regime", "tributacaoIssqn", cad.tributacaoIssqn);
    if (cad.issRetido) patch("regime", "issRetido", cad.issRetido);
    if (cad.simplesNacional) patch("regime", "simplesNacional", cad.simplesNacional);
    if (cad.regimeEspecial) patch("regime", "regimeEspecialTributacao", cad.regimeEspecial);
    if (cad.cstPisCofins) patch("tributacaoFederal", "cstPisCofins", cad.cstPisCofins);
    if (cad.aliquotaPis != null) patch("tributacaoFederal", "aliquotaPis", String(cad.aliquotaPis));
    if (cad.aliquotaCofins != null) {
      patch("tributacaoFederal", "aliquotaCofins", String(cad.aliquotaCofins));
    }
    if (cad.habilitarRetencoes != null) {
      patch("tributacaoFederal", "habilitarRetencoes", cad.habilitarRetencoes);
    }
    if (cad.retencaoInss != null) patch("tributacaoFederal", "retencaoInss", String(cad.retencaoInss));
    if (cad.retencaoIrrf != null) patch("tributacaoFederal", "retencaoIrrf", String(cad.retencaoIrrf));
    if (cad.retencaoCsll != null) patch("tributacaoFederal", "retencaoCsll", String(cad.retencaoCsll));
    if (cad.ibsCbsCst) patch("ibsCbs", "cst", cad.ibsCbsCst);
    if (cad.ibsCbsClassTrib) patch("ibsCbs", "classificacaoTributaria", cad.ibsCbsClassTrib);
    if (cad.aliquotaIbs != null) patch("ibsCbs", "aliquotaIbs", String(cad.aliquotaIbs));
    if (cad.aliquotaCbs != null) patch("ibsCbs", "aliquotaCbs", String(cad.aliquotaCbs));
    if (cad.habilitarIbsCbs != null) patch("ibsCbs", "habilitar", cad.habilitarIbsCbs);
  }

  function escolherServico(opt: AcOption) {
    const raw = opt.raw as ServicoLc116 | undefined;
    patch("servico", "itemListaServico", opt.value);
    patch("servico", "descricaoServico", raw?.descricao || opt.label);
    patch("servico", "codigoTributacaoMunicipio", "");
    patch("servico", "nbs", "");
    setServico(opt);
    setNbs(null);
    const principal = ctx?.empresaCnaes?.find((c) => c.principal) ?? ctx?.empresaCnaes?.[0];
    if (principal) {
      patch("servico", "cnae", principal.codigo);
      setCnae({
        label: principal.descricao,
        value: principal.codigo,
        meta: principal.codigo,
      });
    }
    api.buscarNbs(token, "", 15, opt.value).then((r) => {
      if (r.itens?.[0]) {
        const n = r.itens[0];
        patch("servico", "nbs", n.codigoNacional);
        setNbs({ label: n.descricao, value: n.codigoNacional, meta: n.codigoNacional });
      }
    });
  }

  return (
    <div className="space-y-4">
      <AutoCompleteField
        id="nfse-cadastro"
        label="Tributo / serviço cadastrado"
        hint="Digite para buscar o cadastro de Tributação → NFS-e"
        placeholder="Ex.: consultoria, manutenção, mensalidade…"
        value={cadastro}
        suggestions={cadastroSug}
        completeMethod={buscarCadastros}
        forceSelection
        onChange={(v) => {
          const opt = asOption(v);
          setCadastro(opt);
          if (opt?.value) void aplicarCadastro(opt.value, opt);
        }}
      />

      <AutoCompleteField
        id="nfse-lc116"
        label="Serviço (LC 116)"
        hint={
          !verTodos && cnaesEmpresa.length > 0
            ? "Sugestões filtradas pelos CNAEs da empresa"
            : "Busque pelo nome do serviço"
        }
        placeholder="Digite o nome do serviço…"
        value={servico}
        suggestions={servicoSug}
        completeMethod={buscarServicos}
        forceSelection
        onChange={(v) => {
          const opt = asOption(v);
          if (opt?.value) escolherServico(opt);
          else {
            setServico(opt);
            patch("servico", "itemListaServico", "");
            patch("servico", "descricaoServico", "");
          }
        }}
      />

      {cnaesEmpresa.length > 0 && (
        <button
          type="button"
          className="text-sm font-medium text-[var(--brand)] hover:underline"
          onClick={() => setVerTodos((v) => !v)}
        >
          {verTodos ? "Mostrar só serviços dos CNAEs da empresa" : "Ver todos os serviços LC 116"}
        </button>
      )}

      {form.servico.itemListaServico ? (
        <>
          <AutoCompleteField
            id="nfse-nbs"
            label="NBS"
            hint="Sugestão automática — troque se precisar"
            placeholder="Buscar NBS…"
            value={nbs}
            suggestions={nbsSug}
            completeMethod={buscarNbs}
            forceSelection
            onChange={(v) => {
              const opt = asOption(v);
              setNbs(opt);
              patch("servico", "nbs", opt?.value ?? "");
            }}
          />

          <AutoCompleteField
            id="nfse-cnae"
            label="CNAE (opcional)"
            placeholder="Buscar atividade…"
            value={cnae}
            suggestions={cnaeSug}
            completeMethod={buscarCnae}
            forceSelection
            onChange={(v) => {
              const opt = asOption(v);
              setCnae(opt);
              patch("servico", "cnae", opt?.value ?? "");
            }}
          />
        </>
      ) : null}
    </div>
  );
}
