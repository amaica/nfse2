export type EmissaoFormState = {
  identificacao: {
    numeroRps: string;
    serieRps: string;
    tipoRps: string;
    dataEmissao: string;
    competencia: string;
  };
  regime: {
    tributacaoIssqn: string;
    regimeEspecialTributacao: string;
    simplesNacional: string;
    issRetido: string;
    incentivoFiscal: boolean;
  };
  prestador: {
    cnpj: string;
    inscricaoMunicipal: string;
    razaoSocial: string;
  };
  tomador: {
    documento: string;
    razaoSocial: string;
    email: string;
    telefone: string;
    inscricaoEstadual: string;
    inscricaoMunicipal: string;
  };
  enderecoTomador: {
    cep: string;
    logradouro: string;
    numero: string;
    complemento: string;
    bairro: string;
    cidade: string;
    uf: string;
    codigoMunicipioIbge: string;
    pais: string;
  };
  classificacao: {
    atividadePrincipal: string;
  };
  servico: {
    itemListaServico: string;
    codigoTributacaoMunicipio: string;
    cnae: string;
    nbs: string;
    descricaoServico: string;
    municipioIncidencia: string;
    municipioPrestacao: string;
    localPrestacao: string;
  };
  valores: {
    valorServicos: string;
    deducoes: string;
    descontoIncondicionado: string;
    descontoCondicionado: string;
    baseCalculo: string;
    aliquota: string;
    valorIss: string;
    valorLiquidoNfse: string;
    responsavelRetencaoIss: string;
  };
  tributacaoFederal: {
    cstPisCofins: string;
    tipoRetencaoPisCofins: string;
    baseCalculoPisCofins: string;
    aliquotaPis: string;
    aliquotaCofins: string;
    valorPis: string;
    valorCofins: string;
    retencaoIrrf: string;
    retencaoCsll: string;
    retencaoIss: string;
    retencaoInss: string;
    habilitarRetencoes: boolean;
  };
  ibsCbs: {
    cst: string;
    classificacaoTributaria: string;
    classificacaoOperacao: string;
    baseCalculo: string;
    aliquotaIbs: string;
    aliquotaCbs: string;
    reducaoIbs: string;
    reducaoCbs: string;
    valorIbs: string;
    valorCbs: string;
    valorTotal: string;
    habilitar: boolean;
  };
  retencoesFederais: {
    pis: string;
    cofins: string;
    inss: string;
    ir: string;
    csll: string;
    outrasRetencoes: string;
  };
  construcaoCivil: {
    codigoObra: string;
    art: string;
  };
  intermediario: {
    documento: string;
    razaoSocial: string;
    inscricaoMunicipal: string;
  };
  informacoesAdicionais: {
    observacoes: string;
    informacoesComplementares: string;
  };
};

export type EmissaoPayload = {
  identificacao: {
    numeroRps?: number;
    serieRps: string;
    tipoRps: string;
    dataEmissao: string;
    competencia: string;
  };
  regime: EmissaoFormState["regime"];
  prestador: { inscricaoMunicipal: string };
  tomador: EmissaoFormState["tomador"];
  enderecoTomador: EmissaoFormState["enderecoTomador"];
  classificacao: EmissaoFormState["classificacao"];
  servico: EmissaoFormState["servico"];
  valores: {
    valorServicos: number;
    deducoes?: number;
    descontoIncondicionado?: number;
    descontoCondicionado?: number;
    baseCalculo?: number;
    aliquota?: number;
    valorIss?: number;
    valorLiquidoNfse?: number;
    responsavelRetencaoIss?: string;
  };
  tributacaoFederal: {
    cstPisCofins: string;
    tipoRetencaoPisCofins: string;
    baseCalculoPisCofins?: number;
    aliquotaPis?: number;
    aliquotaCofins?: number;
    valorPis?: number;
    valorCofins?: number;
    retencaoIrrf?: number;
    retencaoCsll?: number;
    retencaoIss?: number;
    retencaoInss?: number;
    habilitarRetencoes: boolean;
  };
  ibsCbs: {
    cst: string;
    classificacaoTributaria: string;
    classificacaoOperacao: string;
    baseCalculo?: number;
    aliquotaIbs?: number;
    aliquotaCbs?: number;
    reducaoIbs?: number;
    reducaoCbs?: number;
    valorIbs?: number;
    valorCbs?: number;
    valorTotal?: number;
    habilitar: boolean;
  };
  retencoesFederais: {
    pis?: number;
    cofins?: number;
    inss?: number;
    ir?: number;
    csll?: number;
    outrasRetencoes?: number;
  };
  construcaoCivil: EmissaoFormState["construcaoCivil"];
  intermediario: EmissaoFormState["intermediario"];
  informacoesAdicionais: EmissaoFormState["informacoesAdicionais"];
};
