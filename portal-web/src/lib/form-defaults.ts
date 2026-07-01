import type { EmissaoContexto } from "@/lib/api";
import type { EmissaoFormState } from "@/types/emissao-form";

function hoje(): string {
  return new Date().toISOString().slice(0, 10);
}

function agoraLocal(): string {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

export function criarFormularioInicial(ctx: EmissaoContexto | null): EmissaoFormState {
  const ibge = ctx?.codigoMunicipioIbge ?? "";
  return {
    identificacao: {
      numeroRps: "",
      serieRps: "1",
      tipoRps: "1",
      dataEmissao: agoraLocal(),
      competencia: hoje(),
    },
    regime: {
      tributacaoIssqn: "1",
      regimeEspecialTributacao: "0",
      simplesNacional: "1",
      issRetido: "1",
      incentivoFiscal: false,
    },
    prestador: {
      cnpj: ctx?.prestadorDocumento ?? "",
      inscricaoMunicipal: "",
      razaoSocial: ctx?.prestadorNome ?? "",
    },
    tomador: {
      documento: "",
      razaoSocial: "",
      email: "",
      telefone: "",
      inscricaoEstadual: "",
      inscricaoMunicipal: "",
    },
    enderecoTomador: {
      cep: "",
      logradouro: "",
      numero: "",
      complemento: "",
      bairro: "",
      cidade: "",
      uf: "RS",
      codigoMunicipioIbge: ibge,
      pais: "Brasil",
    },
    classificacao: {
      atividadePrincipal: "",
    },
    servico: {
      itemListaServico: "",
      codigoTributacaoMunicipio: "",
      cnae: "",
      nbs: "",
      descricaoServico: "",
      municipioIncidencia: ibge,
      municipioPrestacao: ibge,
      localPrestacao: ibge,
    },
    valores: {
      valorServicos: "",
      deducoes: "0",
      descontoIncondicionado: "0",
      descontoCondicionado: "0",
      baseCalculo: "",
      aliquota: ctx?.aliquotaPadraoPercentual != null ? String(ctx.aliquotaPadraoPercentual) : "",
      valorIss: "",
      valorLiquidoNfse: "",
      responsavelRetencaoIss: "1",
    },
    tributacaoFederal: {
      cstPisCofins: "08",
      tipoRetencaoPisCofins: "0",
      baseCalculoPisCofins: "",
      aliquotaPis: "0",
      aliquotaCofins: "0",
      valorPis: "0",
      valorCofins: "0",
      retencaoIrrf: "0",
      retencaoCsll: "0",
      retencaoIss: "0",
      retencaoInss: "0",
      habilitarRetencoes: false,
    },
    ibsCbs: {
      cst: "",
      classificacaoTributaria: "",
      classificacaoOperacao: "",
      baseCalculo: "",
      aliquotaIbs: "0",
      aliquotaCbs: "0",
      reducaoIbs: "0",
      reducaoCbs: "0",
      valorIbs: "0",
      valorCbs: "0",
      valorTotal: "0",
      habilitar: false,
    },
    retencoesFederais: {
      pis: "0",
      cofins: "0",
      inss: "0",
      ir: "0",
      csll: "0",
      outrasRetencoes: "0",
    },
    construcaoCivil: { codigoObra: "", art: "" },
    intermediario: { documento: "", razaoSocial: "", inscricaoMunicipal: "" },
    informacoesAdicionais: { observacoes: "", informacoesComplementares: "" },
  };
}

export function parseNumero(v: string): number | undefined {
  if (!v || !v.trim()) return undefined;
  const n = parseFloat(v.replace(/\./g, "").replace(",", "."));
  return Number.isFinite(n) ? n : undefined;
}

export function formParaPayload(f: EmissaoFormState): import("@/types/emissao-form").EmissaoPayload {
  const valorServicos = parseNumero(f.valores.valorServicos) ?? 0;
  return {
    identificacao: {
      numeroRps: f.identificacao.numeroRps ? parseInt(f.identificacao.numeroRps, 10) : undefined,
      serieRps: f.identificacao.serieRps,
      tipoRps: f.identificacao.tipoRps,
      dataEmissao: f.identificacao.dataEmissao,
      competencia: f.identificacao.competencia,
    },
    regime: f.regime,
    prestador: { inscricaoMunicipal: f.prestador.inscricaoMunicipal },
    tomador: f.tomador,
    enderecoTomador: f.enderecoTomador,
    classificacao: f.classificacao,
    servico: {
      ...f.servico,
      codigoTributacaoMunicipio:
        f.classificacao.atividadePrincipal || f.servico.codigoTributacaoMunicipio,
    },
    valores: {
      valorServicos,
      deducoes: parseNumero(f.valores.deducoes),
      descontoIncondicionado: parseNumero(f.valores.descontoIncondicionado),
      descontoCondicionado: parseNumero(f.valores.descontoCondicionado),
      baseCalculo: parseNumero(f.valores.baseCalculo),
      aliquota: parseNumero(f.valores.aliquota),
      valorIss: parseNumero(f.valores.valorIss),
      valorLiquidoNfse: parseNumero(f.valores.valorLiquidoNfse),
      responsavelRetencaoIss: f.valores.responsavelRetencaoIss,
    },
    tributacaoFederal: {
      ...f.tributacaoFederal,
      baseCalculoPisCofins: parseNumero(f.tributacaoFederal.baseCalculoPisCofins) != null
        ? Math.min(parseNumero(f.tributacaoFederal.baseCalculoPisCofins)!, valorServicos)
        : undefined,
      aliquotaPis: parseNumero(f.tributacaoFederal.aliquotaPis),
      aliquotaCofins: parseNumero(f.tributacaoFederal.aliquotaCofins),
      valorPis: parseNumero(f.tributacaoFederal.valorPis),
      valorCofins: parseNumero(f.tributacaoFederal.valorCofins),
      retencaoIrrf: parseNumero(f.tributacaoFederal.retencaoIrrf),
      retencaoCsll: parseNumero(f.tributacaoFederal.retencaoCsll),
      retencaoIss: parseNumero(f.tributacaoFederal.retencaoIss),
      retencaoInss: parseNumero(f.tributacaoFederal.retencaoInss),
    },
    ibsCbs: {
      ...f.ibsCbs,
      baseCalculo: parseNumero(f.ibsCbs.baseCalculo),
      aliquotaIbs: parseNumero(f.ibsCbs.aliquotaIbs),
      aliquotaCbs: parseNumero(f.ibsCbs.aliquotaCbs),
      reducaoIbs: parseNumero(f.ibsCbs.reducaoIbs),
      reducaoCbs: parseNumero(f.ibsCbs.reducaoCbs),
      valorIbs: parseNumero(f.ibsCbs.valorIbs),
      valorCbs: parseNumero(f.ibsCbs.valorCbs),
      valorTotal: parseNumero(f.ibsCbs.valorTotal),
    },
    retencoesFederais: {
      pis: parseNumero(f.retencoesFederais.pis),
      cofins: parseNumero(f.retencoesFederais.cofins),
      inss: parseNumero(f.retencoesFederais.inss),
      ir: parseNumero(f.retencoesFederais.ir),
      csll: parseNumero(f.retencoesFederais.csll),
      outrasRetencoes: parseNumero(f.retencoesFederais.outrasRetencoes),
    },
    construcaoCivil: f.construcaoCivil,
    intermediario: f.intermediario,
    informacoesAdicionais: f.informacoesAdicionais,
  };
}

function fmt(n: number) {
  return n.toFixed(2);
}

export function recalcularValores(f: EmissaoFormState): EmissaoFormState {
  const v = parseNumero(f.valores.valorServicos) ?? 0;
  const ded = parseNumero(f.valores.deducoes) ?? 0;
  const di = parseNumero(f.valores.descontoIncondicionado) ?? 0;
  const dc = parseNumero(f.valores.descontoCondicionado) ?? 0;
  const base = Math.max(0, v - ded - di - dc);
  const aliq = parseNumero(f.valores.aliquota) ?? 0;
  const iss = (base * aliq) / 100;

  const basePisCofins = parseNumero(f.tributacaoFederal.baseCalculoPisCofins) ?? base;
  const pPis = parseNumero(f.tributacaoFederal.aliquotaPis) ?? 0;
  const pCofins = parseNumero(f.tributacaoFederal.aliquotaCofins) ?? 0;
  const valorPis = (basePisCofins * pPis) / 100;
  const valorCofins = (basePisCofins * pCofins) / 100;

  const baseIbsCbs = parseNumero(f.ibsCbs.baseCalculo) ?? base;
  const pIbs = parseNumero(f.ibsCbs.aliquotaIbs) ?? 0;
  const pCbs = parseNumero(f.ibsCbs.aliquotaCbs) ?? 0;
  const rIbs = parseNumero(f.ibsCbs.reducaoIbs) ?? 0;
  const rCbs = parseNumero(f.ibsCbs.reducaoCbs) ?? 0;
  const valorIbs = (baseIbsCbs * pIbs * (1 - rIbs / 100)) / 100;
  const valorCbs = (baseIbsCbs * pCbs * (1 - rCbs / 100)) / 100;

  const retFed = f.tributacaoFederal.habilitarRetencoes
    ? (parseNumero(f.tributacaoFederal.retencaoIrrf) ?? 0) +
      (parseNumero(f.tributacaoFederal.retencaoCsll) ?? 0) +
      (parseNumero(f.tributacaoFederal.retencaoIss) ?? 0) +
      (parseNumero(f.tributacaoFederal.retencaoInss) ?? 0) +
      valorPis +
      valorCofins
    : (parseNumero(f.retencoesFederais.pis) ?? 0) +
      (parseNumero(f.retencoesFederais.cofins) ?? 0) +
      (parseNumero(f.retencoesFederais.inss) ?? 0) +
      (parseNumero(f.retencoesFederais.ir) ?? 0) +
      (parseNumero(f.retencoesFederais.csll) ?? 0) +
      (parseNumero(f.retencoesFederais.outrasRetencoes) ?? 0);

  const liquido = Math.max(0, v - iss - retFed);

  return {
    ...f,
    valores: {
      ...f.valores,
      baseCalculo: fmt(base),
      valorIss: fmt(iss),
      valorLiquidoNfse: fmt(liquido),
    },
    tributacaoFederal: {
      ...f.tributacaoFederal,
      baseCalculoPisCofins: fmt(base),
      valorPis: fmt(valorPis),
      valorCofins: fmt(valorCofins),
      retencaoIss: f.regime.issRetido === "2" ? fmt(iss) : f.tributacaoFederal.retencaoIss,
    },
    retencoesFederais: {
      ...f.retencoesFederais,
      pis: fmt(valorPis),
      cofins: fmt(valorCofins),
      ir: f.tributacaoFederal.retencaoIrrf,
      csll: f.tributacaoFederal.retencaoCsll,
      inss: f.tributacaoFederal.retencaoInss,
    },
    ibsCbs: {
      ...f.ibsCbs,
      baseCalculo: f.ibsCbs.baseCalculo || fmt(base),
      valorIbs: fmt(valorIbs),
      valorCbs: fmt(valorCbs),
      valorTotal: fmt(valorIbs + valorCbs),
    },
  };
}
