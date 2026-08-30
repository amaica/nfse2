package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.domain.fiscal.TributOperacaoFiscal;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import br.com.synki.nfse.portal.repository.fiscal.TributOperacaoFiscalRepository;
import br.com.synki.nfse.portal.web.dto.nfe.NfeDestinatarioRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeEmitirLoteRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeItemRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeReferenciaRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeReboqueRequest;
import br.com.synki.nfse.portal.web.dto.nfe.NfeTransporteRequest;
import com.fincatto.documentofiscal.DFAmbiente;
import com.fincatto.documentofiscal.DFConfig;
import com.fincatto.documentofiscal.DFModelo;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFTipoEmissao;
import com.fincatto.documentofiscal.nfe400.classes.*;
import com.fincatto.documentofiscal.nfe400.classes.nota.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
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
        return montar(
                empresaId, enderecoId, modelo, serie, numero, naturezaOperacao, operacaoFiscalId,
                destinatarioReq, itensReq, tipoEmissao, justificativaContingencia, null);
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
            String justificativaContingencia,
            NfeEmitirLoteRequest req) {
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
        var montados = montarItens(empresaId, itensReq, empresa.isOptanteSimples(), operacaoFiscal);
        var itens = montados.itens();
        var totalProdutos = montados.vProd();
        var totalDesconto = montados.vDesc();
        var valorFrete = valorFreteDe(req);
        var totalNota = totalProdutos.subtract(totalDesconto).add(valorFrete);

        var identificacao = montarIdentificacao(
                modelo, ambiente, uf, endereco, serieEfetiva, numero, naturezaOperacao, operacaoFiscal,
                tipoEmissao, justificativaContingencia, req);
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
        var total = montarTotal(totalProdutos, totalDesconto, totalNota, valorFrete);
        NfeReformaMontador.aplicarTotais(total, totaisIbs);
        info.setTotal(total);
        info.setTransporte(montarTransporte(req));
        info.setPagamento(montarPagamento(totalNota, req));
        aplicarInfAdic(info, req, operacaoFiscal);

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
            TributOperacaoFiscal operacaoFiscal,
            NFTipoEmissao tipoEmissao,
            String justificativaContingencia,
            NfeEmitirLoteRequest req) {
        var id = new NFNotaInfoIdentificacao();
        id.setUf(uf);
        id.setCodigoMunicipio(endereco.getCodigoMunicipioIbge());
        id.setAmbiente(ambiente);
        id.setModelo(modelo);
        id.setSerie(serie);
        id.setNumeroNota(String.valueOf(numero));
        id.setDataHoraEmissao(ZonedDateTime.now(DFConfig.TIMEZONE_SP.toZoneId()));
        id.setTipo(resolverTipoNf(operacaoFiscal));
        id.setIdentificadorLocalDestinoOperacao(NFIdentificadorLocalDestinoOperacao.OPERACAO_INTERNA);
        id.setTipoImpressao(modelo == DFModelo.NFCE
                ? NFTipoImpressao.DANFE_NFCE
                : NFTipoImpressao.DANFE_NORMAL_RETRATO);
        id.setTipoEmissao(tipoEmissao);
        var finalidade = resolverFinalidade(req, operacaoFiscal);
        id.setFinalidade(finalidade);
        id.setOperacaoConsumidorFinal(resolverConsumidorFinal(req));
        id.setIndicadorPresencaComprador(resolverPresenca(req, modelo));
        id.setProgramaEmissor(NFProcessoEmissor.CONTRIBUINTE);
        id.setVersaoEmissor("SynkiPortal1.0");
        id.setNaturezaOperacao(resolverNatureza(naturezaOperacao, operacaoFiscal));
        id.setIndIntermed(NFIndicadorIntermediador.OPERACAO_SEM_INTERMEDIADOR);
        if (tipoEmissao != NFTipoEmissao.EMISSAO_NORMAL && justificativaContingencia != null) {
            id.setDataHoraContigencia(ZonedDateTime.now(DFConfig.TIMEZONE_SP.toZoneId()));
            id.setJustificativaEntradaContingencia(justificativaContingencia);
        }
        var refs = montarReferencias(req);
        if (!refs.isEmpty()) {
            id.setReferenciadas(refs);
        } else if (finalidade == NFFinalidade.DEVOLUCAO_MERCADORIA || finalidade == NFFinalidade.COMPLEMENTAR) {
            throw new IllegalArgumentException(
                    "Devolução e complementar exigem ao menos um documento referenciado (chave NF-e ou NF de produtor rural).");
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

    private record ItensMontados(List<NFNotaInfoItem> itens, BigDecimal vProd, BigDecimal vDesc) {}

    private ItensMontados montarItens(
            Long empresaId,
            List<NfeItemRequest> itensReq,
            boolean optanteSimples,
            TributOperacaoFiscal operacaoFiscal) {
        var lista = new ArrayList<NFNotaInfoItem>();
        var fonte = (itensReq == null || itensReq.isEmpty()) ? List.of(itemPadrao()) : itensReq;
        int n = 1;
        BigDecimal somaProd = BigDecimal.ZERO;
        BigDecimal somaDesc = BigDecimal.ZERO;
        for (var req : fonte) {
            var resolvido = resolverItem(empresaId, req);
            var qtd = moeda(resolvido.quantidade() != null ? resolvido.quantidade() : BigDecimal.ONE);
            var vUnit = moeda(resolvido.valorUnitario() != null ? resolvido.valorUnitario() : new BigDecimal("100.00"));
            var vProd = moeda(qtd.multiply(vUnit));
            var vDesc = moeda(resolvido.valorDesconto() != null ? resolvido.valorDesconto() : BigDecimal.ZERO);
            if (vDesc.compareTo(vProd) > 0) {
                vDesc = vProd;
            }
            somaProd = somaProd.add(vProd);
            somaDesc = somaDesc.add(vDesc);
            lista.add(montarItem(n++, resolvido, optanteSimples, operacaoFiscal, qtd, vUnit, vProd, vDesc));
        }
        return new ItensMontados(lista, somaProd, somaDesc);
    }

    private NfeItemRequest resolverItem(Long empresaId, NfeItemRequest req) {
        if (req.produtoId() == null) {
            return req;
        }
        return produtoRepository.findByIdAndEmpresaId(req.produtoId(), empresaId).map(p -> new NfeItemRequest(
                p.getId(),
                req.codigo() != null ? req.codigo() : p.getCodigo(),
                req.descricao() != null ? req.descricao() : p.getNome(),
                req.ncm() != null ? req.ncm() : p.getCodigoNcm(),
                req.cfop(),
                req.unidade() != null ? req.unidade() : p.getUnidade(),
                req.quantidade() != null ? req.quantidade() : BigDecimal.ONE,
                req.valorUnitario() != null ? req.valorUnitario() : p.getValorUnitario(),
                req.valorDesconto(),
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
            int numero,
            NfeItemRequest req,
            boolean optanteSimples,
            TributOperacaoFiscal operacaoFiscal,
            BigDecimal qtd,
            BigDecimal vUnit,
            BigDecimal vProd,
            BigDecimal vDesc) {
        var vLiquido = vProd.subtract(vDesc);

        var produto = new NFNotaInfoItemProduto();
        produto.setCodigo(req.codigo() != null ? req.codigo() : String.format("%03d", numero));
        produto.setCodigoDeBarrasGtin("SEM GTIN");
        produto.setCodigoDeBarrasGtinTributavel("SEM GTIN");
        produto.setDescricao(req.descricao() != null ? req.descricao() : "PRODUTO");
        produto.setNcm(req.ncm() != null ? req.ncm() : "61091000");
        produto.setCfop(cfopItem(req.cfop(), operacaoFiscal));
        produto.setUnidadeComercial(req.unidade() != null ? req.unidade() : "UN");
        produto.setQuantidadeComercial(qtd);
        produto.setValorUnitario(vUnit);
        produto.setValorTotalBruto(vProd);
        produto.setUnidadeTributavel(produto.getUnidadeComercial());
        produto.setQuantidadeTributavel(qtd);
        produto.setValorUnitarioTributavel(vUnit);
        if (vDesc.signum() > 0) {
            produto.setValorDesconto(vDesc);
        }
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

        var ibsCbs = NfeReformaMontador.montarIbsCbsItem(vLiquido, req.ibsCbs(), operacaoFiscal);
        if (ibsCbs != null) {
            imposto.setIbsCbs(ibsCbs);
        }

        var item = new NFNotaInfoItem();
        item.setNumeroItem(numero);
        item.setProduto(produto);
        item.setImposto(imposto);
        return item;
    }

    private NFNotaInfoTotal montarTotal(
            BigDecimal totalProdutos, BigDecimal totalDesconto, BigDecimal totalNota, BigDecimal valorFrete) {
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
        icmsTotal.setValorTotalFrete(valorFrete);
        icmsTotal.setValorTotalSeguro(zero);
        icmsTotal.setValorTotalDesconto(totalDesconto);
        icmsTotal.setValorTotalII(zero);
        icmsTotal.setValorTotalIPI(zero);
        icmsTotal.setValorTotalIPIDevolvido(zero);
        icmsTotal.setValorPIS(zero);
        icmsTotal.setValorCOFINS(zero);
        icmsTotal.setOutrasDespesasAcessorias(zero);
        icmsTotal.setValorTotalNFe(totalNota);
        var total = new NFNotaInfoTotal();
        total.setIcmsTotal(icmsTotal);
        return total;
    }

    private NFNotaInfoTransporte montarTransporte(NfeEmitirLoteRequest req) {
        var transporte = new NFNotaInfoTransporte();
        var t = req != null ? req.transporte() : null;
        var mod = t != null && t.modalidadeFrete() != null && !t.modalidadeFrete().isBlank()
                ? NFModalidadeFrete.valueOfCodigo(t.modalidadeFrete().trim())
                : NFModalidadeFrete.SEM_OCORRENCIA_TRANSPORTE;
        if (mod == null) {
            throw new IllegalArgumentException("Modalidade de frete inválida.");
        }
        transporte.setModalidadeFrete(mod);
        if (t == null || mod == NFModalidadeFrete.SEM_OCORRENCIA_TRANSPORTE) {
            return transporte;
        }
        if (temTexto(t.transportadorNome()) || temTexto(t.transportadorDocumento())) {
            var tr = new NFNotaInfoTransportador();
            if (temTexto(t.transportadorNome())) {
                tr.setRazaoSocial(truncar(t.transportadorNome().trim(), 60));
            }
            var doc = soDigitos(t.transportadorDocumento());
            if (doc.length() == 14) {
                tr.setCnpj(doc);
            } else if (doc.length() == 11) {
                tr.setCpf(doc);
            }
            if (temTexto(t.transportadorIe())) {
                tr.setInscricaoEstadual(t.transportadorIe().trim().toUpperCase());
            }
            if (temTexto(t.transportadorMunicipio())) {
                tr.setNomeMunicipio(truncar(t.transportadorMunicipio().trim(), 60));
            }
            if (temTexto(t.transportadorUf())) {
                tr.setUf(resolverUf(t.transportadorUf()));
            }
            transporte.setTransportador(tr);
        }
        if (temTexto(t.placa())) {
            var veiculo = new NFNotaInfoVeiculo();
            veiculo.setPlacaVeiculo(t.placa().replaceAll("[^A-Za-z0-9]", "").toUpperCase());
            if (temTexto(t.placaUf())) {
                veiculo.setUf(resolverUf(t.placaUf()));
            }
            if (temTexto(t.rntc())) {
                veiculo.setRegistroNacionalTransportadorCarga(t.rntc().trim());
            }
            transporte.setVeiculo(veiculo);
        }
        if (t.reboques() != null && !t.reboques().isEmpty()) {
            var reboques = new ArrayList<NFNotaInfoReboque>();
            for (NfeReboqueRequest r : t.reboques()) {
                if (r == null || !temTexto(r.placa())) {
                    continue;
                }
                var reb = new NFNotaInfoReboque();
                reb.setPlacaVeiculo(r.placa().replaceAll("[^A-Za-z0-9]", "").toUpperCase());
                if (temTexto(r.uf())) {
                    reb.setUf(resolverUf(r.uf()));
                }
                if (temTexto(r.rntc())) {
                    reb.setRegistroNacionalTransportadorCarga(r.rntc().trim());
                }
                reboques.add(reb);
            }
            if (!reboques.isEmpty()) {
                transporte.setReboques(reboques);
            }
        }
        if (temVolume(t)) {
            var vol = new NFNotaInfoVolume();
            if (t.volumeQuantidade() != null && t.volumeQuantidade() > 0) {
                vol.setQuantidadeVolumesTransportados(BigInteger.valueOf(t.volumeQuantidade()));
            }
            if (temTexto(t.volumeEspecie())) {
                vol.setEspecieVolumesTransportados(truncar(t.volumeEspecie().trim(), 60));
            }
            if (temTexto(t.volumeMarca())) {
                vol.setMarca(truncar(t.volumeMarca().trim(), 60));
            }
            if (temTexto(t.volumeNumeracao())) {
                vol.setNumeracaoVolumesTransportados(truncar(t.volumeNumeracao().trim(), 60));
            }
            if (t.pesoLiquido() != null && t.pesoLiquido().signum() > 0) {
                vol.setPesoLiquido(t.pesoLiquido().setScale(3, RoundingMode.HALF_UP));
            }
            if (t.pesoBruto() != null && t.pesoBruto().signum() > 0) {
                vol.setPesoBruto(t.pesoBruto().setScale(3, RoundingMode.HALF_UP));
            }
            transporte.setVolumes(List.of(vol));
        }
        return transporte;
    }

    private NFNotaInfoPagamento montarPagamento(BigDecimal valorNota, NfeEmitirLoteRequest req) {
        var forma = new NFNotaInfoFormaPagamento();
        var meio = NFMeioPagamento.DINHEIRO;
        var ind = NFIndicadorFormaPagamento.A_VISTA;
        if (req != null && req.pagamento() != null) {
            if (temTexto(req.pagamento().meioPagamento())) {
                var codigo = normalizarMeioPagamento(req.pagamento().meioPagamento());
                meio = NFMeioPagamento.valueOfCodigo(codigo);
                if (meio == null) {
                    throw new IllegalArgumentException("Meio de pagamento inválido: " + req.pagamento().meioPagamento());
                }
            }
            if (temTexto(req.pagamento().indicadorPagamento())) {
                ind = NFIndicadorFormaPagamento.valueOfCodigo(req.pagamento().indicadorPagamento().trim());
                if (ind == null) {
                    throw new IllegalArgumentException("Prazo de pagamento inválido.");
                }
            }
        }
        forma.setMeioPagamento(meio);
        forma.setIndicadorFormaPagamento(ind);
        boolean semPagamento = meio == NFMeioPagamento.SEM_PAGAMENTO;
        forma.setValorPagamento(semPagamento ? moeda(BigDecimal.ZERO) : valorNota);
        if (meio == NFMeioPagamento.OUTRO) {
            forma.setDescricaoMeioPagamento("Outros");
        }
        var pagamento = new NFNotaInfoPagamento();
        pagamento.setDetalhamentoFormasPagamento(List.of(forma));
        return pagamento;
    }

    private static String resolverNatureza(String naturezaOperacao, TributOperacaoFiscal operacaoFiscal) {
        if (naturezaOperacao != null && !naturezaOperacao.isBlank()) {
            return naturezaOperacao.trim();
        }
        if (operacaoFiscal != null) {
            if (operacaoFiscal.getDescricaoNaNf() != null && !operacaoFiscal.getDescricaoNaNf().isBlank()) {
                return operacaoFiscal.getDescricaoNaNf().trim();
            }
            if (operacaoFiscal.getDescricao() != null && !operacaoFiscal.getDescricao().isBlank()) {
                return operacaoFiscal.getDescricao().trim();
            }
        }
        return "VENDA DE MERCADORIA";
    }

    private static BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static String cfopItem(String cfopReq, TributOperacaoFiscal operacaoFiscal) {
        String doItem = normalizarCfop(cfopReq);
        if (doItem != null) {
            return doItem;
        }
        if (operacaoFiscal != null) {
            String daOp = normalizarCfop(operacaoFiscal.getCfop());
            if (daOp != null) {
                return daOp;
            }
        }
        return "5102";
    }

    private static String normalizarCfop(Integer cfop) {
        if (cfop == null || cfop < 1000 || cfop > 7999) {
            return null;
        }
        return String.format("%04d", cfop);
    }

    private static String normalizarCfop(String cfop) {
        if (cfop == null) {
            return null;
        }
        String digits = cfop.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return null;
        }
        try {
            return normalizarCfop(Integer.parseInt(digits.substring(0, 4)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    private static void aplicarInfAdic(NFNotaInfo info, NfeEmitirLoteRequest req, TributOperacaoFiscal operacaoFiscal) {
        String texto = req != null && temTexto(req.informacoesAdicionais())
                ? req.informacoesAdicionais().trim()
                : (operacaoFiscal != null && temTexto(operacaoFiscal.getObservacao())
                        ? operacaoFiscal.getObservacao().trim()
                        : null);
        if (!temTexto(texto)) {
            return;
        }
        var inf = new NFNotaInfoInformacoesAdicionais();
        inf.setInformacoesComplementaresInteresseContribuinte(truncar(texto, 5000));
        info.setInformacoesAdicionais(inf);
    }

    private static List<NFInfoReferenciada> montarReferencias(NfeEmitirLoteRequest req) {
        var lista = new ArrayList<NFInfoReferenciada>();
        if (req == null || req.referencias() == null) {
            return lista;
        }
        int i = 0;
        for (NfeReferenciaRequest r : req.referencias()) {
            i++;
            if (r == null) {
                continue;
            }
            try {
                lista.add(montarUmaReferencia(r));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Documento referenciado #" + i + ": " + ex.getMessage(), ex);
            }
        }
        return lista;
    }

    private static NFInfoReferenciada montarUmaReferencia(NfeReferenciaRequest r) {
        var ref = new NFInfoReferenciada();
        String tipo = r.tipo() != null ? r.tipo().trim().toUpperCase() : "";
        String chave = soDigitos(r.chave());
        if ("CTE".equals(tipo) || (chave.length() == 44 && "57".equals(chave.substring(20, 22)))) {
            if (chave.length() != 44) {
                throw new IllegalArgumentException("chave CT-e deve ter 44 dígitos.");
            }
            ref.setChaveAcessoCTReferenciada(chave);
            return ref;
        }
        if ("NFE".equals(tipo) || chave.length() == 44) {
            if (chave.length() != 44) {
                throw new IllegalArgumentException("chave NF-e deve ter 44 dígitos.");
            }
            ref.setChaveAcesso(chave);
            return ref;
        }
        var nfp = new NFInfoProdutorRuralReferenciada();
        nfp.setUfEmitente(resolverUf(r.codigoUf()));
        nfp.setAnoMesEmissao(normalizarAamm(r.anoMes()));
        String cnpj = soDigitos(r.cnpj());
        String cpf = soDigitos(r.cpf());
        if (cnpj.length() == 14) {
            nfp.setCnpjEmitente(cnpj);
        } else if (cpf.length() == 11) {
            nfp.setCpfEmitente(cpf);
        } else if (cnpj.length() == 11) {
            nfp.setCpfEmitente(cnpj);
        } else {
            throw new IllegalArgumentException("informe CNPJ ou CPF do emitente da NF de produtor rural.");
        }
        if (!temTexto(r.inscricaoEstadual())) {
            throw new IllegalArgumentException("IE do emitente da NF de produtor rural é obrigatória.");
        }
        nfp.setIeEmitente(r.inscricaoEstadual().trim().toUpperCase());
        nfp.setModeloDocumentoFiscal(normalizarModeloNfp(r.modelo()));
        nfp.setSerieDocumentoFiscal(parseIntCampo(r.serie(), "série"));
        nfp.setNumeroDocumentoFiscal(parseIntCampo(r.numero(), "número"));
        ref.setInfoNFProdutorRuralReferenciada(nfp);
        return ref;
    }

    private static NFTipo resolverTipoNf(TributOperacaoFiscal op) {
        if (op == null || op.getTipoOperacao() == null) {
            return NFTipo.SAIDA;
        }
        String t = op.getTipoOperacao().trim().toUpperCase();
        if ("E".equals(t) || "0".equals(t) || "ENTRADA".equals(t)) {
            return NFTipo.ENTRADA;
        }
        return NFTipo.SAIDA;
    }

    private static NFFinalidade resolverFinalidade(NfeEmitirLoteRequest req, TributOperacaoFiscal op) {
        String codigo = req != null && temTexto(req.finalidade()) ? req.finalidade().trim() : null;
        if (codigo == null && op != null && temTexto(op.getFinalidadeOperacao())) {
            codigo = op.getFinalidadeOperacao().trim();
        }
        if (codigo == null) {
            return NFFinalidade.NORMAL;
        }
        var fin = NFFinalidade.valueOfCodigo(codigo);
        if (fin == null) {
            throw new IllegalArgumentException("Finalidade de emissão inválida: " + codigo);
        }
        return fin;
    }

    private static NFOperacaoConsumidorFinal resolverConsumidorFinal(NfeEmitirLoteRequest req) {
        if (req == null || !temTexto(req.consumidorFinal())) {
            return NFOperacaoConsumidorFinal.SIM;
        }
        String c = req.consumidorFinal().trim();
        if ("0".equals(c) || "N".equalsIgnoreCase(c) || "NAO".equalsIgnoreCase(c) || "NÃO".equalsIgnoreCase(c)) {
            return NFOperacaoConsumidorFinal.NAO;
        }
        return NFOperacaoConsumidorFinal.SIM;
    }

    private static NFIndicadorPresencaComprador resolverPresenca(NfeEmitirLoteRequest req, DFModelo modelo) {
        if (req != null && temTexto(req.indicadorPresenca())) {
            var p = NFIndicadorPresencaComprador.valueOfCodigo(req.indicadorPresenca().trim());
            if (p == null) {
                throw new IllegalArgumentException("Indicador de presença inválido.");
            }
            return p;
        }
        return modelo == DFModelo.NFCE
                ? NFIndicadorPresencaComprador.OPERACAO_PRESENCIAL
                : NFIndicadorPresencaComprador.OPERACAO_NAO_PRESENCIAL_OUTROS;
    }

    private static BigDecimal valorFreteDe(NfeEmitirLoteRequest req) {
        if (req == null || req.transporte() == null || req.transporte().valorFrete() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        var v = req.transporte().valorFrete();
        return v.signum() > 0 ? moeda(v) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean temVolume(NfeTransporteRequest t) {
        return t.volumeQuantidade() != null && t.volumeQuantidade() > 0
                || temTexto(t.volumeEspecie())
                || temTexto(t.volumeMarca())
                || temTexto(t.volumeNumeracao())
                || (t.pesoLiquido() != null && t.pesoLiquido().signum() > 0)
                || (t.pesoBruto() != null && t.pesoBruto().signum() > 0);
    }

    private static String normalizarMeioPagamento(String raw) {
        String d = soDigitos(raw);
        if ("500".equals(d)) {
            return "16";
        }
        if (d.length() == 1) {
            return "0" + d;
        }
        return d;
    }

    private static String normalizarAamm(String anoMes) {
        String d = soDigitos(anoMes);
        if (d.length() != 4) {
            throw new IllegalArgumentException("ano/mês da NF referenciada deve ser AAMM (ex.: 2108 ou 21/08).");
        }
        int mes = Integer.parseInt(d.substring(2));
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("mês da NF referenciada inválido.");
        }
        return d;
    }

    private static String normalizarModeloNfp(String modelo) {
        String d = soDigitos(modelo);
        if (d.isEmpty()) {
            return "04";
        }
        if (d.length() == 1) {
            d = "0" + d;
        }
        return d;
    }

    private static int parseIntCampo(String raw, String nome) {
        String d = soDigitos(raw);
        if (d.isEmpty()) {
            throw new IllegalArgumentException(nome + " da NF referenciada é obrigatório.");
        }
        return Integer.parseInt(d);
    }

    private static DFUnidadeFederativa resolverUf(String valor) {
        if (!temTexto(valor)) {
            throw new IllegalArgumentException("UF é obrigatória.");
        }
        String v = valor.trim().toUpperCase();
        try {
            var uf = DFUnidadeFederativa.valueOfCodigo(v);
            if (uf != null) {
                return uf;
            }
        } catch (RuntimeException ignored) {
            // tenta IBGE abaixo
        }
        String ibge = soDigitos(v);
        for (var uf : DFUnidadeFederativa.values()) {
            if (ibge.equals(uf.getCodigoIbge()) || v.equals(uf.getCodigo()) || v.equals(uf.name())) {
                return uf;
            }
        }
        throw new IllegalArgumentException("UF inválida: " + valor);
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String soDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
