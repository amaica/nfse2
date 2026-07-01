package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributOperacaoFiscalRepository;
import br.com.synki.nfse.portal.web.dto.nfe.NfeDestinatarioRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeItemRequest;
import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.DFConfig;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFTipoEmissao;
import com.fincatto.documentofiscal.nfe400.classes.*;
import com.fincatto.documentofiscal.nfe400.classes.nota.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NfeMontadorService {

    private static final String DEST_HOMOLOG = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
    private static final String CNPJ_DEST_HOMOLOG = "99999999000191";

    private final NfeLibService nfeLibService;
    private final CertificadoLeituraService certificadoLeituraService;
    private final TributOperacaoFiscalRepository operacaoFiscalRepository;
    private final ProdutoRepository produtoRepository;

    public NfeMontadorService(
            NfeLibService nfeLibService,
            CertificadoLeituraService certificadoLeituraService,
            TributOperacaoFiscalRepository operacaoFiscalRepository,
            ProdutoRepository produtoRepository) {
        this.nfeLibService = nfeLibService;
        this.certificadoLeituraService = certificadoLeituraService;
        this.operacaoFiscalRepository = operacaoFiscalRepository;
        this.produtoRepository = produtoRepository;
    }

    public NFNota montar(
            Long empresaId,
            Long enderecoId,
            DFModelo modelo,
            String serie,
            long numero,
            String naturezaOperacao,
            Long operacaoFiscalId,
            NfeDestinatarioRequest destinatarioReq,
            List<NfeItemRequest> itensReq,
            NFTipoEmissao tipoEmissao,
            String justificativaContingencia) {
        var empresa = nfeLibService.empresaOrThrow(empresaId);
        var endereco = enderecoId != null
                ? nfeLibService.enderecoOrThrow(empresaId, enderecoId)
                : nfeLibService.enderecoPrincipalOrThrow(empresaId);
        var ambiente = nfeLibService.ambiente(empresaId);
        var uf = DFUnidadeFederativa.valueOfCodigo(endereco.getUf());
        var documentoEmitente = documentoEmpresa(empresa);
        var emitenteCpf = documentoEmitente.length() == 11;
        var serieEfetiva = NfeSerieUtil.normalizarSerieNfe(serie, emitenteCpf);
        var operacaoFiscal = resolverOperacaoFiscal(empresaId, operacaoFiscalId);
        var itens = montarItens(empresaId, itensReq, empresa.isOptanteSimples(), operacaoFiscal);
        var totalProdutos = itens.stream()
                .map(i -> new BigDecimal(i.getProduto().getValorTotalBruto()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var identificacao = montarIdentificacao(
                modelo, ambiente, uf, endereco, serieEfetiva, numero, naturezaOperacao, tipoEmissao, justificativaContingencia);
        NfeReformaMontador.aplicarIdentificacaoReforma(identificacao, operacaoFiscal, endereco.getCodigoMunicipioIbge());

        var info = new NFNotaInfo();
        info.setVersao(new BigDecimal("4.00"));
        info.setIdentificacao(identificacao);
        info.setEmitente(montarEmitente(empresaId, empresa, endereco));
        if (modelo == DFModelo.NFE || destinatarioReq != null) {
            info.setDestinatario(montarDestinatario(destinatarioReq, ambiente, endereco));
        }
        info.setItens(itens);

        NfeReformaMontador.TotaisIbsCbs totaisIbs = null;
        for (var item : itens) {
            if (item.getImposto() != null && item.getImposto().getIbsCbs() != null) {
                totaisIbs = NfeReformaMontador.acumular(totaisIbs, item.getImposto().getIbsCbs());
            }
        }
        var total = montarTotal(totalProdutos);
        NfeReformaMontador.aplicarTotais(total, totaisIbs);
        info.setTotal(total);
        info.setTransporte(montarTransporte());
        info.setPagamento(montarPagamento(totalProdutos));

        var nota = new NFNota();
        nota.setInfo(info);
        return nota;
    }

    private NFNotaInfoIdentificacao montarIdentificacao(
            DFModelo modelo,
            DFAmbiente ambiente,
            DFUnidadeFederativa uf,
            EmpresaEndereco endereco,
            String serie,
            long numero,
            String naturezaOperacao,
            NFTipoEmissao tipoEmissao,
            String justificativaContingencia) {
        var id = new NFNotaInfoIdentificacao();
        id.setUf(uf);
        id.setCodigoMunicipio(endereco.getCodigoMunicipioIbge());
        id.setAmbiente(ambiente);
        id.setModelo(modelo);
        id.setSerie(serie);
        id.setNumeroNota(String.valueOf(numero));
        id.setDataHoraEmissao(ZonedDateTime.now(DFConfig.TIMEZONE_SP.toZoneId()));
        id.setTipo(NFTipo.SAIDA);
        id.setIdentificadorLocalDestinoOperacao(NFIdentificadorLocalDestinoOperacao.OPERACAO_INTERNA);
        id.setTipoImpressao(modelo == DFModelo.NFCE
                ? NFTipoImpressao.DANFE_NFCE
                : NFTipoImpressao.DANFE_NORMAL_RETRATO);
        id.setTipoEmissao(tipoEmissao);
        id.setFinalidade(NFFinalidade.NORMAL);
        id.setOperacaoConsumidorFinal(NFOperacaoConsumidorFinal.SIM);
        id.setIndicadorPresencaComprador(modelo == DFModelo.NFCE
                ? NFIndicadorPresencaComprador.OPERACAO_PRESENCIAL
                : NFIndicadorPresencaComprador.OPERACAO_NAO_PRESENCIAL_OUTROS);
        id.setProgramaEmissor(NFProcessoEmissor.CONTRIBUINTE);
        id.setVersaoEmissor("SynkiPortal1.0");
        id.setNaturezaOperacao(naturezaOperacao != null && !naturezaOperacao.isBlank()
                ? naturezaOperacao
                : "VENDA DE MERCADORIA");
        id.setIndIntermed(NFIndicadorIntermediador.OPERACAO_SEM_INTERMEDIADOR);
        if (tipoEmissao != NFTipoEmissao.EMISSAO_NORMAL && justificativaContingencia != null) {
            id.setDataHoraContigencia(ZonedDateTime.now(DFConfig.TIMEZONE_SP.toZoneId()));
            id.setJustificativaEntradaContingencia(justificativaContingencia);
        }
        return id;
    }

    private NFNotaInfoEmitente montarEmitente(Long empresaId, Empresa empresa, EmpresaEndereco endereco) {
        var emitente = new NFNotaInfoEmitente();
        var cert = certificadoLeituraService.lerMetadados(empresaId);
        var documento = empresa.getCnpj() != null ? empresa.getCnpj().replaceAll("\\D", "") : "";
        if (documento.length() == 11) {
            emitente.setCpf(documento);
        } else if (documento.length() == 14) {
            emitente.setCnpj(documento);
        } else if (cert.map(CertificadoLeituraService.Metadados::eCpf).orElse(false)
                && cert.map(CertificadoLeituraService.Metadados::cpfTitular).orElse(null) != null) {
            emitente.setCpf(cert.get().cpfTitular());
        } else {
            emitente.setCnpj(empresa.getCnpj());
        }
        emitente.setRazaoSocial(truncar(empresa.getNome(), 60));
        emitente.setNomeFantasia(truncar(
                empresa.getNomeFantasia() != null ? empresa.getNomeFantasia() : empresa.getNome(), 60));
        emitente.setInscricaoEstadual(endereco.getInscricaoEstadual());
        emitente.setEndereco(montarEndereco(endereco));
        emitente.setRegimeTributario(empresa.isOptanteSimples()
                ? NFRegimeTributario.SIMPLES_NACIONAL
                : NFRegimeTributario.NORMAL);
        return emitente;
    }

    private NFNotaInfoDestinatario montarDestinatario(
            NfeDestinatarioRequest req, DFAmbiente ambiente, EmpresaEndereco enderecoEmitente) {
        var dest = new NFNotaInfoDestinatario();
        if (ambiente == DFAmbiente.HOMOLOGACAO && (req == null || req.nome() == null || req.nome().isBlank())) {
            dest.setRazaoSocial(DEST_HOMOLOG);
            dest.setCnpj(CNPJ_DEST_HOMOLOG);
            dest.setIndicadorIEDestinatario(NFIndicadorIEDestinatario.NAO_CONTRIBUINTE);
            dest.setEndereco(montarEndereco(enderecoEmitente));
            return dest;
        }
        var doc = req.documento() != null ? req.documento().replaceAll("\\D", "") : CNPJ_DEST_HOMOLOG;
        dest.setRazaoSocial(truncar(req.nome() != null ? req.nome() : DEST_HOMOLOG, 60));
        if (doc.length() == 14) {
            dest.setCnpj(doc);
        } else if (doc.length() == 11) {
            dest.setCpf(doc);
        } else {
            dest.setCnpj(CNPJ_DEST_HOMOLOG);
        }
        dest.setIndicadorIEDestinatario(NFIndicadorIEDestinatario.NAO_CONTRIBUINTE);
        if (req.email() != null && !req.email().isBlank()) {
            dest.setEmail(req.email());
        }
        dest.setEndereco(montarEndereco(enderecoEmitente));
        return dest;
    }

    private NFEndereco montarEndereco(EmpresaEndereco endereco) {
        var e = new NFEndereco();
        e.setLogradouro(endereco.getLogradouro() != null ? endereco.getLogradouro() : "S/N");
        e.setNumero(endereco.getNumero() != null && !endereco.getNumero().isBlank() ? endereco.getNumero() : "S/N");
        if (endereco.getComplemento() != null && !endereco.getComplemento().isBlank()) {
            e.setComplemento(endereco.getComplemento());
        }
        e.setBairro(endereco.getBairro() != null ? endereco.getBairro() : "CENTRO");
        e.setCodigoMunicipio(endereco.getCodigoMunicipioIbge());
        e.setDescricaoMunicipio(endereco.getMunicipio());
        e.setUf(DFUnidadeFederativa.valueOfCodigo(endereco.getUf()));
        if (endereco.getCep() != null) {
            e.setCep(endereco.getCep().replaceAll("\\D", ""));
        }
        return e;
    }

    private static String documentoEmpresa(Empresa empresa) {
        return empresa.getCnpj() != null ? empresa.getCnpj().replaceAll("\\D", "") : "";
    }

    private List<NFNotaInfoItem> montarItens(
            Long empresaId,
            List<NfeItemRequest> itensReq,
            boolean optanteSimples,
            TributOperacaoFiscal operacaoFiscal) {
        var lista = new ArrayList<NFNotaInfoItem>();
        var fonte = (itensReq == null || itensReq.isEmpty()) ? List.of(itemPadrao()) : itensReq;
        int n = 1;
        for (var req : fonte) {
            lista.add(montarItem(empresaId, n++, resolverItem(req), optanteSimples, operacaoFiscal));
        }
        return lista;
    }

    private NfeItemRequest resolverItem(NfeItemRequest req) {
        if (req.produtoId() == null) {
            return req;
        }
        return produtoRepository.findById(req.produtoId()).map(p -> new NfeItemRequest(
                p.getId(),
                req.codigo() != null ? req.codigo() : p.getCodigo(),
                req.descricao() != null ? req.descricao() : p.getNome(),
                req.ncm() != null ? req.ncm() : p.getCodigoNcm(),
                req.cfop(),
                req.unidade() != null ? req.unidade() : p.getUnidade(),
                req.quantidade() != null ? req.quantidade() : BigDecimal.ONE,
                req.valorUnitario() != null ? req.valorUnitario() : p.getValorUnitario(),
                req.ibsCbs())).orElse(req);
    }

    private TributOperacaoFiscal resolverOperacaoFiscal(Long empresaId, Long operacaoFiscalId) {
        if (operacaoFiscalId == null) {
            return null;
        }
        return operacaoFiscalRepository.findByIdAndEmpresaId(operacaoFiscalId, empresaId).orElse(null);
    }

    private NfeItemRequest itemPadrao() {
        return new NfeItemRequest(
                "001",
                "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL",
                "61091000",
                "5102",
                "UN",
                new BigDecimal("1.00"),
                new BigDecimal("100.00"));
    }

    private NFNotaInfoItem montarItem(
            Long empresaId,
            int numero,
            NfeItemRequest req,
            boolean optanteSimples,
            TributOperacaoFiscal operacaoFiscal) {
        var qtd = moeda(req.quantidade() != null ? req.quantidade() : BigDecimal.ONE);
        var vUnit = moeda(req.valorUnitario() != null ? req.valorUnitario() : new BigDecimal("100.00"));
        var vTotal = moeda(qtd.multiply(vUnit));

        var produto = new NFNotaInfoItemProduto();
        produto.setCodigo(req.codigo() != null ? req.codigo() : String.format("%03d", numero));
        produto.setCodigoDeBarrasGtin("SEM GTIN");
        produto.setCodigoDeBarrasGtinTributavel("SEM GTIN");
        produto.setDescricao(req.descricao() != null ? req.descricao() : "PRODUTO");
        produto.setNcm(req.ncm() != null ? req.ncm() : "61091000");
        produto.setCfop(req.cfop() != null ? req.cfop()
                : operacaoFiscal != null && operacaoFiscal.getCfop() != null
                ? String.valueOf(operacaoFiscal.getCfop())
                : "5102");
        produto.setUnidadeComercial(req.unidade() != null ? req.unidade() : "UN");
        produto.setQuantidadeComercial(qtd);
        produto.setValorUnitario(vUnit);
        produto.setValorTotalBruto(vTotal);
        produto.setUnidadeTributavel(produto.getUnidadeComercial());
        produto.setQuantidadeTributavel(qtd);
        produto.setValorUnitarioTributavel(vUnit);
        produto.setCompoeValorNota(NFProdutoCompoeValorNota.SIM);

        var icms = new NFNotaInfoItemImpostoICMS();
        if (optanteSimples) {
            var icmsSn102 = new NFNotaInfoItemImpostoICMSSN102();
            icmsSn102.setOrigem(NFOrigem.NACIONAL);
            icmsSn102.setSituacaoOperacaoSN(NFNotaSituacaoOperacionalSimplesNacional.CSOSN_102);
            icms.setIcmssn102(icmsSn102);
        } else {
            var icms40 = new NFNotaInfoItemImpostoICMS40();
            icms40.setOrigem(NFOrigem.NACIONAL);
            icms40.setSituacaoTributaria(NFNotaInfoImpostoTributacaoICMS.CST_40);
            icms.setIcms40(icms40);
        }

        var pis = new NFNotaInfoItemImpostoPIS();
        var pisNt = new NFNotaInfoItemImpostoPISNaoTributado();
        pisNt.setSituacaoTributaria(NFNotaInfoSituacaoTributariaPIS.CST_07);
        pis.setNaoTributado(pisNt);

        var cofins = new NFNotaInfoItemImpostoCOFINS();
        var cofinsNt = new NFNotaInfoItemImpostoCOFINSNaoTributavel();
        cofinsNt.setSituacaoTributaria(NFNotaInfoSituacaoTributariaCOFINS.CST_07);
        cofins.setNaoTributavel(cofinsNt);

        var imposto = new NFNotaInfoItemImposto();
        imposto.setIcms(icms);
        imposto.setPis(pis);
        imposto.setCofins(cofins);

        var ibsCbs = NfeReformaMontador.montarIbsCbsItem(vTotal, req.ibsCbs(), operacaoFiscal);
        if (ibsCbs != null) {
            imposto.setIbsCbs(ibsCbs);
        }

        var item = new NFNotaInfoItem();
        item.setNumeroItem(numero);
        item.setProduto(produto);
        item.setImposto(imposto);
        return item;
    }

    private NFNotaInfoTotal montarTotal(BigDecimal totalProdutos) {
        var zero = BigDecimal.ZERO;
        var icmsTotal = new NFNotaInfoICMSTotal();
        icmsTotal.setBaseCalculoICMS(zero);
        icmsTotal.setValorTotalICMS(zero);
        icmsTotal.setValorICMSDesonerado(zero);
        icmsTotal.setValorTotalFundoCombatePobreza(zero);
        icmsTotal.setBaseCalculoICMSST(zero);
        icmsTotal.setValorTotalICMSST(zero);
        icmsTotal.setValorTotalFundoCombatePobrezaST(zero);
        icmsTotal.setValorTotalFundoCombatePobrezaSTRetido(zero);
        icmsTotal.setValorTotalDosProdutosServicos(totalProdutos);
        icmsTotal.setValorTotalFrete(zero);
        icmsTotal.setValorTotalSeguro(zero);
        icmsTotal.setValorTotalDesconto(zero);
        icmsTotal.setValorTotalII(zero);
        icmsTotal.setValorTotalIPI(zero);
        icmsTotal.setValorTotalIPIDevolvido(zero);
        icmsTotal.setValorPIS(zero);
        icmsTotal.setValorCOFINS(zero);
        icmsTotal.setOutrasDespesasAcessorias(zero);
        icmsTotal.setValorTotalNFe(totalProdutos);
        var total = new NFNotaInfoTotal();
        total.setIcmsTotal(icmsTotal);
        return total;
    }

    private NFNotaInfoTransporte montarTransporte() {
        var transporte = new NFNotaInfoTransporte();
        transporte.setModalidadeFrete(NFModalidadeFrete.SEM_OCORRENCIA_TRANSPORTE);
        return transporte;
    }

    private NFNotaInfoPagamento montarPagamento(BigDecimal valor) {
        var forma = new NFNotaInfoFormaPagamento();
        forma.setMeioPagamento(NFMeioPagamento.DINHEIRO);
        forma.setValorPagamento(valor);
        var pagamento = new NFNotaInfoPagamento();
        pagamento.setDetalhamentoFormasPagamento(List.of(forma));
        return pagamento;
    }

    private static BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String truncar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
